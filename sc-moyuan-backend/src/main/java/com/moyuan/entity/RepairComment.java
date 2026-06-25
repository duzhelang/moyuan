package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("repair_comment")
public class RepairComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long repairOrderId;
    private Long userId;
    private String content;
    private String images;
    private Integer isInternal;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String avatar;
}
