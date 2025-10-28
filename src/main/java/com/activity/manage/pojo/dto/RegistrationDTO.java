package com.activity.manage.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDTO implements Serializable {
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
}
