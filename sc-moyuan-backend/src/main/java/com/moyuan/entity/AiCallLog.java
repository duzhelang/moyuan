package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 调用日志：由 AiCallLogService 异步写入。
 * 业务异常返回 HTTP 200，因此失败必须显式标记，不能只看状态码。
 */
@Data
@TableName("ai_call_log")
public class AiCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** chat / chat_stream / analyze / write_poem / ocr / couplet */
    private String endpoint;

    private String model;

    private String moduleCode;

    /** success / error */
    private String status;

    private String errorMsg;

    private Integer durationMs;

    /** 首字延迟，仅流式接口有值 */
    private Integer ttftMs;

    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
