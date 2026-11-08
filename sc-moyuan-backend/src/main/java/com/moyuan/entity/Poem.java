package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("poem")
public class Poem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    /** 作者名（外文诗直接存；其它诗词用 poet_id） */
    private String author;
    private Long poetId;
    private Long dynastyId;
    private Long categoryId;
    private String translation;
    private String appreciation;
    private String background;
    private String source;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer status;
    private Integer isFeatured;
    private Integer isOriginal;
    private String poemType;
    private Integer auditStatus;
    private LocalDateTime auditTime;
    private String auditReason;
    /** 收录备注：记录朝代未识别等上游数据问题，便于后台排查 */
    private String importNote;
    private BigDecimal avgScore;
    private Integer ratingCount;
    @TableField(exist = false)
    private String poetName;
    @TableField(exist = false)
    private String dynastyName;
    @TableField(exist = false)
    private String poetAvatar;
    @TableField(exist = false)
    private String categoryName;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
