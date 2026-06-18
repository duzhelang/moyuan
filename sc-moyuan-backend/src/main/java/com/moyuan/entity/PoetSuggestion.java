package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("poet_suggestion")
public class PoetSuggestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long poetId;

    private Long userId;

    private String section;

    private String content;

    private String category;

    private String status;

    private String ip;

    private String reviewComment;

    private Long reviewerId;

    private LocalDateTime reviewTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
