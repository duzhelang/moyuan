package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 搜索埋点：由 SearchLogService 异步写入，
 * 用于管理端「搜索无结果率」与用户分析的「搜不到的关键词」。
 */
@Data
@TableName("search_log")
public class SearchLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 搜索关键词（trim 后，超长截断） */
    private String keyword;

    /** 命中结果数，0 表示搜不到 */
    private Integer resultCount;

    /** 命中路径：exact / fuzzy / pinyin / pingze / latin / global */
    private String route;

    /** 来源：smart 智能搜索 / global 全局搜索 */
    private String source;

    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
