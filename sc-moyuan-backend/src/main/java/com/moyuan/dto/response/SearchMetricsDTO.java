package com.moyuan.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索指标汇总：无结果率 / 关键词 TOP / 每日趋势。
 * 数据源为 search_log 表，时间窗由 days 参数控制（默认 7 天）。
 */
@Data
public class SearchMetricsDTO {

    /** 统计时间窗（天） */
    private int windowDays;

    /** 表不可用（未执行迁移）时为 false，前端据此显示「待接入」而不是假 0 */
    private boolean available = true;

    private String message;

    /** 搜索总次数 */
    private long total;

    /** 无结果次数（result_count = 0） */
    private long noResult;

    /** 无结果率，0~1 */
    private double noResultRate;

    /** 去重关键词数 */
    private long distinctKeywords;

    /** 热搜关键词 TOP（按次数） */
    private List<KeywordRow> topKeywords = new ArrayList<>();

    /** 搜不到的关键词 TOP（按次数，仅 result_count = 0） */
    private List<KeywordRow> topNoResult = new ArrayList<>();

    /** 每日趋势，按日期升序 */
    private List<DailyRow> trend = new ArrayList<>();

    @Data
    public static class KeywordRow {
        private String keyword;
        private long count;
        /** 该关键词的命中数中位数，0 表示全部无结果 */
        private long avgResult;
    }

    @Data
    public static class DailyRow {
        private String date;
        private long total;
        private long noResult;
    }
}
