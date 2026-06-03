package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.entity.FileMetadata;
import com.moyuan.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "文件接口")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public R<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileType", defaultValue = "common") String fileType,
            @RequestParam(value = "relatedId", required = false) Long relatedId,
            @RequestParam(value = "relatedType", required = false) String relatedType) {
        FileMetadata metadata = fileStorageService.upload(file, fileType, relatedId, relatedType);
        Map<String, String> result = new HashMap<>();
        result.put("url", fileStorageService.getUrl(metadata.getFileKey()));
        result.put("fileName", metadata.getOriginalName());
        result.put("fileKey", metadata.getFileKey());
        return R.success(result);
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/{fileKey}")
    public R<Void> delete(@PathVariable String fileKey) {
        fileStorageService.delete(fileKey);
        return R.success(null);
    }

    @Operation(summary = "获取文件信息")
    @GetMapping("/{fileKey}")
    public R<FileMetadata> getFileInfo(@PathVariable String fileKey) {
        FileMetadata metadata = fileStorageService.getFileInfo(fileKey);
        return R.success(metadata);
    }
}
