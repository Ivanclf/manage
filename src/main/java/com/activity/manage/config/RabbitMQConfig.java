package com.activity.manage.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

import static com.activity.manage.utils.constant.RabbitMQConstant.CHECKIN_QUEUE;
import static com.activity.manage.utils.constant.RabbitMQConstant.REGISTRATION_QUEUE;

@Slf4j
@Configuration
public class RabbitMQConfig {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Bean
    public RabbitAdmin rabbitAdmin() {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public Queue registrationQueue() {
        return new Queue(REGISTRATION_QUEUE, true);
    }

    @Bean
    public Queue checkinQueue() {
        return new Queue(CHECKIN_QUEUE, true);
    }

    @PreDestroy
    public void rabbitAdminWithDeletePolicy() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setIgnoreDeclarationExceptions(true);

        rabbitAdmin.deleteQueue(REGISTRATION_QUEUE);
        rabbitAdmin.deleteQueue(CHECKIN_QUEUE);
        log.info("队列已成功清除");
    }
}