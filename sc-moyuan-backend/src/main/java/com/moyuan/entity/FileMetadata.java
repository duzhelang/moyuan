package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件元数据实体类
 */
@Data
@TableName("file_metadata")
public class FileMetadata {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 存储路径（相对uploads） */
    private String fileKey;

    /** 原始文件名 */
    private String originalName;

    /** 文件类型：avatar/poem/forum/ai_generated/vision/config/export/temp/backup/audit/watermark/cache */
    private String fileType;

    /** MIME类型 */
    private String mimeType;

    /** 文件大小（字节） */
    private Long size;

    /** 文件MD5 */
    private String md5;

    /** 图片宽度 */
    private Integer width;

    /** 图片高度 */
    private Integer height;

    /** 关联的用户ID/诗词ID/帖子ID */
    private Long relatedId;

    /** 关联类型：user/poem/forum/vision/article */
    private String relatedType;

    /** 存储类型：local/oss */
    private String storageType;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
