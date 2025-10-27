package com.activity.manage.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 活动id
     */
    private Long id;

    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 活动简介
     */
    private String activityDescription;

    /**
     * 活动状态，为ActivityConstant中的数据
     */
    private Integer status;

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
     * 二维码存储OSS
     */
    private String qrCodeOssUrl;

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
     * 发布时间
     */
    private LocalDateTime releaseTime;

    /**
     * 创建管理员的id
     */
    private Integer creatorId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 最大报名人数
     */
    private Integer maxParticipants;

    /**
     * 目前报名人数
     */
    private Integer currentParticipants;
}
