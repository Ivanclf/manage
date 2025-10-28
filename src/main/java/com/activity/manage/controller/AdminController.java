package com.activity.manage.controller;

import com.activity.manage.pojo.dto.AdministratorPasswordDTO;
import com.activity.manage.pojo.dto.AdministratorUsernameDTO;
import com.activity.manage.pojo.entity.Administrator;
import com.activity.manage.service.AdminService;
import com.activity.manage.utils.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController {
    @Autowired
    private AdminService adminService;

    /**
     * 登录功能
     * @param administrator
     * @return
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody Administrator administrator) {
        var token = adminService.login(administrator);
        if(token != null && !token.isEmpty()) {
            return Result.success(token);
        } else {
            return Result.error("账号或密码错误");
        }
    }

    /**
     * 登出功能
     * @return
     */
    @PostMapping("/logout")
    public Result logout() {
        adminService.logout();
        return Result.success();
    }

    /**
     * 认证功能
     * @return
     */
    @GetMapping("/authorization")
    public Result authorization() {
        return Result.success();
    }

    /**
     * 修改密码功能
     * @param administratorPasswordDTO
     * @return
     */
    @PutMapping("/password")
    public Result password(@RequestBody AdministratorPasswordDTO administratorPasswordDTO) {
        return adminService.updatePassword(administratorPasswordDTO);
    }

    /**
     * 修改账号功能
     * @param administratorUsernameDTO
     * @return
     */
    @PutMapping("/username")
    public Result username(@RequestBody AdministratorUsernameDTO administratorUsernameDTO) {
        if(administratorUsernameDTO.getUserName() == null || administratorUsernameDTO.getUserName().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        adminService.updateName(administratorUsernameDTO);
        return Result.success();
    }
}
