package com.activity.manage.utils;

import cn.hutool.core.util.StrUtil;

public class RegexUtil {

    /**
     * 校验电话号是否正确
     * @param phone
     * @return
     */
    public static boolean isPhoneValid(String phone) { return mismatch(phone, "^1([38][0-9]|4[579]|5[0-3,5-9]|6[6]|7[0135678]|9[89])\\d{8}$");}

    // 校验是否不符合正则格式
    private static boolean mismatch(String str, String regex){
        if (StrUtil.isBlank(str)) {
            return true;
        }
        return !str.matches(regex);
    }
}
