package com.activity.manage.utils.exception;

public class ResourcesException extends BaseException {
    public ResourcesException(String message) {
        super("获取" + message + "失败");
    }
}
