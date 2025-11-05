package com.activity.manage.utils.exception;

public class AdminInfoErrorException extends BaseException {
    public AdminInfoErrorException() {
        super("账号或密码错误");
    }
}