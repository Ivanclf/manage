package com.activity.manage.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.activity.manage.mapper.AdminMapper;
import com.activity.manage.pojo.dto.AdministratorDTO;
import com.activity.manage.pojo.dto.AdministratorPasswordDTO;
import com.activity.manage.pojo.dto.AdministratorUsernameDTO;
import com.activity.manage.pojo.entity.Administrator;
import com.activity.manage.utils.AdminHolder;
import com.activity.manage.utils.TokenUtil;
import com.activity.manage.utils.exception.AdminInfoErrorException;
import com.activity.manage.utils.exception.AdminInfoNullException;
import com.activity.manage.utils.exception.NullParamException;
import com.activity.manage.utils.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.activity.manage.utils.constant.RedisConstant.LOGIN_ADMIN_KEY;
import static com.activity.manage.utils.constant.RedisConstant.LOGIN_CODE_TTL;

@Service
@Slf4j
public class AdminService {
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Transactional
    public String login(Administrator administrator) {
        // 校验完整性
        if(administrator.getUserPassword().isEmpty()) {
            throw new AdminInfoNullException();
        }
        // 检查用户通过什么方式登录，有没有写入账号
        Administrator admin = null;
        if(administrator.getId() != null) {
            admin = adminMapper.loginById(administrator);
        } else if(administrator.getUserName() != null) {
            admin = adminMapper.loginByName(administrator);
        } else {
            throw new AdminInfoNullException();
        }
        if(admin == null) {
            throw new AdminInfoErrorException();
        }
        // 登录成功，获取token，放到redis中。redis的键为token，值为用户的id和账号
        String token = UUID.randomUUID().toString(true);
        String tokenKey = LOGIN_ADMIN_KEY + token;
        AdministratorDTO administratorDTO = BeanUtil.copyProperties(administrator, AdministratorDTO.class);
        Map<String, Object> adminMap = BeanUtil.beanToMap(administratorDTO, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor(
                        (fieldName, fieldValue) -> fieldValue == null ? null : fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(tokenKey, adminMap);
        stringRedisTemplate.expire(tokenKey, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.info("用户 " + token + " 登录成功");
        // 返回token给前端
        return token;
    }

    @Transactional
    public void logout() {
        // 使用 TokenUtil 获取 token 并删除对应的 Redis key
        String token = TokenUtil.getTokenFromRequest();
        if (!StrUtil.isBlank(token)) {
            String tokenKey = LOGIN_ADMIN_KEY + token;
            stringRedisTemplate.delete(tokenKey);
        } else {
            throw new NullParamException();
        }
        // 清理线程本地存储的管理员信息
        AdminHolder.removeAdmin();
    }

    @Transactional
    public Result updatePassword(AdministratorPasswordDTO administratorPasswordDTO) {
        // 从redis中获取用户信息
        AdministratorDTO administratorDTO = TokenUtil.getAdminFromRequest(stringRedisTemplate);
        Administrator administratorOld = BeanUtil.copyProperties(administratorDTO, Administrator.class);
        administratorOld.setUserPassword(administratorPasswordDTO.getOldPassword());
        // 查看旧密码有没有输入正确
        Administrator administratorNew = adminMapper.queryById(administratorOld);
        if(administratorNew == null) {
            throw new AdminInfoErrorException();
        }
        // 输入正确，更新密码
        administratorNew.setUserPassword(administratorPasswordDTO.getNewPassword());
        adminMapper.update(administratorNew);
        return Result.success();
    }

    @Transactional
    public void updateName(AdministratorUsernameDTO administratorUsernameDTO) {
        // 从redis里获取用户信息
        String token = LOGIN_ADMIN_KEY + TokenUtil.getTokenFromRequest();
        AdministratorDTO administratorDTO = TokenUtil.getAdminFromToken(token, stringRedisTemplate);
        if(administratorDTO == null) {
            throw new AdminInfoErrorException();
        }
        // 更改信息，更新数据库数据
        administratorDTO.setUserName(administratorUsernameDTO.getUserName());
        Administrator administrator = BeanUtil.copyProperties(administratorDTO, Administrator.class);
        adminMapper.update(administrator);
        // 写回redis
        Map<String, Object> adminMap = BeanUtil.beanToMap(administratorDTO, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor(
                        (fieldName, fieldValue) -> fieldValue == null ? null : fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(token, adminMap);
    }
}
