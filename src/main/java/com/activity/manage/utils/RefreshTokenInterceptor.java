package com.activity.manage.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.activity.manage.pojo.dto.AdministratorDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.activity.manage.utils.constant.RedisConstant.LOGIN_ADMIN_KEY;
import static com.activity.manage.utils.constant.RedisConstant.LOGIN_CODE_TTL;

public class RefreshTokenInterceptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头
        String token = request.getHeader("authorization");
        if(StrUtil.isBlank(token)) {
            return true;
        }
        String key = LOGIN_ADMIN_KEY + token;
        Map<Object, Object> adminMap = stringRedisTemplate.opsForHash().entries(key);
        if(adminMap.isEmpty()) {
            return true;
        }
        // 查询到的redis数据转为AdministratorDTO对象
        AdministratorDTO administratorDTO = BeanUtil.fillBeanWithMap(adminMap, new AdministratorDTO(), false);
        // 存在则保存信息到ThreadLocal
        AdminHolder.saveAdmin(administratorDTO);
        // 刷新有效期
        stringRedisTemplate.expire(key, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        AdminHolder.removeAdmin();
    }
}
