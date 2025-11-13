package com.activity.manage.service;

import cn.hutool.core.bean.BeanUtil;
import com.activity.manage.mapper.ActivityMapper;
import com.activity.manage.pojo.dto.ActivityDTO;
import com.activity.manage.pojo.dto.AdministratorDTO;
import com.activity.manage.pojo.entity.Activity;
import com.activity.manage.utils.AdminHolder;
import com.activity.manage.utils.AliOSSUtil;
import com.activity.manage.utils.SnowFlakeGenerator;
import com.activity.manage.utils.constant.ActivityConstant;
import com.activity.manage.utils.exception.ActivityNotFoundException;
import com.activity.manage.utils.exception.AdminTokenExpiredException;
import com.activity.manage.utils.exception.BaseException;
import com.activity.manage.utils.result.Result;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.activity.manage.utils.constant.RedisConstant.*;

@Service
@Slf4j
public class ActivityService {

    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private AliOSSUtil aliOSSUtil;

    /**
     * 1. 创建活动 (POST /activity)
     */
    @Transactional
    public Result<Long> createActivity(ActivityDTO activityDTO) {
        // 1. 从 ThreadLocal 获取当前管理员
        AdministratorDTO admin = AdminHolder.getAdmin();
        if (admin == null) {
            throw new AdminTokenExpiredException();
        }

        // 2. DTO 转换为 Entity
        Activity activity = BeanUtil.copyProperties(activityDTO, Activity.class);

        // 3. 补全业务字段
        activity.setId(SnowFlakeGenerator.generateId());
        activity.setCreatorId(admin.getId());
        activity.setCreateTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        activity.setStatus(ActivityConstant.UNRELEASED); // 初始状态：未发布
        activity.setCurrentParticipants(0);

        // 4. 插入数据库
        activityMapper.insert(activity);

        // 5. 返回活动ID
        return Result.success(activity.getId());
    }

    /**
     * 2. 查询/搜索活动 (GET /activity)
     */
    public Result<PageInfo<Activity>> searchActivities(String activityName, Integer status, Boolean isFull, String location, int pageNum, int pageSize) {
        // 1. 启动 PageHelper 分页
        PageHelper.startPage(pageNum, pageSize);

        // 2. 构造查询参数
        Activity activity = Activity.builder()
                .activityName(activityName)
                .status(status)
                .location(location)
                .build();

        // 3. 执行查询
        List<Activity> list = activityMapper.select(activity);
        if (isFull != null) {
            list = list.stream()
                    .filter(a -> (a.getMaxParticipants() - a.getCurrentParticipants() == 0) == isFull)
                    .toList();
        }

        if(list.isEmpty()) {
            throw new ActivityNotFoundException();
        }

        // 4. 封装分页结果
        PageInfo<Activity> pageInfo = new PageInfo<>(list);
        return Result.success(pageInfo);
    }

