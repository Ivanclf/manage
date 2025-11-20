package com.activity.manage.task;

import com.activity.manage.config.RabbitMQConfig;
import com.activity.manage.mapper.ActivityMapper;
import com.activity.manage.mapper.RegistrationMapper;
import com.activity.manage.pojo.entity.Activity;
import com.activity.manage.utils.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.activity.manage.utils.constant.ActivityConstant.REGISTERING;
import static com.activity.manage.utils.constant.ActivityConstant.UNDERGOING;
import static com.activity.manage.utils.constant.RabbitMQConstant.CHECKIN_QUEUE;
import static com.activity.manage.utils.constant.RabbitMQConstant.REGISTRATION_QUEUE;
import static com.activity.manage.utils.constant.RedisConstant.*;

@Component
@Slf4j
public class activityTask {
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitAdmin rabbitAdmin;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitMQConfig rabbitMQConfig;
    @Autowired
    private static final DefaultRedisScript<Long> CHECKIN_PROCESS_SCRIPT;
    static {
        CHECKIN_PROCESS_SCRIPT = new DefaultRedisScript<>();
        CHECKIN_PROCESS_SCRIPT.setLocation(new ClassPathResource("checkinProcess.lua"));
        CHECKIN_PROCESS_SCRIPT.setResultType(Long.class);
    }

    /**
     * 处理到报名时间的活动
     */
    @Scheduled(cron = "*/10 * * * * ?")
    @Transactional
    public void processOnRegistrationTimeActivity() {
        LocalDateTime now = LocalDateTime.now();
        log.info("定时处理到时间的活动：{}", now);
        // 处理报名数据
        List<Activity> activityList = activityMapper.selectByRegistrationStart(now);
        if(activityList != null && !activityList.isEmpty()) {
            for(Activity activity : activityList) {
                Long activityId = activity.getId();
                String key = REGISTRATION_ACTIVITY_KEY + activityId;
                if(stringRedisTemplate.opsForValue().get(key) == null) {
                    // 检查活动的registrationEnd是否为null
                    if (activity.getRegistrationEnd() == null) {
                        log.warn("活动 {} 的 registrationEnd 为 null，跳过处理", activityId);
                        continue;
                    }
                    Duration duration = Duration.between(now, activity.getRegistrationEnd());
                    // 存入用户数据，分为存储最大报名人数的字符串和用户手机号哈希
                    stringRedisTemplate.opsForValue().set(key, activity.getMaxParticipants().toString(), duration);

                    activity.setStatus(REGISTERING);
                    activityMapper.update(activity);
                }
            }
        }
        if (activityList != null) {
            activityList.clear();
        }
        now = LocalDateTime.now();
        // 处理签到活动
        activityList = activityMapper.selectByActivityStart(now);
        if(activityList != null && !activityList.isEmpty()) {
            for(Activity activity : activityList) {
                Long activityId = activity.getId();
                List<Point> positionlist = stringRedisTemplate.opsForGeo().position(CHECKIN_LOCATION_KEY, activityId.toString());
                if(positionlist == null) {
                    continue;
                }
                Point point = positionlist.getFirst();
                if(point == null) {
                    // 检查活动的activityEnd是否为null
                    if (activity.getActivityEnd() == null) {
                        log.warn("活动 {} 的 activityEnd 为 null，跳过处理", activityId);
                        continue;
                    }
                    // 准备lua脚本参数
                    Duration duration = Duration.between(now, activity.getActivityEnd());
                    String userKey = CHECKIN_USER_KEY + activityId;
                    List<String> phoneList = registrationMapper.selectPhoneByActivity(activityId);

                    if(phoneList == null || phoneList.isEmpty()) {
                        log.warn("该活动无人报名，不生成签到数据");
                        continue;
                    }

                    List<String> keys = new ArrayList<>();
                    keys.add(userKey);
                    keys.add(CHECKIN_LOCATION_KEY + ":" + activityId);

                    List<String> args = new ArrayList<>();
                    args.add(activityId.toString());
                    args.add(String.valueOf(duration.getSeconds()));
                    args.add(activity.getLatitude() != null ? activity.getLatitude().toString() : "0");
                    args.add(activity.getLongitude() != null ? activity.getLongitude().toString() : "0");
                    args.addAll(phoneList);

                    // 执行lua脚本
                    Long result = stringRedisTemplate.execute(
                            CHECKIN_PROCESS_SCRIPT,
                            keys,
                            args.toArray()
                    );
                    if(result == null || result == 0L) {
                        throw new BaseException("lua脚本执行失败");
                    }
                    log.info("活动 {} 处理成功，应签到人数为 {}", activity.getId(), result);

                    activity.setStatus(UNDERGOING);
                    activityMapper.update(activity);
                }

            }
        }
    }
}
