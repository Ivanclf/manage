package com.activity.manage.utils.exception;

public class ActivityNotFoundException extends BaseException {
    public ActivityNotFoundException() {
        super("寻找的活动不存在");
    }
}
