package com.activity.manage.task;

import com.activity.manage.config.RabbitMQConfig;
import com.activity.manage.mapper.ActivityMapper;
import com.activity.manage.mapper.RegistrationMapper;
import com.activity.manage.pojo.entity.Activity;
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
import java.util.*;

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
    @Scheduled(cron = "*/30 * * * * ?")
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
                String queue = activityId + REGISTRATION_QUEUE;
                if(stringRedisTemplate.opsForValue().get(key) == null) {
                    Duration duration = Duration.between(now, activity.getRegistrationEnd());
                    // 存入用户数据，分为存储最大报名人数的字符串和用户手机号哈希
                    stringRedisTemplate.opsForValue().set(key, activity.getMaxParticipants().toString(), duration);
                    rabbitMQConfig.addRegistrationActivityId(activityId);
                    rabbitTemplate.convertAndSend(queue, "");

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
                String queue = activityId + CHECKIN_QUEUE;
                if(stringRedisTemplate.opsForGeo().position(CHECKIN_LOCATION_KEY, activityId.toString()) == null) {
                    // 准备lua脚本参数
                    Duration duration = Duration.between(now, activity.getActivityEnd());
                    String userKey = CHECKIN_USER_KEY + activityId;
                    List<String> phoneList = registrationMapper.selectPhoneByActivity(activityId);

                    List<String> keys = new ArrayList<>();
                    keys.add(userKey);
                    keys.add(CHECKIN_LOCATION_KEY);

                    List<String> args = new ArrayList<>();
                    args.add(activityId.toString());
                    args.add(String.valueOf(duration.getSeconds()));
                    args.add(activity.getLatitude().toString());
                    args.add(activity.getLongitude().toString());
                    args.addAll(phoneList);

                    // 执行lua脚本
                    Long result = stringRedisTemplate.execute(
                            CHECKIN_PROCESS_SCRIPT,
                            keys,
                            args
                    );
                    if(result == null || result == 0L) {
                        log.info("lua脚本执行失败");
                        continue;
                    }
                    rabbitMQConfig.addCheckinActivityId(activityId);
                    rabbitTemplate.convertAndSend(queue, "");
                    log.info("活动 {} 处理成功，应签到人数为 {}", activity.getId(), result);

                    activity.setStatus(UNDERGOING);
                    activityMapper.update(activity);
                }

            }
        }
    }

    /**
     * 清理空队列
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanEmptyQueues() {
        log.info("定时执行每日空队列清理任务");
        try {
            // 获取所有活动队列名称（包含报名与签到队列）
            Set<String> queueNames = new HashSet<>();
            queueNames.addAll(rabbitMQConfig.getRegistrationQueues());
            queueNames.addAll(rabbitMQConfig.getCheckinQueues());
            for (String queueName : queueNames) {
                // 获取队列信息
                Properties queueInfo = rabbitAdmin.getQueueProperties(queueName);
                if (queueInfo != null) {
                    // 获取消息总数
                    Long messageCount = (Long) queueInfo.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
                    // 如果队列为空，则移除该活动ID（会自动删除队列）
                    if (messageCount != null && messageCount == 0) {
                        // 兼容两种后缀，先尝试去掉报名后缀
                        String activityIdStr = queueName.replace(REGISTRATION_QUEUE, "");
                        if (activityIdStr.equals(queueName)) {
                            // 说明不是报名队列，尝试去掉签到后缀
                            activityIdStr = queueName.replace(CHECKIN_QUEUE, "");
                        }
                        try {
                            Long activityId = Long.parseLong(activityIdStr);
                            rabbitMQConfig.removeActivityId(activityId);
                            log.info("已删除空队列: {}", queueName);
                        } catch (NumberFormatException e) {
                            log.warn("无法解析活动ID: {}", activityIdStr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("清理空队列时发生错误", e);
        }
        log.info("每日空队列清理任务执行完毕");
    }
}
