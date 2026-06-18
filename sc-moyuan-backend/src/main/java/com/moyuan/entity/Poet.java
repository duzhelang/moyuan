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
    @TableField(exist = false)
    private String dynastyName;
    private Integer birthYear;
    private Integer deathYear;
    private String birthplace;
    private String biography;
    private String lifeStory;
    private String influence;
    private String evaluation;
    private String anecdotes;
    private String avatar;
    private String poetType;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
