package com.activity.manage.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.activity.manage.utils.constant.RabbitMQConstant.CHECKIN_QUEUE;
import static com.activity.manage.utils.constant.RabbitMQConstant.REGISTRATION_QUEUE;
import static com.activity.manage.utils.constant.RedisConstant.CHECKIN_LOCATION_KEY;
import static com.activity.manage.utils.constant.RedisConstant.REGISTRATION_ACTIVITY_KEY;

@Slf4j
@Configuration
public class RabbitMQConfig {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Autowired
    private RabbitAdmin rabbitAdmin;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 存储当前所有有效的 activityId
    private final Set<Long> registrationActivityIds = new HashSet<>();
    private final Set<Long> checkinActivityIds = new HashSet<>();

    /**
     * 注册一个新的活动ID，自动创建对应的队列
     */
    public void addRegistrationActivityId(Long activityId) {
        registrationActivityIds.add(activityId);
    }
    public void addCheckinActivityId(Long activityId) {
        checkinActivityIds.add(activityId);
    }

    /**
     * 获取所有注册队列名称
     */
    public Set<String> getRegistrationQueues() {
        // 先收集有效ID用于更新原集合
        Set<Long> validIds = registrationActivityIds.stream()
                .filter(id -> stringRedisTemplate.opsForValue()
                        .get(REGISTRATION_ACTIVITY_KEY + id) != null)
                .collect(Collectors.toSet());

        // 更新原集合
        registrationActivityIds.clear();
        registrationActivityIds.addAll(validIds);

        // 返回转换结果
        return validIds.stream()
                .map(id -> id + REGISTRATION_QUEUE)
                .collect(Collectors.toSet());
    }

    /**
     * 获取所有签到队列名称
     */
    public Set<String> getCheckinQueues() {
        // 先收集有效ID用于更新原集合
        Set<Long> validIds = checkinActivityIds.stream()
                .filter(id -> stringRedisTemplate.opsForGeo()
                        .position(CHECKIN_LOCATION_KEY, id.toString()) != null)
                .collect(Collectors.toSet());

        // 更新原集合
        checkinActivityIds.clear();
        checkinActivityIds.addAll(validIds);

        // 返回转换结果
        return validIds.stream()
                .map(id -> id + CHECKIN_QUEUE)
                .collect(Collectors.toSet());
    }

    /**
     * 动态定义多个队列 Bean
     */
    @Bean
    public Set<Queue> registrationQueues() {
        log.info("获取报名信息队列中...");
        return registrationActivityIds.stream()
                .map(id -> new Queue(id + REGISTRATION_QUEUE, true))
                .collect(Collectors.toSet());
    }

    /**
     * 动态定义签到队列 Bean
     */
    @Bean
    public Set<Queue> checkinQueues() {
        log.info("获取签到信息队列中...");
        return checkinActivityIds.stream()
                .map(id -> new Queue(id + CHECKIN_QUEUE, true))
                .collect(Collectors.toSet());
    }

    /**
     * 移除活动ID并删除对应队列
     */
    public void removeActivityId(Long activityId) {
        registrationActivityIds.remove(activityId);
        checkinActivityIds.remove(activityId);
        String queueName = activityId + REGISTRATION_QUEUE;
        try {
            rabbitAdmin.deleteQueue(queueName);
        } catch (Exception e) {
            log.warn("删除注册队列 {} 时发生错误: {}", queueName, e.getMessage());
        }
        String checkinQueue = activityId + CHECKIN_QUEUE;
        try {
            rabbitAdmin.deleteQueue(checkinQueue);
        } catch (Exception e) {
            log.warn("删除签到队列 {} 时发生错误: {}", checkinQueue, e.getMessage());
        }
        log.info("已尝试删除活动ID为 {} 的队列: {} , {}", activityId, queueName, checkinQueue);
    }

}
