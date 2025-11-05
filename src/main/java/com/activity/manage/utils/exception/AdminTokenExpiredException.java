package com.activity.manage.utils.exception;

public class AdminTokenExpiredException extends BaseException {
    public AdminTokenExpiredException() {
        super("未登录或认证超时，请重新登录");
    }
}
