package com.moyuan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.FileMetadata;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 */
public interface FileStorageService extends IService<FileMetadata> {

    /**
     * 上传文件
     *
     * @param file        文件
     * @param fileType    文件类型（avatar/poem/forum/vision/ai_generated等）
     * @param relatedId   关联ID（用户ID/诗词ID/帖子ID等）
     * @param relatedType 关联类型（user/poem/forum/vision/article等）
     * @return 文件元数据
     */
    FileMetadata upload(MultipartFile file, String fileType, Long relatedId, String relatedType);

    /**
     * 删除文件
     *
     * @param fileKey 文件存储路径
     */
    void delete(String fileKey);

    /**
     * 获取文件访问URL
     *
     * @param fileKey 文件存储路径
     * @return 完整访问URL
     */
    String getUrl(String fileKey);

    /**
     * 生成缩略图
     *
     * @param fileKey 文件存储路径
     * @param width   目标宽度
     * @param height  目标高度
     * @return 缩略图文件元数据
     */
    FileMetadata generateThumb(String fileKey, int width, int height);

    /**
     * 获取文件信息
     *
     * @param fileKey 文件存储路径
     * @return 文件元数据
     */
    FileMetadata getFileInfo(String fileKey);
}
