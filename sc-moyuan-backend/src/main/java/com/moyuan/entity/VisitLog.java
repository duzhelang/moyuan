package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("visit_log")
public class VisitLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String ip;
    private String userAgent;
    private String path;
    private LocalDate visitDate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
