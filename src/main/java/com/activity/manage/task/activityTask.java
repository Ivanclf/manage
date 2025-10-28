package com.activity.manage.task;

import com.activity.manage.mapper.ActivityMapper;
import com.activity.manage.pojo.entity.Activity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.activity.manage.utils.constant.RedisConstant.REGISTRATION_ACTIVITY_KEY;

@Component
@Slf4j
public class activityTask {
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 处理到报名时间的活动
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void processOnRegistrationTimeActivity() {
        LocalDateTime now = LocalDateTime.now();
        log.info("定时处理到报名时间的活动：{}", now);
        List<Activity> activityList = new ArrayList<>();
        // TODO 此处查询出符合时间条件的列表
        if(activityList != null && !activityList.isEmpty()) {
            for(Activity activity : activityList) {
                Duration duration = Duration.between(now, activity.getRegistrationEnd());
                String key = REGISTRATION_ACTIVITY_KEY + activity.getId().toString();
                stringRedisTemplate.opsForValue().set(key, activity.getMaxParticipants().toString(), duration);
            }
        }
    }
}
