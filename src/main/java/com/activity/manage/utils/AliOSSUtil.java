package com.activity.manage.utils;

import com.activity.manage.config.AliOssConfig;
import com.activity.manage.utils.exception.BaseException;
import com.activity.manage.utils.exception.NullParamException;
import com.activity.manage.utils.exception.ResourcesException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.*;

import static com.activity.manage.utils.constant.QRCodeConstant.QRCODE_FORMAT;

@Component
@Slf4j
public class AliOSSUtil {

    @Autowired
    private AliOssConfig aliOssConfig;
    private OSS ossClient;

    @PostConstruct
    public void init() throws RuntimeException{
        try {
            ossClient = new OSSClientBuilder().build(
                    aliOssConfig.getEndpoint(),
                    aliOssConfig.getAccessKeyId(),
                    aliOssConfig.getAccessKeySecret()
            );
            log.info("初始化 AliOSS 客户端，endpoint={}, bucket={}", aliOssConfig.getEndpoint(), aliOssConfig.getBucketName());
        } catch (Exception e) {
            throw new BaseException("初始化 AliOSS 客户端失败");
        }
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            try {
                ossClient.shutdown();
                log.info("AliOSS 客户端已成功关闭");
            } catch (Exception e) {
                throw new BaseException("AliOSS 客户端关闭失败");
            }
        }
    }

    /**
     * 将二维码字节上传到 OSS，基于内容 MD5 去重（以 md5.png 命名）。
     * 若已存在则直接返回已存在文件的 URL。
     * @param bytes 二维码 PNG 字节
     * @return 可访问的文件 URL
     */
    public String upload(byte[] bytes, QRCodeMap QRCodeMap) {
        if (bytes == null || bytes.length == 0) {
            log.warn("byte为空");
            throw new NullParamException();
        }
        if(QRCodeMap == null || QRCodeMap.getQRCodeMap().isEmpty()) {
            log.warn("参数为空");
            throw new NullParamException();
        }

        Map<String, String> map = QRCodeMap.getQRCodeMap();
        try {
            String md5 = Md5Util.md5Str(map.get("id"));
            String key = map.get("route") + md5 + map.get("format");
            String bucket = aliOssConfig.getBucketName();
            // 如果对象已存在，则返回带签名的私有访问 URL
            if (ossClient.doesObjectExist(bucket, key)) {
                log.info("对象已存在，直接返回签名 URL: {}", key);
                return generatePresignedUrl(key, 3600);
            }

            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(bytes.length);
            switch (map.get("format")) {
                case QRCODE_FORMAT -> {
                    meta.setContentType("image/png");
                }
                default -> {
                    throw new BaseException("传输的文件格式错误");
                }
            }
            ossClient.putObject(bucket, key, is, meta);
            log.info("上传二维码到 OSS 完成: {}", key);
            return generatePresignedUrl(key, 3600);
        } catch (Exception e) {
            throw new BaseException("上传二维码到 OSS 失败");
        }
    }

    /**
     * 为私有 Bucket 生成预签名 URL
     * @param key 对象 key
     * @param expireSeconds 失效时长（秒）
     * @return 可访问的签名 URL
     */
    public String generatePresignedUrl(String key, long expireSeconds) {
        String bucket = aliOssConfig.getBucketName();
        Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000);
        try {
            URL url = ossClient.generatePresignedUrl(bucket, key, expiration);
            return url.toString();
        } catch (Exception e) {
            throw new ResourcesException("url");
        }

    }

    /**
     * 从指定的OSS URL获取图片字节数据
     * @param objectKey OSS对象键（不是完整URL）
     * @return 图片的字节数据
     */
    public byte[] getImage(String objectKey) {
        try {
            if(objectKey == null) {
                throw new NullParamException();
            }
            String bucket = aliOssConfig.getBucketName();
            if(!ossClient.doesObjectExist(bucket, objectKey)) {
                throw new ResourcesException("图片");
            }
            // 获取图片流
            OSSObject ossObject = ossClient.getObject(bucket, objectKey);
            // 转换为字节数组
            byte[] bytes = ossObject.getObjectContent().readAllBytes();
            log.info("从OSS获取图片成功，对象键: {}", objectKey);
            return bytes;
        } catch (Exception e) {
            throw new ResourcesException("图片");
        }
    }

    public List<String> listObjectsByPrefix(String prefix) {
        try {
            String bucket = aliOssConfig.getBucketName();
            ListObjectsRequest listObjectsRequest =
                    new ListObjectsRequest(bucket);
            listObjectsRequest.setPrefix(prefix);

            ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);
            List<String> objectKeys = new ArrayList<>();

            for (OSSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                String key = objectSummary.getKey();
                // 跳过目录标记和空对象
                if(!key.endsWith("/") && objectSummary.getSize() > 0) {
                    objectKeys.add(key);
                }
            }

            log.info("列出路径 {} 下的对象共 {} 个", prefix, objectKeys.size());
            return objectKeys;
        } catch (Exception e) {
            throw new ResourcesException("模板");
        }
    }

    /**
     * 获取OSS存储桶名称
     * @return 存储桶名称
     */
    public String getBucketName() {
        return aliOssConfig.getBucketName();
    }

    /**
     * 删除OSS对象
     * @param bucketName 存储桶名称
     * @param objectKey 对象键
     */
    public void deleteObject(String bucketName, String objectKey) {
        ossClient.deleteObject(bucketName, objectKey);
    }
}
