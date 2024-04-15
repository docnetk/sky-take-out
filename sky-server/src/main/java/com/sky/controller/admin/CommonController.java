package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.QiniuUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
public class CommonController {
    @Autowired
    private QiniuUtil qiniuUtil;

    @PostMapping("/admin/common/upload")
    public Result<String> uploadImage(MultipartFile file) {
        // 获取文件后缀
        String originFileName = file.getOriginalFilename();

        assert originFileName != null;
        String filePost = originFileName.substring(originFileName.lastIndexOf("."));

//        System.out.println("fileName: " + filePost);
        // 生成新的文件名
        String fileName = UUID.randomUUID() + filePost;
        String url = qiniuUtil.upload(fileName.getBytes(), fileName);

        return Result.success(url);
    }
}
