package com.activity.manage.utils.exception;

public class IllegalParamException extends BaseException {
    public IllegalParamException(String message) {
        super("请输入正确的" + message);
    }
}
