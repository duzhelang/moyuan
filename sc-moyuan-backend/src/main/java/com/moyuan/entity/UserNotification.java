package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知：审核结果、举报处理结果与系统通知。
 */
@Data
@TableName("user_notification")
public class UserNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** audit_result / report_result / system */
    private String type;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private String link;
    private Integer isRead;
    private LocalDateTime readAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
