package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推荐模型版本（四组实验模型策略：era/genre/fusion/global）。
 * active_strategy 为数据库生成列（status=1 时取 strategy，否则为 NULL），
 * 由唯一键保证每个策略最多只有一个上线版本，实体中不做映射。
 */
@Data
@TableName("recommendation_model_version")
public class RecommendationModelVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String version;
    private String strategy;
    private String dataFingerprint;
    private String metricsPool;
    private String metricsJson;
    /** 0=下线，1=上线 */
    private Integer status;
    private LocalDateTime createTime;
}
