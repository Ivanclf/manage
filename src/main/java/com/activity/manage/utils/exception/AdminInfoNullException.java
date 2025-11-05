package com.activity.manage.utils.exception;

public class AdminInfoNullException extends BaseException{
    public AdminInfoNullException() {
        super("用户的账号和密码不能为空");
    }
}
