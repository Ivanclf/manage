package com.activity.manage.task;

import com.activity.manage.config.RabbitMQConfig;
import com.activity.manage.mapper.ActivityMapper;
import com.activity.manage.pojo.entity.Activity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitAdmin rabbitAdmin;
    @Autowired
    private RabbitMQConfig rabbitMQConfig;

    /**
     * 处理到报名时间的活动
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void processOnRegistrationTimeActivity() {
        LocalDateTime now = LocalDateTime.now();
        log.info("定时处理到时间的活动：{}", now);
        List<Activity> activityList = new ArrayList<>();
        // TODO 此处查询出符合时间条件的列表
        if(activityList != null && !activityList.isEmpty()) {
            for(Activity activity : activityList) {
                Duration duration = Duration.between(now, activity.getRegistrationEnd());
                // 存入用户数据
                String key = REGISTRATION_ACTIVITY_KEY + activity.getId().toString();
                stringRedisTemplate.opsForValue().set(key, activity.getMaxParticipants().toString(), duration);

                activity.setStatus(REGISTERING);
                // TODO 此处更新数据库数据
            }
        }
        activityList.clear();
        now = LocalDateTime.now();
        // TODO 此处查询符合条件的列表
        if(activityList != null && !activityList.isEmpty()) {
            for(Activity activity : activityList) {
                Duration duration = Duration.between(now, activity.getActivityEnd());
                String userKey = CHECKIN_USER_KEY + activity.getId().toString();
                List<String> phoneList = new ArrayList<>();
                // TODO 此处根据活动id查出已报名的所有手机号
                stringRedisTemplate.opsForSet().add(userKey, phoneList.toArray(new String[0]));
                stringRedisTemplate.expire(userKey, duration);
                activity.setStatus(UNDERGOING);
                // TODO 此处更新数据库数据
                // 存入地理数据
                double latitude = activity.getLatitude().doubleValue();
                double longitude = activity.getLongitude().doubleValue();
                Point point = new Point(longitude, latitude);
                stringRedisTemplate.opsForGeo().add(CHECKIN_LOCATION_KEY, point, activity.getId().toString());
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
                        // 需要从activeActivityIds中提取activityId
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
