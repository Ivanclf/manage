package com.activity.manage.controller;

import cn.hutool.core.util.ObjectUtil;
import com.activity.manage.pojo.dto.CheckinDTO;
import com.activity.manage.pojo.dto.RegistrationDTO;
import com.activity.manage.pojo.vo.Activity2RegisterVO;
import com.activity.manage.service.QRCodeService;
import com.activity.manage.service.RegistrationService;
import com.activity.manage.utils.RegexUtil;
import com.activity.manage.utils.result.Result;
import com.google.zxing.WriterException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.IOException;
import java.util.List;

import static com.activity.manage.utils.constant.QRCodeConstant.*;

@RestController
@RequestMapping("/registration")
@Slf4j
public class RegistrationController {
    @Autowired
    private QRCodeService qrCodeService;
    @Autowired
    private RegistrationService registrationService;

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

    /**
     * 报名表单提交
     * @param registrationDTO
     * @return
     */
    @PostMapping
    public Result registration(@RequestBody RegistrationDTO registrationDTO) {
        if(!RegexUtil.isPhoneValid(registrationDTO.getPhone())) {
            return Result.error("输入的电话号不正确");
        }
        return registrationService.registration(registrationDTO);
    }

    /**
     * 用户查看报名信息
     * @param phone
     * @return
     */
    @GetMapping
    public Result<List<Activity2RegisterVO>> queryRegistrationInfo(@RequestParam("phone") String phone) {
        if(!RegexUtil.isPhoneValid(phone)) {
            return Result.error("错误的手机格式");
        }
        // TODO 完成相关service中封装类和批量查询的功能
        return Result.success();
    }

    @PostMapping("/checkin")
    public Result checkinConfirm(@RequestBody CheckinDTO checkinDTO) {
        if(ObjectUtil.hasEmpty(checkinDTO.getId(), checkinDTO.getPhone(), checkinDTO.getLatitude(), checkinDTO.getLongitude())) {
            return Result.error("未提交完整数据");
        }
        return registrationService.checkinConfirm(checkinDTO);
    }
}
