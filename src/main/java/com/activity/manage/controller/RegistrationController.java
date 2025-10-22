package com.activity.manage.controller;

import com.activity.manage.service.QRCodeService;
import com.activity.manage.utils.QRCodeUtil;
import com.google.zxing.WriterException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.IOException;

import static com.activity.manage.utils.constant.QRCodeConstant.*;

@RestController
@RequestMapping("/registration")
@Slf4j
public class RegistrationController {
    @Autowired
    private QRCodeService qrCodeService;

    /**
     * 生成活动报名二维码
     * @param id
     * @param width
     * @param height
     * @return
     * @throws WriterException
     * @throws IOException
     */
    @GetMapping(value = "/{id}/registration/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getRegistrationQRCode(@PathVariable("id") Long id,
                                            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(100) @Max(1000) int width,
                                            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(100) @Max(1000) int height) throws WriterException, IOException {
        String content = REGISTRATION_PAGE;
        return qrCodeService.generateQRCode(content, width, height);
    }

    /**
     * 生成签到二维码
     * @param id
     * @param width
     * @param height
     * @return
     * @throws WriterException
     * @throws IOException
     */
    @GetMapping(value = "/{id}/checkin/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getCheckinQRCode(@PathVariable("id") Long id,
                                            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(100) @Max(1000) int width,
                                            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(100) @Max(1000) int height) throws WriterException, IOException {
        String content = CHECKIN_PAGE;
        return qrCodeService.generateQRCode(content, width, height);
    }
}
