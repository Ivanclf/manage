package com.activity.manage.service;

import cn.hutool.core.bean.BeanUtil;
import com.activity.manage.config.RabbitMQConfig;
import com.activity.manage.mapper.ActivityMapper;
import com.activity.manage.mapper.RegistrationMapper;
import com.activity.manage.pojo.dto.CheckinDTO;
import com.activity.manage.pojo.dto.RegistrationDTO;
import com.activity.manage.pojo.entity.Activity;
import com.activity.manage.pojo.entity.Registration;
import com.activity.manage.pojo.vo.Activity2RegisterVO;
import com.activity.manage.utils.result.Result;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.activity.manage.utils.constant.RabbitMQConstant.CHECKIN_QUEUE;
import static com.activity.manage.utils.constant.RabbitMQConstant.REGISTRATION_QUEUE;
import static com.activity.manage.utils.constant.RedisConstant.CHECKIN_LOCATION_KEY;
import static com.activity.manage.utils.constant.RedisConstant.CHECKIN_USER_KEY;

@Service
@Slf4j
public class RegistrationService {
    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
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
        // 执行Lua脚本，查看返回的结果
        Long result = stringRedisTemplate.execute(
                REGISTRATION_SCRIPT,
                Collections.emptyList(),
                activityId.toString(),
                phone
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

    public Result checkinConfirm(CheckinDTO checkinDTO) {
        Long activityId = checkinDTO.getId();
        String key = CHECKIN_USER_KEY + activityId.toString();
        String phone = checkinDTO.getPhone();
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, phone);
        if(isMember == null || !isMember) {
            return Result.error("该用户没有该活动的报名信息");
        }

        // 获取坐标信息进行匹配
        double latitude = checkinDTO.getLatitude().doubleValue();
        double longitude = checkinDTO.getLongitude().doubleValue();
        Point point = new Point(longitude, latitude);
        stringRedisTemplate.opsForGeo().add(CHECKIN_LOCATION_KEY, point, "temp");
        Double distance;
        try {
            distance = Objects.requireNonNull(stringRedisTemplate.opsForGeo()
                            .distance(CHECKIN_LOCATION_KEY, activityId.toString(), "temp"))
                    .getValue();
        } catch (Exception e) {
            return Result.error("活动尚未开始");
        }

        if (distance == null) {
            return Result.error("活动尚未开始");
        }
        
        if (distance > 0.1) {
            return Result.error("不在签到范围内");
        }
        stringRedisTemplate.opsForGeo().remove(CHECKIN_LOCATION_KEY, "temp");
        // 将签到信息发送到对应队列，异步处理
        String queue = activityId + CHECKIN_QUEUE;
        rabbitTemplate.convertAndSend(queue, checkinDTO);
        return Result.success();
    }

    /**
     * 消费队列，执行数据库更新
     * @param checkinDTO
     */
    @RabbitListener(queues = "#{@rabbitMQConfig.checkinQueues}")
    public void doCheckin(CheckinDTO checkinDTO) {
        try {
            Registration registration = new Registration();
            BeanUtil.copyProperties(checkinDTO, Registration.class);
            registration.setCheckin(1);
            registrationMapper.checkin(registration);
        } catch (Exception e) {
            log.error("处理签到消息时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 用户查看报名信息
     *
     * @param phone
     * @param pageNum
     * @param pageSize
     * @return
     */
    public Result<PageInfo<Activity2RegisterVO>> searchActivitiesByPhone(String phone, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Long> activityIds = registrationMapper.selectActivityIdByPhone(phone);
        List<Activity> activityList = activityMapper.selectByIdBatch(activityIds);
        List<Activity2RegisterVO> activity2RegisterVOList = new ArrayList<>();
        if(activityList != null) {
            for (Activity activity : activityList) {
                Activity2RegisterVO a = BeanUtil.copyProperties(activity, Activity2RegisterVO.class);
                activity2RegisterVOList.add(a);
            }
        }
        PageInfo<Activity2RegisterVO> pageInfo = new PageInfo<>(activity2RegisterVOList);
        return Result.success(pageInfo);
    }
}
