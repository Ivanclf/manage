package com.activity.manage.utils;

import java.util.HashMap;
import java.util.Map;

public class QRCodeMap {
    private final Map<String, String> QRCODEMAP;

    public QRCodeMap(String route, String id, String format) {
        QRCODEMAP = new HashMap<>();
        QRCODEMAP.put("route", route);
        QRCODEMAP.put("id", id);
        QRCODEMAP.put("format", format);
    }

    public Map<String, String> getQRCodeMap() {
        return QRCODEMAP;
    }
}
