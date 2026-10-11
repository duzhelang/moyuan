package com.moyuan.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 今日诗词（jinrishici）响应解析工具
 *
 * <p>将外部返回体转换为收录所需的诗词字段，使外部响应适配逻辑不散落在 Controller 层。</p>
 */
public final class JinrishiciParser {

    private JinrishiciParser() {
    }

    /**
     * 解析今日诗词返回体，提取 title / content / author / dynasty。
     *
     * @param body 今日诗词接口返回的原始对象
     * @return 归一化后的诗词字段 Map；无法解析或标题为空时返回 {@code null}
     */
    public static Map<String, Object> toPoemMap(Object body) {
        if (!(body instanceof Map)) {
            return null;
        }
        Object dataObj = ((Map<?, ?>) body).get("data");
        if (!(dataObj instanceof Map)) {
            return null;
        }
        Object originObj = ((Map<?, ?>) dataObj).get("origin");
        if (!(originObj instanceof Map)) {
            return null;
        }
        Map<?, ?> origin = (Map<?, ?>) originObj;
        Object titleObj = origin.get("title");
        if (titleObj == null || String.valueOf(titleObj).trim().isEmpty()) {
            return null;
        }

        String content = "";
        Object contentObj = origin.get("content");
        if (contentObj instanceof List) {
            content = ((List<?>) contentObj).stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .collect(Collectors.joining("\n"));
        }

        Map<String, Object> item = new HashMap<>();
        item.put("title", String.valueOf(titleObj));
        item.put("content", content);
        item.put("author", origin.get("author") == null ? "" : String.valueOf(origin.get("author")));
        item.put("dynasty", origin.get("dynasty") == null ? "" : String.valueOf(origin.get("dynasty")));
        return item;
    }
}
