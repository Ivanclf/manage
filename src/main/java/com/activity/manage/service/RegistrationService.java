package com.activity.manage.service;

import cn.hutool.core.bean.BeanUtil;
import com.activity.manage.config.RabbitMQConfig;
import com.activity.manage.mapper.RegistrationMapper;
import com.activity.manage.pojo.dto.RegistrationDTO;
import com.activity.manage.pojo.entity.Registration;
import com.activity.manage.utils.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;

import static com.activity.manage.utils.constant.RabbitMQConstant.REGISTRATION_QUEUE;

@Service
@Slf4j
public class RegistrationService {
    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitMQConfig rabbitMQConfig;
    @Autowired
    private static final DefaultRedisScript<Long> REGISTRATION_SCRIPT;
    static {
        REGISTRATION_SCRIPT = new DefaultRedisScript<>();
        REGISTRATION_SCRIPT.setLocation(new ClassPathResource("registration.lua"));
        REGISTRATION_SCRIPT.setResultType(Long.class);
    }

    public Result registration(RegistrationDTO registrationDTO) {
        Long activityId = registrationDTO.getActivityId();
        String phone = registrationDTO.getPhone();
        // TODO 此处需要查询活动信息，查看活动是否存在，是否在报名时间内
        // 将活动信息放进集合中
        rabbitMQConfig.addActiveActivityId(activityId);
        // 执行Lua脚本，查看返回的结果
        Long result = stringRedisTemplate.execute(
                REGISTRATION_SCRIPT,
                Collections.emptyList(),
                activityId.toString(), phone
        );
        switch (result.intValue()) {
            case 1 -> {
                return Result.error("不在报名时间内");
            }
            case 2 -> {
                return Result.error("名额不足");
            }
            case 3 -> {
                return Result.error("不能重复报名");
            }
            default -> {
                // 报名成功，返回正确结果，再放入RabbitMQ，异步写入数据库
                String queue = activityId + REGISTRATION_QUEUE;
                rabbitTemplate.convertAndSend(queue, registrationDTO);
                return Result.success();
            }
        }
    }

    /**
     * 若RabbitMQ中报名队列存在数据，则拉取并存进数据库里
     * @param registrationDTO
     */
    @RabbitListener(queues = "#{@rabbitMQConfig.registrationQueues}")
    public void insert(RegistrationDTO registrationDTO) {
        Registration registration = BeanUtil.copyProperties(registrationDTO, Registration.class);
        registration.setRegistrationTime(LocalDateTime.now());
        registration.setCheckin(0);
        registrationMapper.insert(registration);
    }
}
