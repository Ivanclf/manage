package com.activity.manage.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import com.activity.manage.mapper.AdminMapper;
import com.activity.manage.pojo.dto.AdministratorDTO;
import com.activity.manage.pojo.entity.Administrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.activity.manage.utils.constant.RedisConstant.LOGIN_CODE_KEY;
import static com.activity.manage.utils.constant.RedisConstant.LOGIN_CODE_TTL;

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
        String tokenKey = LOGIN_CODE_KEY + token;
        AdministratorDTO administratorDTO = BeanUtil.copyProperties(administrator, AdministratorDTO.class);
        Map<String, Object> adminMap = BeanUtil.beanToMap(administratorDTO, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(tokenKey, adminMap);
        stringRedisTemplate.expire(tokenKey, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.info("用户" + token + "登录成功");
        return token;
    }
}
