package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("poem_content_cache")
public class PoemContentCache {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String poemTitle;
    private String poetName;
    private String contentType;
    private String content;
    private String source;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
