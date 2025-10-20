package com.activity.manage.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Administrator implements Serializable {

    private static final long serialVersionUID = 1L;
    // 管理员id
    private Integer id;
    // 管理员账号
    private String userName;
    // 管理员密码（MD5加密）
    private String userPassword;

}
