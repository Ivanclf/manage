package com.activity.manage.controller;

import cn.hutool.core.util.ObjectUtil;
import com.activity.manage.config.UrlConfig;
import com.activity.manage.pojo.dto.RegistrationCheckinDTO;
import com.activity.manage.pojo.dto.RegistrationDTO;
import com.activity.manage.pojo.dto.RegistrationDeleteDTO;
import com.activity.manage.pojo.vo.Activity2RegisterVO;
import com.activity.manage.service.QRCodeService;
import com.activity.manage.service.RegistrationService;
import com.activity.manage.utils.RegexUtil;
import com.activity.manage.utils.exception.IllegalParamException;
import com.activity.manage.utils.exception.NullParamException;
import com.activity.manage.utils.result.Result;
import com.github.pagehelper.PageInfo;
import com.google.zxing.WriterException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private UrlConfig urlConfig;

    /**
     * 生成活动报名二维码
     * @param id
     * @param width
     * @param height
     * @return
     * @throws WriterException
     * @throws IOException
     */
    @GetMapping(value = "/{id}/registration/qrcode")
    public Result<String> getRegistrationQRCode(@PathVariable("id") Long id,
                                                @RequestParam(defaultValue = DEFAULT_SIZE) @Min(100) @Max(1000) int width,
                                                @RequestParam(defaultValue = DEFAULT_SIZE) @Min(100) @Max(1000) int height) throws WriterException, IOException {
        String content = urlConfig.getRegistrationPage() + "/registration/" + id.toString();
        return qrCodeService.generateQRCodeWithUrl(content, width, height, QRCODE_REGISTRATION_ROUTE, id);
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
    @GetMapping(value = "/{id}/checkin/qrcode")
    public Result<String> getCheckinQRCode(@PathVariable("id") Long id,
                                            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(100) @Max(1000) int width,
                                            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(100) @Max(1000) int height) throws WriterException, IOException {
        String content = urlConfig.getCheckinPage() + "/checkin/" + id.toString();
        return qrCodeService.generateQRCodeWithUrl(content, width, height, QRCODE_CHECKIN_ROUTE, id);
    }

    /**
     * 报名表单提交
     * @param registrationDTO
     * @return
     */
    @PostMapping
    public Result registration(@RequestBody RegistrationDTO registrationDTO) {
        if(!RegexUtil.isPhoneValid(registrationDTO.getPhone())) {
            throw new IllegalParamException("手机号");
        }
        return registrationService.registration(registrationDTO);
    }

    @DeleteMapping
    public Result registrationDelete(@RequestBody RegistrationDeleteDTO registrationDeleteDTO) {
        return registrationService.registrationDelete(registrationDeleteDTO);
    }

    /**
     * 用户查看报名信息
     * @param phone
     * @return
     */
    @GetMapping
    public Result<PageInfo<Activity2RegisterVO>> queryRegistrationInfo(
            @RequestParam("phone") String phone,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        if(!RegexUtil.isPhoneValid(phone)) {
            throw new IllegalParamException("手机号");
        }
        return registrationService.searchActivitiesByPhone(phone, pageNum, pageSize);
    }

    /**
     * 签到
     * @param registrationCheckinDTO
     * @return
     */
    @PostMapping("/checkin")
    public Result checkinConfirm(@RequestBody RegistrationCheckinDTO registrationCheckinDTO) {
        if(ObjectUtil.hasEmpty(registrationCheckinDTO.getActivityId(), registrationCheckinDTO.getPhone(), registrationCheckinDTO.getLatitude(), registrationCheckinDTO.getLongitude())) {
            throw new NullParamException();
        }
        return registrationService.checkinConfirm(registrationCheckinDTO);
    }
}
