package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户举报（用户对用户/内容违规）。
 * reporterId = 0 表示系统/AI 自动风控生成，区别于系统 Bug 报修（repair_order）。
 */
@Data
@TableName("user_report")
public class UserReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reporterId;
    /** post / comment / poem / user */
    private String targetType;
    private Long targetId;
    private Long targetUserId;
    /** spam / abuse / porn / plagiarism / illegal / ai_auto / other */
    private String reasonType;
    private String reason;
    private String evidence;
    /** pending / processing / accepted / rejected */
    private String status;
    private String handleNote;
    private Long handlerId;
    private LocalDateTime handledAt;
    /** 0-用户提交，1-AI 自动风控 */
    private Integer aiFlag;
    /** low / medium / high */
    private String aiRiskLevel;
    private String aiReason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
