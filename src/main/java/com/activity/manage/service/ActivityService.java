package com.activity.manage.service;

import cn.hutool.core.bean.BeanUtil;
import com.activity.manage.mapper.ActivityMapper;
import com.activity.manage.pojo.dto.ActivityDTO;
import com.activity.manage.pojo.dto.AdministratorDTO;
import com.activity.manage.pojo.entity.Activity;
import com.activity.manage.utils.AdminHolder;
import com.activity.manage.utils.constant.ActivityConstant;
import com.activity.manage.utils.result.Result;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ActivityService {

    @Autowired
    private ActivityMapper activityMapper;

    /**
     * 1. 创建活动 (POST /activity)
     */
    @Transactional
    public Result<Long> createActivity(ActivityDTO activityDTO) {
        // 1. 从 ThreadLocal 获取当前管理员
        AdministratorDTO admin = AdminHolder.getAdmin();
        if (admin == null) {
            return Result.error("未登录或认证失败");
        }

        // 2. DTO 转换为 Entity
        Activity activity = BeanUtil.copyProperties(activityDTO, Activity.class);

        // 3. 补全业务字段
        activity.setCreatorId(admin.getId());
        activity.setCreateTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        activity.setStatus(ActivityConstant.UNRELEASED); // 初始状态：未发布
        activity.setCurrentParticipants(0);

        // 4. 插入数据库
        activityMapper.insert(activity);

        // 5. 返回活动ID (已通过 useGeneratedKeys 注入到 activity 对象中)
        return Result.success(activity.getId());
    }

    /**
     * 2. 查询/搜索活动 (GET /activity)
     */
    public Result<PageInfo<Activity>> searchActivities(String activityName, Integer status, Boolean isFull, String location, int pageNum, int pageSize) {
        // 1. 启动 PageHelper 分页 (pom.xml 中已包含依赖)
        PageHelper.startPage(pageNum, pageSize);

        // 2. 构造查询参数
        Map<String, Object> params = new HashMap<>();
        params.put("activityName", activityName);
        params.put("status", status);
        params.put("isFull", isFull);
        params.put("location", location);

        // 3. 执行查询
        List<Activity> list = activityMapper.selectByParams(params);

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
            return Result.error("活动不存在");
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
            return Result.error("活动不存在");
        }

        // 2. 检查权限 (允许所有管理员修改)
        AdministratorDTO admin = AdminHolder.getAdmin();
        if (admin == null) {
            return Result.error("未登录或认证失败");
        }

        // 3. DTO 转换为 Entity
        Activity activityToUpdate = BeanUtil.copyProperties(activityDTO, Activity.class);

        // 4. 设置关键字段
        activityToUpdate.setId(id);
        activityToUpdate.setUpdateTime(LocalDateTime.now());

        // 5. 执行更新 (Mapper.xml 中使用动态SQL，只会更新 DTO 中非 null 的字段)
        activityMapper.update(activityToUpdate);
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
            return Result.error("活动不存在");
        }

        // 2. 执行删除
        activityMapper.deleteById(id);

        // TODO: 3. 清理相关数据（例如：报名信息、Redis缓存等）
        // registrationMapper.deleteByActivityId(id);
        // stringRedisTemplate.delete(REGISTRATION_ACTIVITY_KEY + id);

        return Result.success();
    }
}