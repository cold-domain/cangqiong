package com.sky.controller.admin;

import com.sky.properties.AliOssProperties;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 通用接口
 */
@RestController
@Slf4j
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file) {
        //springmvc框架会自动将上传的文件绑定到这个参数上，且跟前端的传递参数名一致
        log.info("开始上传文件:{}",file);

        try {
            //原始文件名
            String originalFilename = file.getOriginalFilename();

            //截取原始文件名的后缀    .png/.jpg
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

            //生成新的文件拓展名
            String ObjectName = UUID.randomUUID().toString() + extension;

            //文件请求路径
            String filePath = aliOssUtil.upload(file.getBytes(),ObjectName);
            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败: {}",e);
        }

        return null;
    }
}
