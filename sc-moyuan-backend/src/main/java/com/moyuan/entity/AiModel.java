package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_model")
public class AiModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String displayName;

    private String provider;

    private String modelType;

    private String apiUrl;

    private String apiKey;

    private String modelId;

    private String visionModelId;

    private Integer maxTokens;

    private String extraConfig;

    private Integer isEnabled;

    private Integer isDefault;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}