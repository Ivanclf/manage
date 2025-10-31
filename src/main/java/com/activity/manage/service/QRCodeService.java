package com.activity.manage.service;

import com.activity.manage.utils.AliOSSUtil;
import com.activity.manage.utils.QRCodeMap;
import com.activity.manage.utils.QRCodeUtil;
import com.activity.manage.utils.result.Result;
import com.google.zxing.WriterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.activity.manage.utils.constant.QRCodeConstant.QRCODE_FORMAT;

@Service
public class QRCodeService {

    @Autowired
    private AliOSSUtil aliOSSUtil;
    @Autowired
    private QRCodeUtil qrCodeUtil;

    /**
     * 生成二维码图片并返回响应实体
     *
     * @param content 二维码包含的内容字符串
     * @param width 二维码图片宽度
     * @param height 二维码图片高度
     * @return 包含二维码图片字节数据的响应实体，类型为PNG图片
     * @throws WriterException 二维码生成过程中出现编码错误时抛出
     * @throws IOException 图片处理过程中出现IO异常时抛出
     */
    public ResponseEntity<byte[]> generateQRCode(String content, int width, int height) throws WriterException, IOException {
        byte[] qrCodeBytes = qrCodeUtil.generateQRCodeBytes(content, width, height);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("Cache-Control", "public, max-age=3600")
                .body(qrCodeBytes);
    }

    /**
     * 生成带URL的二维码图片并上传到阿里云OSS
     *
     * @param content 二维码内容字符串
     * @param width 二维码图片宽度
     * @param height 二维码图片高度
     * @param route 路由信息，用于生成文件路径
     * @param id 标识ID，用于生成文件路径
     * @return 返回上传成功后的文件访问URL
     * @throws WriterException 二维码生成异常
     * @throws IOException 文件操作异常
     */
    public Result<String> generateQRCodeWithUrl(String content, int width, int height, String route, Long id) throws WriterException, IOException{
        byte[] image = qrCodeUtil.generateQRCodeBytes(content, width, height);

        String result = aliOSSUtil.upload(image,
                new QRCodeMap(route, id.toString(), QRCODE_FORMAT));
        return Result.success(result);
    }


}
