package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推荐结果缓存（离线训练导出的 user × 策略 × Top20 查表结果）。
 * user_id / poem_id 使用主库 id 空间，rank_no 取值 1-20。
 */
@Data
@TableName("recommendation_cache")
public class RecommendationCache {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String strategy;
    private Long poemId;
    private Integer rankNo;
    private Double score;
    private String modelVersion;
    private LocalDateTime createTime;
}
