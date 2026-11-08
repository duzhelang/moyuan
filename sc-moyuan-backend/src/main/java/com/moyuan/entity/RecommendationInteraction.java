package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推荐线上曝光/点击埋点。
 * userId 由后端从 JWT 解出；同一批次内同一作品同一事件只记一次。
 */
@Data
@TableName("recommendation_interaction")
public class RecommendationInteraction {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 一次推荐结果的批次号，曝光与点击共用 */
    private String batchId;

    private Long userId;

    /** 页面标识，如 home */
    private String page;

    /** rule / era / genre / fusion / global */
    private String strategy;

    private Long itemId;

    /** impression / click */
    private String eventType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
