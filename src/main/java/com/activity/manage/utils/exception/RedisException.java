package com.activity.manage.utils.exception;

public class RedisException extends BaseException {
    public RedisException(String message) {
        super(message);
    }
    public RedisException() {
        super("数据存入 redis 时产生异常");
    }
}
