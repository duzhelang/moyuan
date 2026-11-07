package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_ticket")
public class AuditTicket {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** poem / comment / ai_content / poet_profile / poet_draft / poet_suggestion */
    private String bizType;

    private Long bizId;

    /** 冗余标题，看板直接展示，避免联表 */
    private String title;

    private String authorName;

    /** 作者 id，供 author 规则匹配（评论类为空） */
    private String authorRef;

    private String dynastyName;

    /** poem_ancient / poem_modern / comment / ai_content / ... */
    private String contentType;

    /** 0 普通 1 高 2 紧急 */
    private Integer priority;

    /** pending / assigned / reviewing / approved / rejected / void */
    private String status;

    private Long reviewerId;

    private String result;

    /** 命中哪条规则分的单，便于追溯 */
    private String assignNote;

    private LocalDateTime assignedAt;

    private LocalDateTime claimedAt;

    private LocalDateTime finishedAt;

    /** 按优先级算的 SLA 截止时间 */
    private LocalDateTime deadlineAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
