package com.activity.manage.controller;

import com.activity.manage.service.PosterService;
import com.activity.manage.utils.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/poster")
@Slf4j
public class PosterController {

    @Autowired
    private PosterService posterService;

    /**
     * 查询所有模板
     * @return 返回包含所有模板名称的列表结果
     */
    @GetMapping("/templates")
    public Result<List<String>> selectAllTemplates() {
        return posterService.selectAllTemplates();
    }

    /**
     * 合成海报接口
     * @param templateUrl 模板图片URL地址
     * @param qrCodeUrl 二维码图片URL地址
     * @param activityId 活动ID
     * @return 合成后的海报图片URL地址
     */
    @GetMapping("/combine")
    public Result<String> combine(@RequestParam("templateUrl") String templateUrl,
                                  @RequestParam("qrCodeUrl") String qrCodeUrl,
                                  @RequestParam("id") Long activityId) {
        return posterService.combine2Poster(templateUrl, qrCodeUrl, activityId);
    }

    @GetMapping
    public Result<List<String>> getPoster(@RequestParam("id") Long activityId) {
        return posterService.selectPostersById(activityId);
    }
}
