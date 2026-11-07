package com.moyuan.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统健康检查响应：管理端链路条 / 监测页使用。
 * 每个节点独立探测，任一节点失败不影响其他节点返回。
 */
@Data
public class SystemHealthDTO {

    /** 检查时间（ISO-8601） */
    private String checkedAt;

    private List<Node> nodes = new ArrayList<>();

    @Data
    public static class Node {
        /** 节点标识：api / mysql / redis / rag */
        private String key;
        private String name;
        /** ok / warn / down */
        private String status;
        /** 探测耗时（毫秒） */
        private Long latencyMs;
        /** 失败原因或补充说明，成功时为空 */
        private String message;
        private List<Metric> metrics = new ArrayList<>();

        public static Node of(String key, String name) {
            Node node = new Node();
            node.setKey(key);
            node.setName(name);
            node.setStatus("ok");
            return node;
        }

        public Node metric(String label, String value) {
            Metric m = new Metric();
            m.setLabel(label);
            m.setValue(value);
            this.metrics.add(m);
            return this;
        }
    }

    @Data
    public static class Metric {
        private String label;
        private String value;
    }
}
