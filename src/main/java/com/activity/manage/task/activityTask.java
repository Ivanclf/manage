package com.activity.manage.task;

import com.activity.manage.config.RabbitMQConfig;
import com.activity.manage.mapper.ActivityMapper;
import com.activity.manage.pojo.entity.Activity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static com.activity.manage.utils.constant.ActivityConstant.REGISTERING;
import static com.activity.manage.utils.constant.RabbitMQConstant.REGISTRATION_QUEUE;
import static com.activity.manage.utils.constant.RedisConstant.REGISTRATION_ACTIVITY_KEY;

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
        log.info("定时处理到报名时间的活动：{}", now);
        List<Activity> activityList = new ArrayList<>();
        // TODO 此处查询出符合时间条件的列表
        if(activityList != null && !activityList.isEmpty()) {
            for(Activity activity : activityList) {
                Duration duration = Duration.between(now, activity.getRegistrationEnd());
                String key = REGISTRATION_ACTIVITY_KEY + activity.getId().toString();
                stringRedisTemplate.opsForValue().set(key, activity.getMaxParticipants().toString(), duration);
                activity.setStatus(REGISTERING);
                // TODO 此处更新数据库数据
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
            // 获取所有活动队列名称
            Set<String> queueNames = rabbitMQConfig.getRegistrationQueues();
            for (String queueName : queueNames) {
                // 获取队列信息
                Properties queueInfo = rabbitAdmin.getQueueProperties(queueName);
                if (queueInfo != null) {
                    // 获取消息总数
                    Long messageCount = (Long) queueInfo.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
                    // 如果队列为空，则移除该活动ID（会自动删除队列）
                    if (messageCount != null && messageCount == 0) {
                        // 需要从activeActivityIds中提取activityId
                        String activityIdStr = queueName.replace(REGISTRATION_QUEUE, "");
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
