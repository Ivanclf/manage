package com.activity.manage;

import com.activity.manage.mapper.AdminMapper;
import com.activity.manage.pojo.entity.Administrator;
import com.activity.manage.utils.AliOSSUtil;
import com.activity.manage.utils.QRCodeUtil;
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
    private AdminMapper adminMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private AliOSSUtil aliOSSUtil;

	@Test
	void mysqlConnected() {
        Administrator administrator = adminMapper.loginById(new Administrator(1, "admin", "e10adc3949ba59abbe56e057f20f883e"));
        log.info("数据库链接成功，显示结果为 {}", administrator);
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

    @Test
    void testUploadQRCode() {
        try {
            String url = aliOSSUtil.uploadQRCode("localhost:8080", 300, 300);
            log.info("二维码已上传到OSS，访问URL：{}", url);
        } catch (Exception e) {
            log.error("上传二维码失败");
        }
    }
}
