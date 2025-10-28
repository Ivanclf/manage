package com.activity.manage.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity2RegisterVO {
    /**
     * 活动id
     */
    private Long id;
    /**
     * 活动名称
     */
    private String activityName;
    /**
     * 活动状态
     */
    private Integer status;
    /**
     * 是否已满
     */
    private Integer isFull;
    /**
     * 位置描述
     */
    private String location;
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
    private LocalDateTime maxParticipants;
    /**
     * 目前报名人数
     */
    private LocalDateTime currentParticipant;
}
