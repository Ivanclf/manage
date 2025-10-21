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
public class AdministratorPasswordDTO implements Serializable {

    private String oldPassword;

    private String newPassword;
}
