package com.moyuan.controller;

import com.moyuan.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Tag(name = "文件接口")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.base-url:http://localhost:8080/uploads}")
    private String baseUrl;

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.error(400, "请选择文件");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String allowedExtensions = ".jpg,.jpeg,.png,.gif,.webp,.bmp";
        if (!allowedExtensions.toLowerCase().contains(extension.toLowerCase())) {
            return R.error(400, "不支持的文件格式");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            return R.error(400, "文件大小不能超过5MB");
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            file.transferTo(new File(dir, fileName));
        } catch (IOException e) {
            return R.error(500, "文件上传失败");
        }

        Map<String, String> result = new HashMap<>();
        result.put("url", baseUrl + "/" + fileName);
        result.put("fileName", fileName);
        return R.success(result);
    }
}
