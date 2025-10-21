package com.activity.manage.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;

import com.activity.manage.mapper.AdminMapper;
import com.activity.manage.pojo.dto.AdministratorDTO;
import com.activity.manage.pojo.dto.AdministratorPasswordDTO;
import com.activity.manage.pojo.entity.Administrator;
import com.activity.manage.utils.AdminHolder;
import com.activity.manage.utils.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.activity.manage.utils.TokenUtil;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.activity.manage.utils.constant.RedisConstant.*;

@Service
@Slf4j
public class AdminService {
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public String login(Administrator administrator) {
        if(administrator.getUserPassword().isEmpty()) {
            return null;
        }
        Administrator admin = null;
        if(administrator.getId() != null) {
            admin = adminMapper.loginById(administrator);
        } else if(administrator.getUserName() != null) {
            admin = adminMapper.loginByName(administrator);
        }
        if(admin == null) {
            return null;
        }
        String token = UUID.randomUUID().toString(true);
        String tokenKey = LOGIN_ADMIN_KEY + token;
        AdministratorDTO administratorDTO = BeanUtil.copyProperties(administrator, AdministratorDTO.class);
        Map<String, Object> adminMap = BeanUtil.beanToMap(administratorDTO, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor(
                        (fieldName, fieldValue) -> fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(tokenKey, adminMap);
        stringRedisTemplate.expire(tokenKey, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.info("用户 " + token + " 登录成功");
        return token;
    }

    public void logout() {
        // 使用 TokenUtil 获取 token 并删除对应的 Redis key
        String token = TokenUtil.getTokenFromRequest();
        if (!StrUtil.isBlank(token)) {
            String tokenKey = LOGIN_ADMIN_KEY + token;
            stringRedisTemplate.delete(tokenKey);
        }
        // 清理线程本地存储的管理员信息
        AdminHolder.removeAdmin();
    }

    public Result updatePassword(AdministratorPasswordDTO administratorPasswordDTO) {
        AdministratorDTO administratorDTO = TokenUtil.getAdminFromRequest(stringRedisTemplate);
        Administrator administratorOld = BeanUtil.copyProperties(administratorDTO, Administrator.class);
        administratorOld.setUserPassword(administratorPasswordDTO.getOldPassword());
        Administrator administratorNew = adminMapper.queryById(administratorOld);
        if(administratorNew == null) {
            return Result.error("旧密码输入错误");
        }
        administratorNew.setUserPassword(administratorPasswordDTO.getNewPassword());
        adminMapper.update(administratorNew);
        return Result.success();
    }
}
