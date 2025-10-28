package com.activity.manage.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.activity.manage.utils.constant.RabbitMQConstant.CHECKIN_QUEUE;
import static com.activity.manage.utils.constant.RabbitMQConstant.REGISTRATION_QUEUE;

@Slf4j
@Configuration
public class RabbitMQConfig {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Autowired
    private RabbitAdmin rabbitAdmin;

    // 存储当前所有有效的 activityId
    private final Set<Long> activeActivityIds = new HashSet<>();

    /**
     * 注册一个新的活动ID，自动创建对应的队列
     */
    public void addActiveActivityId(Long activityId) {
        activeActivityIds.add(activityId);
    }

    /**
     * 获取所有注册队列名称
     */
    public Set<String> getRegistrationQueues() {
        // TODO 此处需要查询相关活动的报名结束时间，若结束则需要删除
        return activeActivityIds.stream()
                .map(id -> id + REGISTRATION_QUEUE)
                .collect(Collectors.toSet());
    }

    /**
     * 获取所有签到队列名称
     */
    public Set<String> getCheckinQueues() {
        return activeActivityIds.stream()
                .map(id -> id + CHECKIN_QUEUE)
                .collect(Collectors.toSet());
    }

    /**
     * 动态定义多个队列 Bean
     */
    @Bean
    public Set<Queue> registrationQueues() {
        log.info("获取报名信息队列中...");
        return activeActivityIds.stream()
                .map(id -> new Queue(id + REGISTRATION_QUEUE, true))
                .collect(Collectors.toSet());
    }

    /**
     * 动态定义签到队列 Bean
     */
    @Bean
    public Set<Queue> checkinQueues() {
        log.info("获取签到信息队列中...");
        return activeActivityIds.stream()
                .map(id -> new Queue(id + CHECKIN_QUEUE, true))
                .collect(Collectors.toSet());
    }

    /**
     * 移除活动ID并删除对应队列
     */
    public void removeActivityId(Long activityId) {
        activeActivityIds.remove(activityId);
        String queueName = activityId + REGISTRATION_QUEUE;
        try {
            rabbitAdmin.deleteQueue(queueName);
        } catch (Exception e) {
            log.warn("删除注册队列 {} 时发生错误（忽略）: {}", queueName, e.getMessage());
        }
        String checkinQueue = activityId + CHECKIN_QUEUE;
        try {
            rabbitAdmin.deleteQueue(checkinQueue);
        } catch (Exception e) {
            log.warn("删除签到队列 {} 时发生错误（忽略）: {}", checkinQueue, e.getMessage());
        }
        log.info("已尝试删除活动ID为 {} 的队列: {} , {}", activityId, queueName, checkinQueue);
    }

}
