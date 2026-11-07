package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_reviewer")
public class AuditReviewer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 工位名，如「诗部·唐」 */
    private String displayName;

    /** 擅长类型，逗号分隔：poem_ancient,poem_modern,comment,ai_content,... */
    private String specialties;

    /** 同时在手工单上限，超过后分派引擎不再派新单 */
    private Integer maxConcurrent;

    /** 1 在岗 / 0 停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
