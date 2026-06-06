package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("poet_profile")
public class PoetProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String penName;
    private String realName;
    private String specialty;
    private String introduction;
    private String literaryConcept;
    private String achievements;
    private String contactInfo;
    private Integer verifiedStatus;
    private LocalDateTime verifiedTime;
    private String verifiedReason;
    private Integer workCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer followerCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}