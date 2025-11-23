package com.activity.manage.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.activity.manage.pojo.dto.AdministratorDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static com.activity.manage.utils.constant.RedisConstant.LOGIN_ADMIN_KEY;

/**
 * Token 相关工具方法
 */
public class TokenUtil {

    /**
     * 从请求中获取请求头的token
     * @return token
     */
    public static String getTokenFromRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            return null;
        }
        return token;
    }

    /**
     * 从token中获取管理员账户信息
     * @param token
     * @param stringRedisTemplate
     * @return
     */
    public static AdministratorDTO getAdminFromToken(String token, StringRedisTemplate stringRedisTemplate) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        Map<Object, Object> adminMap = stringRedisTemplate.opsForHash().entries(LOGIN_ADMIN_KEY + token);
        if (adminMap == null || adminMap.isEmpty()) {
            return null;
        }
        AdministratorDTO administratorDTO = BeanUtil.fillBeanWithMap(adminMap, new AdministratorDTO(), false);
        return administratorDTO;
    }

    /**
     * 根据给定的 token 从 redis 获取管理员信息并保存到 AdminHolder
     * @param token token 字符串
     * @param stringRedisTemplate redis 操作模板
     * @return AdministratorDTO 或 null
     */
    public static AdministratorDTO getAdminFromTokenAndSave(String token, StringRedisTemplate stringRedisTemplate) {
        AdministratorDTO administratorDTO = getAdminFromToken(token, stringRedisTemplate);
        if (administratorDTO != null) {
            AdminHolder.saveAdmin(administratorDTO);
        }
        return administratorDTO;
    }
}
