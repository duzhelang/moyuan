package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("poem_rating")
public class PoemRating {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long poemId;
    private Long userId;
    private BigDecimal score;
    private Integer ratingType;
    private String dimension;
    private String comment;
    private String aiModel;
    private String aiSummary;
    private String aiAnalysis;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}