package com.activity.manage.service;

import com.activity.manage.config.AliOssConfig;
import com.activity.manage.utils.AliOSSUtil;
import com.activity.manage.utils.Md5Util;
import com.activity.manage.utils.QRCodeMap;
import com.activity.manage.utils.exception.IllegalParamException;
import com.activity.manage.utils.exception.ResourcesException;
import com.activity.manage.utils.result.Result;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.activity.manage.utils.constant.QRCodeConstant.*;

@Service
@Slf4j
public class PosterService {

    @Autowired
    private AliOSSUtil aliOSSUtil;
    
    @Autowired
    private AliOssConfig aliOssConfig;

    public Result<List<String>> selectAllTemplates() {
        try {
            List<String> templateKeys = aliOSSUtil.listObjectsByPrefix(TEMPLATE_ROUTE);
            // 生成可访问的URL列表
            List<String> templateUrls = templateKeys.stream()
                    .map(key -> aliOSSUtil.generatePresignedUrl(key, 3600))
                    .collect(Collectors.toList());

            return Result.success(templateUrls);
        } catch (Exception e) {
            throw new ResourcesException("模板图片");
        }
    }

    public Result<String> combine2Poster(String templateUrl, String qrCodeUrl, Long activityId) {
        try {
            // 使用更可靠的方式解析URL，而不是硬编码索引
            // 从URL中提取对象键来判断类型
            String templateKey = extractKeyFromUrl(templateUrl);
            String qrCodeKey = extractKeyFromUrl(qrCodeUrl);
            
            String route;
            if (qrCodeKey.startsWith("qrcodes/activity/")) {
                route = POSTER_ACTIVITY_ROUTE;
            } else if (qrCodeKey.startsWith("qrcodes/registration/")) {
                route = POSTER_REGISTRATION_ROUTE;
            } else if (qrCodeKey.startsWith("qrcodes/checkin/")) {
                route = POSTER_CHECKIN_ROUTE;
            } else {
                throw new IllegalParamException("url");
            }
            
            QRCodeMap posterMap = new QRCodeMap(route, activityId.toString(), QRCODE_FORMAT);

            // 从OSS获取模板图片和二维码图片的字节数据
            byte[] templateBytes = aliOSSUtil.getImage(templateKey);
            byte[] qrCodeBytes = aliOSSUtil.getImage(qrCodeKey);

            // 创建临时文件用于存储下载的图片和生成的结果
            Path templatePathTemp = Files.createTempFile("template_", ".png");
            Path qrCodePathTemp = Files.createTempFile("qrcode_", ".png");
            Path outputPath = Files.createTempFile("poster_", ".png");

            // 将字节数据写入临时文件
            Files.write(templatePathTemp, templateBytes);
            Files.write(qrCodePathTemp, qrCodeBytes);

            // 使用 Jaffree 执行 FFmpeg 命令
            FFmpeg.atPath()
                    .addInput(UrlInput.fromPath(templatePathTemp))
                    .addInput(UrlInput.fromPath(qrCodePathTemp))
                    .setOverwriteOutput(true)
                    .addArguments("-filter_complex", "[1:v]scale=250:250[scaled];[0:v][scaled]overlay=125:175")
                    .addOutput(UrlOutput.toPath(outputPath))
                    .execute();

            // 读取生成的海报图片
            byte[] posterBytes = Files.readAllBytes(outputPath);

            // 清理临时文件
            Files.deleteIfExists(templatePathTemp);
            Files.deleteIfExists(qrCodePathTemp);
            Files.deleteIfExists(outputPath);

            // 将生成的海报上传到OSS并返回URL
            String posterUrl = aliOSSUtil.upload(posterBytes, posterMap);

            return Result.success(posterUrl);
        } catch (Exception e) {
            throw new ResourcesException("合成海报");
        }
    }

    /**
     * 从URL中安全地提取OSS对象键
     * @param url 完整的OSS URL
     * @return 对象键
     */
    private String extractKeyFromUrl(String url) {
        try {
            // 解析URL，提取对象键
            String bucketName = aliOSSUtil.getBucketName();
            String endpoint = aliOssConfig.getEndpoint().replaceFirst("^https?://", "");

            // 处理虚拟主机样式URL (bucket.endpoint/key)
            String host = bucketName + "." + endpoint;
            int startIndex = url.indexOf(host);
            if (startIndex != -1) {
                startIndex += host.length() + 1; // +1 for the trailing slash
            } else {
                // 处理路径样式URL (endpoint/bucket/key)
                int bucketIndex = url.indexOf("/" + bucketName + "/");
                if (bucketIndex != -1) {
                    startIndex = bucketIndex + bucketName.length() + 2; // +2 for the two slashes
                } else {
                    // 如果URL已经是对象键本身
                    if (!url.contains("://")) {
                        return url;
                    }
                    throw new IllegalParamException("url");
                }
            }

            // 查找查询参数的开始位置（如果有）
            int queryIndex = url.indexOf("?", startIndex);
            if (queryIndex != -1) {
                // 如果有查询参数，只取对象键部分
                return url.substring(startIndex, queryIndex);
            } else {
                // 没有查询参数，直接截取到字符串末尾
                return url.substring(startIndex);
            }
        } catch (Exception e) {
            log.error("Error extracting key from URL: {}", url, e);
            throw new IllegalParamException("url");
        }
    }

    public Result<List<String>> selectPostersById(Long activityId) {
        try {
            // 根据活动ID获取相关的海报
            List<String> keys = new ArrayList<>();
            
            // 构造特定活动的路径前缀
            String activityPosterPrefix = POSTER_ACTIVITY_ROUTE.substring(1) + Md5Util.md5Str(activityId.toString()) + "/";
            String registrationPosterPrefix = POSTER_REGISTRATION_ROUTE.substring(1) + Md5Util.md5Str(activityId.toString()) + "/";
            String checkinPosterPrefix = POSTER_CHECKIN_ROUTE.substring(1) + Md5Util.md5Str(activityId.toString()) + "/";
            
            // 添加所有匹配的海报键
            keys.addAll(aliOSSUtil.listObjectsByPrefix(activityPosterPrefix));
            keys.addAll(aliOSSUtil.listObjectsByPrefix(registrationPosterPrefix));
            keys.addAll(aliOSSUtil.listObjectsByPrefix(checkinPosterPrefix));
            
            // 生成可访问的URL列表
            List<String> posterUrls = keys.stream()
                    .map(key -> aliOSSUtil.generatePresignedUrl(key, 3600))
                    .collect(Collectors.toList());

            return Result.success(posterUrls);
        } catch (Exception e) {
            throw new ResourcesException("海报");
        }
    }
}
