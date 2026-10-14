package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.entity.FileMetadata;
import com.moyuan.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 后台文件管理接口
 */
@Slf4j
@Tag(name = "后台文件管理接口")
@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
public class AdminFileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "分页查询文件列表")
    @GetMapping
    public R<?> listFiles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        try {
            return R.success(fileStorageService.listFiles(page, size, fileType, keyword, status));
        } catch (Exception e) {
            log.error("查询文件列表失败", e);
            return R.error("查询文件列表失败");
        }
    }

    @Operation(summary = "获取存储统计信息")
    @GetMapping("/stats")
    public R<Map<String, Object>> getStorageStats() {
        try {
            return R.success(fileStorageService.getStorageStats());
        } catch (Exception e) {
            log.error("获取存储统计失败", e);
            return R.error("获取存储统计失败");
        }
    }

    @Operation(summary = "清理孤儿文件")
    @PostMapping("/clean-orphans")
    public R<Map<String, Object>> cleanOrphanFiles() {
        try {
            return R.success(fileStorageService.cleanOrphanFiles());
        } catch (Exception e) {
            log.error("清理孤儿文件失败", e);
            return R.error("清理孤儿文件失败");
        }
    }

    @Operation(summary = "删除单个文件")
    @DeleteMapping("/{id}")
    public R<Void> deleteFile(@PathVariable Long id) {
        try {
            FileMetadata metadata = fileStorageService.getById(id);
            if (metadata == null) {
                return R.error("文件记录不存在");
            }
            fileStorageService.delete(metadata.getFileKey());
            return R.success();
        } catch (Exception e) {
            log.error("删除文件失败：id={}", id, e);
            return R.error("删除文件失败");
        }
    }
}
