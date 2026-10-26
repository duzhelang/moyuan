package com.moyuan.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 推荐策略切换埋点请求（POST /api/recommend/events）。
 * 前端只上报 page/from/to，userId 由后端从 JWT 解出。
 */
@Data
public class RecommendEventRequest {
    /** 发生页面标识，缺省由服务端按 home 处理 */
    private String page;
    /** 切换前策略 */
    private String from;
    /** 切换后策略（事件表 NOT NULL，必填） */
    @NotBlank(message = "切换后策略不能为空")
    private String to;
}
