package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("poet")
public class Poet {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String courtesyName;
    private String pseudonym;
    private Long dynastyId;
    private Integer birthYear;
    private Integer deathYear;
    private String birthplace;
    private String biography;
    @TableField(exist = false)
    private String lifeStory;
    @TableField(exist = false)
    private String influence;
    @TableField(exist = false)
    private String evaluation;
    @TableField(exist = false)
    private String anecdotes;
    private String avatar;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
