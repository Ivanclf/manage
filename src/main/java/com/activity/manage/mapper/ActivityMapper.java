package com.activity.manage.mapper;

import com.activity.manage.pojo.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ActivityMapper {

    /**
     * 插入活动，并返回自增ID
     * @param activity
     * @return
     */
    int insert(Activity activity);

    /**
     * 根据ID查询
     * @param id
     * @return
     */
    Activity selectById(Long id);

    /**
     * 动态条件查询（用于搜索和分页）
     * @param params
     * @return
     */
    List<Activity> selectByParams(Map<String, Object> params);

    /**
     * 动态更新
     * @param activity
     * @return
     */
    int update(Activity activity);

    /**
     * 根据ID删除
     * @param id
     * @return
     */
    int deleteById(Long id);

    /**
     * 根据时间范围和状态查询活动（用于定时任务）
     * @param params (status, fieldName, startTime, endTime)
     * @return
     */
    List<Activity> selectByTimeRange(Map<String, Object> params);
}