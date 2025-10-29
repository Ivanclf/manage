package com.activity.manage.utils;

import com.activity.manage.config.AliOssConfig;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Locale;

@Component
@Slf4j
public class AliOSSUtil {

    @Autowired
    private AliOssConfig aliOssConfig;
    private OSS ossClient;

    @PostConstruct
    public void init() {
        try {
            ossClient = new OSSClientBuilder().build(
                    aliOssConfig.getEndpoint(),
                    aliOssConfig.getAccessKeyId(),
                    aliOssConfig.getAccessKeySecret()
            );
            log.info("初始化 AliOSS 客户端，endpoint={}, bucket={}", aliOssConfig.getEndpoint(), aliOssConfig.getBucketName());
        } catch (Exception e) {
            log.error("初始化 AliOSS 客户端失败", e);
            // ossClient 保持为 null，后续调用会检查并抛出
        }
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            try {
                ossClient.shutdown();
            } catch (Exception e) {
                log.warn("关闭 AliOSS 客户端时发生错误（忽略）: {}", e.getMessage());
            }
        }
    }

    /**
     * 将二维码字节上传到 OSS，基于内容 MD5 去重（以 md5.png 命名）。
     * 若已存在则直接返回已存在文件的 URL。
     * @param bytes 二维码 PNG 字节
     * @return 可访问的文件 URL
     */
    public String uploadQRCode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("bytes 为空");
        }
        if (ossClient == null) {
            throw new IllegalStateException("OSS 客户端未初始化");
        }
        try {
            String md5 = md5Hex(bytes);
            String key = "qrcodes/" + md5 + ".png";
            String bucket = aliOssConfig.getBucketName();
            // 如果对象已存在，则返回带签名的私有访问 URL
            if (ossClient.doesObjectExist(bucket, key)) {
                log.info("对象已存在，直接返回签名 URL: {}", key);
                return generatePresignedUrl(key, 3600);
            }

            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(bytes.length);
            meta.setContentType("image/png");
            ossClient.putObject(bucket, key, is, meta);
            log.info("上传二维码到 OSS 完成: {}", key);
            return generatePresignedUrl(key, 3600);
        } catch (Exception e) {
            log.error("上传二维码到 OSS 失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 直接使用 content/尺寸生成二维码并上传
     */
    public String uploadQRCode(String content, int width, int height) {
        // 添加参数验证
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("content 不能为空");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width 和 height 必须大于0");
        }

        try {
            byte[] bytes = QRCodeUtil.generateQRCodeBytes(content, width, height);
            return uploadQRCode(bytes);
        } catch (Exception e) {
            log.error("生成或上传二维码失败", e);
            throw new RuntimeException(e);
        }
    }

    private String generateUrl(String key) {
        // 备用：若需要公开 URL，可使用此方法；当前项目使用私有 bucket 的签名 URL
        String endpoint = aliOssConfig.getEndpoint();
        String bucket = aliOssConfig.getBucketName();
        String cleanedEndpoint = endpoint.replaceFirst("^https?://", "");
        return String.format(Locale.ROOT, "https://%s.%s/%s", bucket, cleanedEndpoint, key);
    }

    /**
     * 为私有 Bucket 生成预签名 URL
     * @param key 对象 key
     * @param expireSeconds 失效时长（秒）
     * @return 可访问的签名 URL
     */
    private String generatePresignedUrl(String key, long expireSeconds) {
        if (ossClient == null) {
            throw new IllegalStateException("OSS 客户端未初始化");
        }
        String bucket = aliOssConfig.getBucketName();
        Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000);
        try {
            URL url = ossClient.generatePresignedUrl(bucket, key, expiration);
            return url.toString();
        } catch (Exception e) {
            log.error("生成预签名 URL 失败", e);
            // 回退为公开 URL（如果 endpoint/bucket 配置允许）
            return generateUrl(key);
        }
    }

    private String md5Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 算法不可用", e);
        }
    }

}
