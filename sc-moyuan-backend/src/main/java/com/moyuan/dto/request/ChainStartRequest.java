package com.moyuan.dto.request;

import lombok.Data;

/**
 * 诗词接龙开局请求（单一玩法，仅需输入方式与范围）。
 */
@Data
public class ChainStartRequest {

    /**
     * 选择范围：朝代 ID。
     * 为空表示「中国古典诗词（不限朝代）」，非空则起句、AI 接句与玩家校验均限定在该朝代。
     */
    private Long dynastyId;

    /** 输入方式：text 文本 / voice 语音 */
    private String inputType = "text";
}
