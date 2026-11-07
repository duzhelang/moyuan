package com.moyuan.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全事件指标：登录失败 / 越权 403 / 限流触发 / 高危操作。
 * 数据源为 security_event（前三类）与 operation_log（高危操作，复用现有表）。
 */
@Data
public class SecurityMetricsDTO {

    private int windowHours;

    private boolean available = true;

    private String message;

    /** 登录失败次数 */
    private long loginFail;

    /** 越权访问（403）次数 */
    private long forbidden;

    /** 限流触发次数 */
    private long rateLimit;

    /** 事件合计 */
    private long total;

    /** 涉及的不同 IP 数（仅登录失败，用于判断是否被撞库） */
    private long distinctIp;

    /** 最近 24 小时的高危操作数（来自 operation_log，读不到时为 null） */
    private Long highRiskOps;

    /** 按类型分组的明细，便于直接渲染 */
    private List<TypeRow> byType = new ArrayList<>();

    /** 登录失败 TOP IP */
    private List<IpRow> topIps = new ArrayList<>();

    @Data
    public static class TypeRow {
        private String type;
        private long count;
    }

    @Data
    public static class IpRow {
        private String ip;
        private long count;
    }
}