    /**
     * 3. 获取活动详情 (GET /activity/{id})
     */
    public Result<Activity> getActivityById(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new ActivityNotFoundException();
        }
        return Result.success(activity);
    }

    /**
     * 4. 更新活动 (PUT /activity/{id})
     */
    @Transactional
    public Result updateActivity(Long id, ActivityDTO activityDTO) {
        // 1. 检查活动是否存在
        Activity dbActivity = activityMapper.selectById(id);
        if (dbActivity == null) {
            throw new ActivityNotFoundException();
        }

        // 2. 检查权限 (允许所有管理员修改)
        AdministratorDTO admin = AdminHolder.getAdmin();
        if (admin == null) {
            throw new AdminTokenExpiredException();
        }

        // 3. DTO 转换为 Entity
        Activity activityToUpdate = BeanUtil.copyProperties(activityDTO, Activity.class);

        // 4. 设置关键字段
        activityToUpdate.setId(id);
        activityToUpdate.setUpdateTime(LocalDateTime.now());

        // 5. 执行更新
        activityMapper.update(activityToUpdate);
        
        // 按照新报名时间、新活动开始时间、新报名人数更新redis中的过期时间和其他数据
        updateActivityRedisData(dbActivity, activityToUpdate);
        
        return Result.success();
    }


    /**
     * 5. 删除活动 (DELETE /activity/{id})
     */
    @Transactional
    public Result deleteActivity(Long id) {
        // 1. 检查活动是否存在
        Activity dbActivity = activityMapper.selectById(id);
        if (dbActivity == null) {
            throw new ActivityNotFoundException();
        }
        // 2. 执行删除
        activityMapper.deleteById(id);

        // 3. 清理redis相关数据
        String registrationKey = REGISTRATION_ACTIVITY_KEY + id;
        String activityUserKey = CHECKIN_USER_KEY + id;
        String activityLocationKey = CHECKIN_LOCATION_KEY + id;

        stringRedisTemplate.delete(registrationKey);
        stringRedisTemplate.delete(activityUserKey);
        stringRedisTemplate.delete(activityLocationKey);

        // 删除OSS上的二维码
        if (dbActivity.getQrCodeOssUrl() != null && !dbActivity.getQrCodeOssUrl().isEmpty()) {
            try {
                String qrCodeUrl = dbActivity.getQrCodeOssUrl();
                // 从URL中提取OSS对象键
                String objectKey = extractObjectKeyFromUrl(qrCodeUrl);
                if (objectKey != null && !objectKey.isEmpty()) {
                    String bucketName = aliOSSUtil.getBucketName();
                    aliOSSUtil.deleteObject(bucketName, objectKey);
                    log.info("已删除活动 {} 的二维码: {}", id, objectKey);
                }
            } catch (Exception e) {
                throw new BaseException("删除活动 " + id + " 的二维码失败");
            }
        }

        return Result.success();
    }
    
    /**
     * 从二维码URL中提取OSS对象键
     * @param qrCodeUrl 完整的二维码URL
     * @return OSS对象键
     */
    private String extractObjectKeyFromUrl(String qrCodeUrl) {
        if (qrCodeUrl == null || qrCodeUrl.isEmpty()) {
            return null;
        }
        
        try {
            // 查找.aliyuncs.com/后的内容作为对象键
            int index = qrCodeUrl.indexOf(".aliyuncs.com/");
            if (index != -1) {
                return qrCodeUrl.substring(index + 14); // .aliyuncs.com/ 的长度是14
            }
            
            // 如果没有找到.aliyuncs.com/，尝试其他方式
            // 假设URL格式为 https://bucket.endpoint/objectKey
            java.net.URI uri = java.net.URI.create(qrCodeUrl);
            String path = uri.getPath();
            if (path != null && path.startsWith("/")) {
                return path.substring(1); // 移除开头的 "/"
            }
            
            return path;
        } catch (Exception e) {
            throw new BaseException("解析二维码URL失败: " + qrCodeUrl);
        }
    }
    
    /**
     * 更新活动相关的Redis数据
     * @param oldActivity 旧活动数据
     * @param newActivity 新活动数据
     */
    private void updateActivityRedisData(Activity oldActivity, Activity newActivity) {
        Long activityId = newActivity.getId();
        LocalDateTime now = LocalDateTime.now();
        
        // 检查报名相关信息是否发生变化
        boolean registrationChanged = 
            (oldActivity.getMaxParticipants() != null && 
             !oldActivity.getMaxParticipants().equals(newActivity.getMaxParticipants())) ||
            (oldActivity.getRegistrationStart() != null && 
             !oldActivity.getRegistrationStart().equals(newActivity.getRegistrationStart())) ||
            (oldActivity.getRegistrationEnd() != null && 
             !oldActivity.getRegistrationEnd().equals(newActivity.getRegistrationEnd()));
        
        // 如果报名相关信息发生变化，更新Redis中的报名数据
        if (registrationChanged) {
            String registrationKey = REGISTRATION_ACTIVITY_KEY + activityId;
            
            // 如果Redis中存在该活动的报名数据，则更新
            String currentParticipantsStr = stringRedisTemplate.opsForValue().get(registrationKey);
            if (currentParticipantsStr != null) {
                // 计算剩余名额
                int maxParticipants = newActivity.getMaxParticipants() != null ? newActivity.getMaxParticipants() : 0;
                int currentParticipants = oldActivity.getCurrentParticipants() != null ? oldActivity.getCurrentParticipants() : 0;
                
                // 更新最大报名人数，并保持当前已报名人数不变
                Duration duration = Duration.between(now, newActivity.getRegistrationEnd());
                stringRedisTemplate.opsForValue().set(
                    registrationKey, 
                    String.valueOf(maxParticipants), 
                    duration
                );
            }
        }
        
        // 检查活动时间相关信息是否发生变化
        boolean activityTimeChanged = 
            (oldActivity.getActivityStart() != null && 
             !oldActivity.getActivityStart().equals(newActivity.getActivityStart())) ||
            (oldActivity.getActivityEnd() != null && 
             !oldActivity.getActivityEnd().equals(newActivity.getActivityEnd())) ||
            (oldActivity.getLatitude() != null && 
             !oldActivity.getLatitude().equals(newActivity.getLatitude())) ||
            (oldActivity.getLongitude() != null && 
             !oldActivity.getLongitude().equals(newActivity.getLongitude()));
        
        // 如果活动时间或位置信息发生变化，记录日志（实际的签到数据会在活动开始时重新加载）
        if (activityTimeChanged) {
            log.info("活动 {} 的时间或位置信息已更新，将在下次定时任务执行时生效", activityId);
        }
    }
}