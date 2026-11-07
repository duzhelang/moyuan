package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口调用指标：由 ApiMetricInterceptor 异步写入，
 * 用于管理端「今日体检」错误率 / P95 与监测页「可用性」。
 */
@Data
@TableName("api_request_metric")
public class ApiRequestMetric {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String method;

    /** 接口路径（不含查询串） */
    private String path;

    private Integer statusCode;

    private Integer durationMs;

    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
