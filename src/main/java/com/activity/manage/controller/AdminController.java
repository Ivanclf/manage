package com.activity.manage.controller;

import com.activity.manage.pojo.entity.Administrator;
import com.activity.manage.service.AdminService;
import com.activity.manage.utils.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController {
    @Autowired
    private AdminService adminService;

    @PostMapping("/login")
    public Result<String> login(@RequestBody Administrator administrator) {
        var token = adminService.login(administrator);
        if(!token.isEmpty()) {
            return Result.success(token);
        } else {
            return Result.error("账号或密码错误");
        }
    }
}
