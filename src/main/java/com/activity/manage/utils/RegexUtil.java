package com.activity.manage.utils;

import cn.hutool.core.util.StrUtil;

public class RegexUtil {

    /**
     * 校验电话号是否正确
     * @param phone
     * @return
     */
    public static boolean isPhoneValid(String phone) { return mismatch(phone, "^1[3-9]\\d{9}$");}

    // 校验是否不符合正则格式
    private static boolean mismatch(String str, String regex){
        if (StrUtil.isBlank(str)) {
            return true;
        }
        return str.matches(regex);
    }
}
