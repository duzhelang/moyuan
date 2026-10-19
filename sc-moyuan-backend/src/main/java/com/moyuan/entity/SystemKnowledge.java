package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统知识库实体（供 AI 小助手查阅系统用法）
 */
@Data
@TableName("system_knowledge")
public class SystemKnowledge {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 条目标识，如 play.feihualing */
    private String code;
    /** 标题 */
    private String title;
    /** 分类：play-玩法 / page-页面 / feature-功能 / account-账号 */
    private String category;
    /** 关键词，英文逗号分隔 */
    private String keywords;
    /** 同义词/别名，英文逗号分隔 */
    private String aliases;
    /** 正文说明（简洁，不超过 300 字） */
    private String body;
    /** 步骤，每行一条，换行符分隔 */
    private String steps;
    /** FAQ 的 JSON 数组文本，元素形如 {"q":"问题","a":"答案","aliases":["别名1"]} */
    private String faq;
    /** 关联前端路由，如 /game/feihua */
    private String route;
    /** 是否启用：0-停用，1-启用 */
    private Integer isShow;
    /** 排序 */
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
