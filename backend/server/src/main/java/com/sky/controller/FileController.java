package com.sky.controller;

import com.sky.vo.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传控制器
 * <p>
 * 处理图片等文件的上传，将文件保存到本地指定目录，
 * 使用 UUID 重命名避免文件名冲突
 */
@RestController
@RequestMapping("/file")
public class FileController {

    /** 文件上传存储目录，默认 D:/temp，可通过 upload.path 配置 */
    @Value("${upload.path:D:/temp}")
    private String uploadPath;

    /**
     * 上传文件
     * <p>
     * 接收 MultipartFile，生成 UUID 短文件名，保留原始扩展名，
     * 保存到 uploadPath 目录，返回 /uploads/ 下的访问 URL
     *
     * @param file 上传的文件
     * @return 文件的访问 URL
     * @throws IOException 文件写入失败时抛出
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        File dir = new File(uploadPath);
        if (!dir.exists()) dir.mkdirs();

        String ext = file.getOriginalFilename();
        ext = ext != null && ext.contains(".") ? ext.substring(ext.lastIndexOf(".")) : ".png";
        String name = UUID.randomUUID().toString().substring(0, 8) + ext;
        file.transferTo(new File(dir, name));

        return Result.success("/uploads/" + name);
    }
}
