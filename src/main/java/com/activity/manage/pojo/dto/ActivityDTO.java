package com.activity.manage.pojo.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动的数据传输对象，用于创建和更新
 */
@Data
public class ActivityDTO implements Serializable {

    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 活动简介
     */
    private String activityDescription;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 位置描述
     */
    private String location;

    /**
     * 相关链接
     */
    private String link;

    /**
     * 报名开始时间
     */
    private LocalDateTime registrationStart;

    /**
     * 报名结束时间
     */
    private LocalDateTime registrationEnd;

    /**
     * 活动开始时间
     */
    private LocalDateTime activityStart;

    /**
     * 活动结束时间
     */
    private LocalDateTime activityEnd;

    /**
     * 最大报名人数
     */
    private Integer maxParticipants;
}