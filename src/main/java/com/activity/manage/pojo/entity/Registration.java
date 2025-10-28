package com.activity.manage.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.transform.sax.SAXResult;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Registration implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 报名人员id
     */
    private Long id;
    /**
     * 活动id
     */
    private Long activityId;
    /**
     * 报名人
     */
    private String registrationName;
    /**
     * 学院
     */
    private String college;
    /**
     * 电话
     */
    private String phone;
    /**
     * 报名时间
     */
    private LocalDateTime registrationTime;
    /**
     * 是否已签到。0为未签到，1为已签到
     */
    private Integer checkin;
}
