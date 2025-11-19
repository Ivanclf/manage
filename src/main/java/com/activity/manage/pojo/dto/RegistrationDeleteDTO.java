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
public class RegistrationDeleteDTO implements Serializable {
    /**
     * 活动id
     */
    private Long activityId;
    /**
     * 电话
     */
    private String phone;
}
