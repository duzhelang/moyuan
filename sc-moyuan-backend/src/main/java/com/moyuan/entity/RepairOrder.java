package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("repair_order")
public class RepairOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String title;
    private String description;
    private String category;
    private Integer priority;
    private Integer status;
    private String images;
    private Long userId;
    private Long assigneeId;
    private String resolveContent;
    private LocalDateTime resolveTime;
    private LocalDateTime closeTime;
    private Integer satisfaction;
    private String satisfactionComment;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String avatar;
    @TableField(exist = false)
    private String assigneeName;
}
