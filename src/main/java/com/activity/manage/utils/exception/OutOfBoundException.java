package com.activity.manage.utils.exception;

public class OutOfBoundException extends BaseException {
    public OutOfBoundException(String message) {
        super("不在" + message + "内");
    }
}
