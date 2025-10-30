package com.activity.manage.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class QRCodeUtil {
    /**
     * 生成一个二维码
     * @param content
     * @param width
     * @param height
     * @return BufferedImage
     * @throws WriterException
     */
    public BufferedImage generateQRCodeImage(String content, int width, int height) throws WriterException {
        Map<EncodeHintType, Object> hints = generateHints();
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);

        /**
         * 容错级别有四种
         * - L 7%左右错误
         * - M 15%左右错误
         * - Q 25%左右错误
         * - H 30%左右错误
         */
    }

    /**
     * 生成一个二维码
     * @param content
     * @param width
     * @param height
     * @return byte[]
     * @throws WriterException
     */
    public byte[] generateQRCodeBytes(String content, int width, int height) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = generateHints();
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * 生成二维码编码提示参数映射表
     *
     * @return 包含编码参数的映射表，包括字符集、容错级别和边框宽度设置
     */
    private Map<EncodeHintType, Object> generateHints() {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8"); // 默认编码：UTF-8
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q); // 容错级别：cite3
        hints.put(EncodeHintType.MARGIN, 1); // 二维码边框空白宽度为1
        return hints;
    }
}
