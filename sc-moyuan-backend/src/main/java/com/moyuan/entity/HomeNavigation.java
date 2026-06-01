package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("home_navigation")
public class HomeNavigation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private String title;
    private String subtitle;
    private String imageUrl;
    private Long linkId;
    private String linkType;
    private Integer sortOrder;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
