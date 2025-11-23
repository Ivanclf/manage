package com.activity.manage.utils.constant;

import org.springframework.beans.factory.annotation.Value;

public class QRCodeConstant {

    /**
     * 默认二维码大小
     */
    public static final String DEFAULT_SIZE = "300";
    /**
     * 二维码存放到OSS上的路径
     */
    public static final String QRCODE_ACTIVITY_ROUTE = "qrcodes/activity/";
    public static final String QRCODE_REGISTRATION_ROUTE = "qrcodes/registration/";
    public static final String QRCODE_CHECKIN_ROUTE = "qrcodes/checkin/";
    /**
     * 模板路径
     */
    public static final String TEMPLATE_ROUTE = "template/";
    /**
     * 组合图片路径
     */
    public static final String POSTER_ACTIVITY_ROUTE = "poster/activity/";
    public static final String POSTER_REGISTRATION_ROUTE = "poster/registration/";
    public static final String POSTER_CHECKIN_ROUTE = "poster/checkin/";
    /**
     * 二维码的图片格式
     */
    public static final String QRCODE_FORMAT = ".png";
}
