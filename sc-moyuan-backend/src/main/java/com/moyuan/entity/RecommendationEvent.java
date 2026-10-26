package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推荐策略切换事件（可选埋点）。
 * userId 由后端从 JWT 解出，不取前端传值；上报失败静默。
 */
@Data
@TableName("recommendation_event")
public class RecommendationEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String page;
    private String fromStrategy;
    private String toStrategy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
