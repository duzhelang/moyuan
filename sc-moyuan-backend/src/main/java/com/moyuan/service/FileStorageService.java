package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.FileMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

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

    /**
     * 管理端分页查询文件列表
     *
     * @param page     页码
     * @param size     每页大小
     * @param fileType 文件类型筛选（可选）
     * @param keyword  关键字（匹配原始文件名或fileKey，可选）
     * @param status   状态筛选（可选）
     * @return 分页结果
     */
    IPage<FileMetadata> listFiles(int page, int size, String fileType, String keyword, Integer status);

    /**
     * 获取存储统计信息：总文件数、总占用字节、按类型分组统计，
     * 以及“数据库有记录但物理文件缺失”的异常数量（只统计不删除）
     *
     * @return 统计信息
     */
    Map<String, Object> getStorageStats();

    /**
     * 清理孤儿文件：扫描物理上传目录，删除磁盘存在但数据库无记录的文件
     *
     * @return 删除数量与释放字节
     */
    Map<String, Object> cleanOrphanFiles();
}
