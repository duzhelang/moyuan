package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("poem")
public class Poem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private Long poetId;

    private Long dynastyId;

    private Long categoryId;

    private String translation;

    private String appreciation;

    private String background;

    private String source;

    private Integer viewCount;

    private Integer likeCount;

    private Integer favoriteCount;

    private Integer status;

    private Integer isFeatured;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
