package com.activity.manage.controller;

import com.activity.manage.pojo.dto.ActivityDTO;
import com.activity.manage.pojo.entity.Activity;
import com.activity.manage.service.ActivityService;
import com.activity.manage.service.QRCodeService;
import com.activity.manage.utils.result.Result;
import com.github.pagehelper.PageInfo;
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

    @Autowired
    private ActivityService activityService;

    /**
     * 1. 创建活动
     * 接口文档: POST /activity
     */
    @PostMapping
    public Result<Long> createActivity(@RequestBody ActivityDTO activityDTO) {
        return activityService.createActivity(activityDTO);
    }

    /**
     * 2. 查询/搜索活动 (支持分页和过滤)
     * 接口文档: GET /activity
     */
    @GetMapping
    public Result<PageInfo<Activity>> searchActivities(
            @RequestParam(required = false) String activityName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean isFull,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        // 接口文档 要求 data 即为分页对象
        return activityService.searchActivities(activityName, status, isFull, location, pageNum, pageSize);
    }

    /**
     * 3. 获取活动详情
     * 接口文档: GET /activity/{id}
     */
    @GetMapping("/{id}")
    public Result<Activity> getActivityById(@PathVariable Long id) {
        return activityService.getActivityById(id);
    }

    /**
     * 4. 更新活动
     * 接口文档: PUT /activity/{id}
     */
    @PutMapping("/{id}")
    public Result updateActivity(@PathVariable Long id, @RequestBody ActivityDTO activityDTO) {
        return activityService.updateActivity(id, activityDTO);
    }

    /**
     * 5. 删除活动
     * 接口文档: DELETE /activity/{id}
     */
    @DeleteMapping("/{id}")
    public Result deleteActivity(@PathVariable Long id) {
        return activityService.deleteActivity(id);
    }


    /**
     * 6. 生成活动网页二维码 (你已有的功能)
     * 接口文档: GET /activity/{id}/qrcode
     */
    @GetMapping(value = "/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public Result<String> getQRCode(@PathVariable("id") Long id,
                                    @RequestParam(defaultValue = DEFAULT_SIZE) @Min(100) @Max(1000) int width,
                                    @RequestParam(defaultValue = DEFAULT_SIZE) @Min(100) @Max(1000) int height) throws WriterException, IOException {
        // 优化：二维码内容应指向具体活动，而不仅仅是首页
        String content = ACTIVITY_PAGE;
        return qrCodeService.generateQRCodeWithUrl(content, width, height);
    }
}