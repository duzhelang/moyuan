package com.moyuan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 推荐线上效果埋点：一次请求上报一批作品的曝光或点击。
 */
@Data
public class RecommendInteractionRequest {

    /** 页面标识，缺省 home */
    private String page;
    /** 推荐策略：rule / era / genre / fusion / global */
    private String strategy;
    /** 批次号，曝光与点击共用同一值 */
    @NotBlank(message = "batchId 不能为空")
    private String batchId;
    /** impression / click */
    @NotBlank(message = "eventType 不能为空")
    private String eventType;
    /** 本次涉及的作品 ID，最多 20 个 */
    @NotEmpty(message = "itemIds 不能为空")
    private List<Long> itemIds;
}
