package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_module_config")
public class AiModuleConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String moduleCode;

    private String moduleName;

    private Long modelId;

    private Integer requireVision;

    private String description;

    private String promptTemplate;

    @TableField("max_response_length")
    private Integer maxLength;

    private String responseStyle;

    @TableField("first_response_length")
    private Integer firstResponseLength;

    @TableField("enable_markdown")
    private Integer enableMarkdown;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
