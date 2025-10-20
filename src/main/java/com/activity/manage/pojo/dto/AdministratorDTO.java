package com.activity.manage.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdministratorDTO {
    // 管理员id
    private Integer id;
    // 管理员账号
    private String userName;
}
