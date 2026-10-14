package com.moyuan.dto.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流式对话请求（{@code POST /api/ai/chat/stream}）。
 * <p>{@code messages} 为完整的 system + 历史 + user 数组，元素形如 {@code {"role":"user","content":"..."}}。
 */
@Data
public class ChatStreamRequest {

    /** 模块编码，前端必传 'chat'（已绑定 DeepSeek）；为空则回退默认模型 */
    private String moduleCode;

    /** 完整消息数组（含 system 角色），不可为空 */
    private List<Map<String, Object>> messages;

    /** 采样温度，可为空，为空时使用模型默认值 */
    private Double temperature;

    /** 最大生成 token 数，可为空，为空时使用模型配置值 */
    private Integer maxTokens;
}
