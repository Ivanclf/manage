package com.activity.manage;

import com.activity.manage.mapper.AdminMapper;
import com.activity.manage.pojo.entity.Administrator;
import com.activity.manage.utils.*;
import com.activity.manage.utils.result.Result;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.activity.manage.utils.constant.QRCodeConstant.*;

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
    @Autowired
    private QRCodeUtil qrCodeUtil;

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
            byte[] image = qrCodeUtil.generateQRCodeBytes("localhost:8080", 300, 300);

            String url = aliOSSUtil.upload(image,
                    new QRCodeMap(QRCODE_ACTIVITY_ROUTE, Md5Util.md5Str("test"), QRCODE_FORMAT));
            log.info("二维码已上传到OSS，访问URL：{}", url);
        } catch (Exception e) {
            log.error("上传二维码失败");
        }
    }

    /**
     * 测试ffmpeg是否能正常链接
     */
    @Test
    void ffmpeg() {
        FFmpegResult result = FFmpeg.atPath()
                .addArgument("-version")
                .execute();
    }
}
