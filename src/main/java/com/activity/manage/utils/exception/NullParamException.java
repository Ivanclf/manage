package com.activity.manage.utils.exception;

public class NullParamException extends BaseException {
    public NullParamException() {
        super("输入的参数有空值");
    }
}
