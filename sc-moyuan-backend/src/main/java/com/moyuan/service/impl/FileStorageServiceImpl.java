package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.entity.FileMetadata;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.FileMetadataMapper;
import com.moyuan.service.FileStorageService;
import com.moyuan.util.WatermarkUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 文件存储服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl extends ServiceImpl<FileMetadataMapper, FileMetadata> implements FileStorageService {

    private final FileMetadataMapper fileMetadataMapper;
    private final WatermarkUtil watermarkUtil;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.base-url:/uploads}")
    private String baseUrl;

    @Value("${file.allowed-extensions:.jpg,.jpeg,.png,.gif,.webp,.bmp}")
    private String allowedExtensions;

    @Value("${file.max-size:10485760}")
    private long maxSize;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileMetadata upload(MultipartFile file, String fileType, Long relatedId, String relatedType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("400", "上传文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);

        // 校验扩展名
        if (!isAllowedExtension(extension)) {
            throw new BusinessException("400", "不支持的文件格式：" + extension);
        }

        // 校验文件大小
        if (file.getSize() > maxSize) {
            throw new BusinessException("400", "文件大小不能超过" + (maxSize / 1024 / 1024) + "MB");
        }

        // 生成唯一文件名
        String uniqueName = UUID.randomUUID().toString().replace("-", "") + extension;

        // 根据文件类型确定存储目录
        String subDir = buildSubDirectory(fileType, relatedId);
        String relativePath = subDir + uniqueName;

        // 读取文件字节数组（一次性读取，避免流被重复消费）
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取文件字节失败：{}", originalName, e);
            throw new BusinessException("500", "文件上传失败");
        }

        // 计算文件MD5
        String md5 = calculateMd5(fileBytes);

        // 检查MD5是否已存在（秒传支持）
        FileMetadata existing = fileMetadataMapper.selectOne(
                new LambdaQueryWrapper<FileMetadata>().eq(FileMetadata::getMd5, md5));
        if (existing != null) {
            File existingFile = new File(uploadDir + File.separator + existing.getFileKey());
            if (existingFile.exists()) {
                log.info("文件MD5已存在且物理文件存在，复用文件记录：{}", existing.getFileKey());
                return existing;
            }
            log.warn("文件MD5已存在但物理文件不存在，继续上传：{}", existing.getFileKey());
        }

        // 写入磁盘
        String absolutePath = uploadDir + File.separator + relativePath;
        File destFile = new File(absolutePath);
        try {
            File parentDir = destFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            java.nio.file.Files.write(destFile.toPath(), fileBytes);
        } catch (IOException e) {
            log.error("文件写入失败：{}", absolutePath, e);
            throw new BusinessException("500", "文件上传失败");
        }

        // 对AI生成的图片自动添加水印
        if ("ai_generated".equals(fileType) && isImageMimeType(file.getContentType())) {
            applyWatermark(destFile, file.getContentType());
        }

        // 读取图片宽高（仅限图片类型）
        Integer width = null;
        Integer height = null;
        if (isImageMimeType(file.getContentType())) {
            try (InputStream is = new java.io.FileInputStream(destFile)) {
                BufferedImage image = ImageIO.read(is);
                if (image != null) {
                    width = image.getWidth();
                    height = image.getHeight();
                }
            } catch (IOException e) {
                log.warn("读取图片尺寸失败：{}", originalName, e);
            }
        }

        // 保存文件元数据
        long fileSize = destFile.exists() ? destFile.length() : file.getSize();
        FileMetadata metadata = new FileMetadata();
        metadata.setFileKey(relativePath);
        metadata.setOriginalName(originalName);
        metadata.setFileType(fileType);
        metadata.setMimeType(file.getContentType());
        metadata.setSize(fileSize);
        metadata.setMd5(md5);
        metadata.setWidth(width);
        metadata.setHeight(height);
        metadata.setRelatedId(relatedId);
        metadata.setRelatedType(relatedType);
        metadata.setStorageType("local");
        metadata.setStatus(1);
        fileMetadataMapper.insert(metadata);

        log.info("文件上传成功：fileKey={}, size={}", relativePath, fileSize);
        return metadata;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String fileKey) {
        FileMetadata metadata = fileMetadataMapper.selectOne(
                new LambdaQueryWrapper<FileMetadata>().eq(FileMetadata::getFileKey, fileKey));
        if (metadata == null) {
            log.warn("文件记录不存在：{}", fileKey);
            return;
        }

        // 删除物理文件
        String absolutePath = uploadDir + File.separator + fileKey;
        File file = new File(absolutePath);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                log.info("物理文件已删除：{}", absolutePath);
            } else {
                log.warn("物理文件删除失败：{}", absolutePath);
            }
        }

        // 删除数据库记录
        fileMetadataMapper.delete(
                new LambdaQueryWrapper<FileMetadata>().eq(FileMetadata::getFileKey, fileKey));

        log.info("文件记录已删除：{}", fileKey);
    }

    @Override
    public String getUrl(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return null;
        }
        return baseUrl + "/" + fileKey;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileMetadata generateThumb(String fileKey, int width, int height) {
        FileMetadata source = fileMetadataMapper.selectOne(
                new LambdaQueryWrapper<FileMetadata>().eq(FileMetadata::getFileKey, fileKey));
        if (source == null) {
            throw new BusinessException("404", "文件记录不存在");
        }

        if (!isImageMimeType(source.getMimeType())) {
            throw new BusinessException("400", "仅支持图片类型生成缩略图");
        }

        // 生成缩略图路径
        String thumbKey = fileKey.replace(".", "_thumb_" + width + "x" + height + ".");
        String sourcePath = uploadDir + File.separator + fileKey;
        String thumbPath = uploadDir + File.separator + thumbKey;

        // 使用ImageIO生成缩略图
        try {
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) {
                throw new BusinessException("404", "源文件不存在");
            }

            BufferedImage sourceImage = ImageIO.read(sourceFile);
            if (sourceImage == null) {
                throw new BusinessException("500", "无法读取图片");
            }

            // 等比缩放
            int originalWidth = sourceImage.getWidth();
            int originalHeight = sourceImage.getHeight();
            double ratio = Math.min((double) width / originalWidth, (double) height / originalHeight);
            int targetWidth = (int) (originalWidth * ratio);
            int targetHeight = (int) (originalHeight * ratio);

            BufferedImage thumbImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            thumbImage.getGraphics().drawImage(
                    sourceImage.getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_SMOOTH),
                    0, 0, null);

            // 获取输出格式
            String formatName = extractFormatName(source.getMimeType());

            File thumbFile = new File(thumbPath);
            File parentDir = thumbFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            ImageIO.write(thumbImage, formatName, thumbFile);

            // 保存缩略图元数据
            FileMetadata thumbMetadata = new FileMetadata();
            thumbMetadata.setFileKey(thumbKey);
            thumbMetadata.setOriginalName(source.getOriginalName());
            thumbMetadata.setFileType(source.getFileType());
            thumbMetadata.setMimeType(source.getMimeType());
            thumbMetadata.setSize(thumbFile.length());
            thumbMetadata.setMd5(calculateMd5(thumbFile));
            thumbMetadata.setWidth(targetWidth);
            thumbMetadata.setHeight(targetHeight);
            thumbMetadata.setRelatedId(source.getRelatedId());
            thumbMetadata.setRelatedType(source.getRelatedType());
            thumbMetadata.setStorageType("local");
            thumbMetadata.setStatus(1);
            fileMetadataMapper.insert(thumbMetadata);

            log.info("缩略图生成成功：thumbKey={}, {}x{}", thumbKey, targetWidth, targetHeight);
            return thumbMetadata;
        } catch (IOException e) {
            log.error("缩略图生成失败：{}", fileKey, e);
            throw new BusinessException("500", "缩略图生成失败");
        }
    }

    @Override
    public FileMetadata getFileInfo(String fileKey) {
        return fileMetadataMapper.selectOne(
                new LambdaQueryWrapper<FileMetadata>().eq(FileMetadata::getFileKey, fileKey));
    }

    /**
     * 对图片文件添加AI生成水印并覆盖原文件
     *
     * @param imageFile 图片文件
     * @param mimeType  图片MIME类型
     */
    private void applyWatermark(File imageFile, String mimeType) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                log.warn("无法读取图片，跳过水印添加：{}", imageFile.getName());
                return;
            }
            BufferedImage watermarked = watermarkUtil.addAiWatermark(image);
            String formatName = extractFormatName(mimeType);
            ImageIO.write(watermarked, formatName, imageFile);
            log.info("AI生成图片水印添加成功：{}", imageFile.getName());
        } catch (IOException e) {
            log.error("AI生成图片水印添加失败：{}", imageFile.getName(), e);
        }
    }

    /**
     * 根据文件类型和关联ID构建子目录
     */
    private String buildSubDirectory(String fileType, Long relatedId) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        return switch (fileType) {
            case "avatar" -> "avatars/" + relatedId + "/";
            case "poem" -> "poems/" + relatedId + "/";
            case "user_poem" -> "user_poems/" + relatedId + "/";
            case "forum" -> "forum/" + dateStr + "/";
            case "vision" -> "vision/article_" + relatedId + "/";
            case "ai_generated" -> "ai_generated/" + LocalDate.now() + "/";
            case "config" -> "config/";
            case "export" -> "export/";
            case "temp" -> "temp/";
            case "backup" -> "backup/";
            case "audit" -> "audit/";
            case "watermark" -> "watermark/";
            case "cache" -> "cache/";
            default -> fileType + "/";
        };
    }

    /**
     * 提取文件扩展名（含点号）
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 判断是否为图片MIME类型
     */
    private boolean isImageMimeType(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * 根据MIME类型获取ImageIO格式名
     */
    private String extractFormatName(String mimeType) {
        if (mimeType == null) {
            return "jpg";
        }
        return switch (mimeType) {
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    /**
     * 校验文件扩展名是否在允许列表中
     */
    private boolean isAllowedExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        String lowerExtension = extension.toLowerCase();
        for (String allowed : allowedExtensions.split(",")) {
            if (allowed.trim().toLowerCase().equals(lowerExtension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算文件MD5（从字节数组）
     */
    private String calculateMd5(byte[] fileBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(md.digest(fileBytes));
        } catch (NoSuchAlgorithmException e) {
            log.warn("计算MD5失败", e);
            return null;
        }
    }

    /**
     * 计算物理文件MD5
     */
    private String calculateMd5(File file) {
        try (var fis = new java.io.FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            log.warn("计算MD5失败：{}", file.getPath(), e);
            return null;
        }
    }
}
