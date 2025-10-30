package com.activity.manage.mapper;

import com.activity.manage.pojo.entity.Activity;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

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
     * 动态条件查询
     * @param activity
     * @return
     */
    List<Activity> select(Activity activity);

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

    List<Activity> selectByRegistrationStart(LocalDateTime time);

    List<Activity> selectByActivityStart(LocalDateTime time);

    List<Activity> selectByIdBatch(List<Long> ids);
}