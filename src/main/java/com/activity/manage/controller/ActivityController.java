package com.activity.manage.controller;

import com.activity.manage.service.QRCodeService;
import com.google.zxing.WriterException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.IOException;

import static com.activity.manage.utils.constant.QRCodeConstant.ACTIVITY_PAGE;
import static com.activity.manage.utils.constant.QRCodeConstant.DEFAULT_SIZE;

@RestController
@RequestMapping("/activity")
@Slf4j
public class ActivityController {
    @Autowired
    private QRCodeService qrCodeService;

    /**
     * 生成活动网页二维码
     * @param id
     * @param width
     * @param height
     * @return
     * @throws WriterException
     * @throws IOException
     */
    @GetMapping(value = "/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQRCode(@PathVariable("id") Long id,
                                            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(100) @Max(1000) int width,
                                            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(100) @Max(1000) int height) throws WriterException, IOException {
        String content = ACTIVITY_PAGE;
        return qrCodeService.generateQRCode(content, width, height);
    }


}
