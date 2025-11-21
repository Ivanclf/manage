package com.activity.manage.service;

import cn.hutool.core.bean.BeanUtil;
import com.activity.manage.mapper.ActivityMapper;
import com.activity.manage.mapper.RegistrationMapper;
import com.activity.manage.pojo.dto.RegistrationCheckinDTO;
import com.activity.manage.pojo.dto.RegistrationDTO;
import com.activity.manage.pojo.dto.RegistrationDeleteDTO;
import com.activity.manage.pojo.entity.Activity;
import com.activity.manage.pojo.entity.Registration;
import com.activity.manage.pojo.vo.Activity2RegisterVO;
import com.activity.manage.utils.exception.ActivityNotFoundException;
import com.activity.manage.utils.exception.BaseException;
import com.activity.manage.utils.exception.NullParamException;
import com.activity.manage.utils.exception.OutOfBoundException;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.hutool.core.bean.BeanUtil.copyProperties;
import static com.activity.manage.utils.constant.RabbitMQConstant.CHECKIN_QUEUE;
import static com.activity.manage.utils.constant.RabbitMQConstant.REGISTRATION_QUEUE;
import static com.activity.manage.utils.constant.RedisConstant.*;

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

    @Transactional
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
                throw new OutOfBoundException("报名时间");
            }
            case 2 -> {
                throw new BaseException("名额不足");
            }
            case 3 -> {
                throw new BaseException("不能再次报名");
            }
            default -> {
                // 报名成功，返回正确结果，再放入RabbitMQ，异步写入数据库
                stringRedisTemplate.opsForSet().add(REGISTRATION_REGISTRATOR_KEY + activityId, phone);
                rabbitTemplate.convertAndSend(REGISTRATION_QUEUE, registrationDTO);
                return Result.success();
            }
        }
    }

    /**
     * 若RabbitMQ中报名队列存在数据，则拉取并存进数据库里
     * @param registrationDTO
     */
    @RabbitListener(queues = REGISTRATION_QUEUE)
    public void doRegistration(RegistrationDTO registrationDTO) {
        if(registrationDTO == null)
            throw new NullParamException();
        Registration registration = copyProperties(registrationDTO, Registration.class);
        registration.setRegistrationTime(LocalDateTime.now());
        registration.setCheckin(0);
        registrationMapper.insert(registration);
    }

    @Transactional
    public Result checkinConfirm(RegistrationCheckinDTO registrationCheckinDTO) {
        Long activityId = registrationCheckinDTO.getActivityId();
        String key = CHECKIN_USER_KEY + activityId.toString();
        String locationKey = CHECKIN_LOCATION_KEY + activityId;
        String phone = registrationCheckinDTO.getPhone();
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, phone);
        if(isMember == null || !isMember) {
            throw new BaseException("请进入正确的活动页面中");
        }

        // 获取坐标信息进行匹配
        double latitude = registrationCheckinDTO.getLatitude().doubleValue();
        double longitude = registrationCheckinDTO.getLongitude().doubleValue();
        Point point = new Point(longitude, latitude);
        stringRedisTemplate.opsForGeo().add(locationKey, point, "temp");
        Double distance;
        try {
            distance = Objects.requireNonNull(stringRedisTemplate.opsForGeo()
                            .distance(locationKey, activityId.toString(), "temp"))
                    .getValue();
        } catch (Exception e) {
            throw new OutOfBoundException("活动时间");
        }

        if (distance == null) {
            throw new OutOfBoundException("活动时间");
        }
        
        if (distance > 0.1) {
            throw new OutOfBoundException("签到范围");
        }
        stringRedisTemplate.opsForGeo().remove(locationKey, "temp");
        stringRedisTemplate.opsForSet().remove(key, phone);
        // 将签到信息发送到对应队列，异步处理
        rabbitTemplate.convertAndSend(CHECKIN_QUEUE, registrationCheckinDTO);
        return Result.success();
    }

    /**
     * 消费队列，执行数据库更新
     * @param registrationCheckinDTO
     */
    @RabbitListener(queues = CHECKIN_QUEUE)
    public void doCheckin(RegistrationCheckinDTO registrationCheckinDTO) {
        if(registrationCheckinDTO == null)
            throw new NullParamException();
        if(registrationMapper.isCheckin(registrationCheckinDTO.getActivityId(), registrationCheckinDTO.getPhone()) == 1)
            return;
        Registration registration = BeanUtil.copyProperties(registrationCheckinDTO, Registration.class);
        registrationMapper.checkin(registration);
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
        if(activityIds == null || activityIds.isEmpty()) {
            return Result.error("未查询到结果");
        }
        List<Activity> activityList = activityMapper.selectByIdBatch(activityIds);
        List<Activity2RegisterVO> activity2RegisterVOList = new ArrayList<>();
        if(activityList != null) {
            for (Activity activity : activityList) {
                Activity2RegisterVO a = copyProperties(activity, Activity2RegisterVO.class);
                activity2RegisterVOList.add(a);
            }
        } else {
            throw new ActivityNotFoundException();
        }
        PageInfo<Activity2RegisterVO> pageInfo = new PageInfo<>(activity2RegisterVOList);
        return Result.success(pageInfo);
    }

    /**
     * 删除报名活动
     * @param registrationDeleteDTO
     * @return
     */
    public Result registrationDelete(RegistrationDeleteDTO registrationDeleteDTO) {
        Long activityId = registrationDeleteDTO.getActivityId();
        String phone = registrationDeleteDTO.getPhone();
        stringRedisTemplate.opsForSet().remove(REGISTRATION_REGISTRATOR_KEY + activityId, phone);
        registrationMapper.delete(activityId, phone);
        return Result.success();
    }
}
