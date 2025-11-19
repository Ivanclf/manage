package com.activity.manage.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * 配置JSON消息转换器
     * 将RabbitMQ的消息体在Java对象和JSON格式之间进行转换
     * 替代默认的Java原生序列化，提供更好的跨语言兼容性和安全性
     *
     * @return Jackson2JsonMessageConverter 使用Jackson库的JSON转换器实例
     */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.activity.manage.pojo.dto");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    /**
     * 配置RabbitTemplate，用于发送消息到RabbitMQ
     * 设置使用JSON消息转换器，确保发送的消息为JSON格式
     * 与接收端的消息格式保持一致，避免序列化/反序列化不匹配
     *
     * @param connectionFactory RabbitMQ连接工厂，由Spring Boot自动配置
     * @return 配置了JSON转换器的RabbitTemplate实例
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * 配置RabbitMQ监听器容器工厂
     * 用于创建消息监听容器，处理到达队列的消息
     * 设置使用JSON消息转换器，确保能正确反序列化JSON格式的消息
     *
     * @param connectionFactory RabbitMQ连接工厂，由Spring Boot自动配置
     * @return 配置了JSON转换器的监听器容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }

    /**
     * 在容器销毁前执行的清理方法，用于删除RabbitMQ队列
     */
    @PreDestroy
    public void rabbitAdminWithDeletePolicy() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setIgnoreDeclarationExceptions(true);

        rabbitAdmin.deleteQueue(REGISTRATION_QUEUE);
        rabbitAdmin.deleteQueue(CHECKIN_QUEUE);
        log.info("队列已成功清除");
    }
}