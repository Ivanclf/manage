package com.activity.manage.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Util {
    /**
     * 计算字节数组的MD5哈希值并返回十六进制字符串表示
     *
     * @param bytes 需要计算MD5哈希值的字节数组
     * @return MD5哈希值的十六进制字符串表示
     * @throws RuntimeException 当MD5算法不可用时抛出运行时异常
     */
    public static String md5Hex(byte[] bytes) {
        if(bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 算法不可用", e);
        }
    }

    /**
     * 计算字符串的MD5哈希值
     * @param str
     * @return
     */
    public static String md5Str(String str) {
        return md5Hex(str.getBytes());
    }
}
