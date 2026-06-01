package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("poet_featured")
public class PoetFeatured {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long poetId;
    private String name;
    private String dynasty;
    private String description;
    private String biography;
    private String imageUrl;
    private Integer sortOrder;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}