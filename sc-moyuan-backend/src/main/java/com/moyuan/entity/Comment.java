package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String content;
    private Long userId;
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String avatar;
    private Long targetId;
    private Integer targetType;
    private Long parentId;
    private Long replyToUserId;
    private Integer likeCount;
    private Integer status;
    private String auditReason;
    private LocalDateTime auditTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
