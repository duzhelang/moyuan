package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("poet_draft")
public class PoetDraft {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long poetId;

    private String section;

    private String content;

    private String originalContent;

    private Long editorId;

    private Integer status;

    private Long reviewerId;

    private String reviewComment;

    private LocalDateTime reviewTime;

    private String ip;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}