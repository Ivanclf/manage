package com.activity.manage.service;

import com.activity.manage.utils.QRCodeUtil;
import com.google.zxing.WriterException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class QRCodeService {

    public ResponseEntity<byte[]> generateQRCode(String content, int width, int height) throws WriterException, IOException {
        byte[] qrCodeBytes = QRCodeUtil.generateQRCodeBytes(content, width, height);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("Cache-Control", "public, max-age=3600")
                .body(qrCodeBytes);
    }
}
