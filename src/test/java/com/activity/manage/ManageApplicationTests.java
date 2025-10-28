package com.activity.manage;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@SpringBootTest
class ManageApplicationTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
	@Test
	void contextLoads() {
	}

    /**
     * 测试redis是否连接成功
     */
    @Test
    void redisConnected() {
        String test = stringRedisTemplate.opsForValue().get("test");
        log.info("已成功链接redis，测试字段为 {}", test);
    }

    /**
     * 测试rabbitmq是否连接成功
     */
    @Test
    void rabbitConnected() {
        String queue = "test";
        String value = "rabbitMQ-connection-complete";
        rabbitTemplate.convertAndSend(queue, value);
    }

    @RabbitListener(queues = "test")
    private void listenTest(String test) {
        log.info("接收到信息 {}", test);
    }
}
