package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rhyme")
public class Rhyme {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String character;

    private String rhymeGroup;

    private String toneType;

    private String rhymeCategory;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
