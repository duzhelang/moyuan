package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_rule")
public class AuditRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** author / dynasty / content_type / global */
    private String scopeType;

    /** 作者 id、朝代名或内容类型；global 规则为空 */
    private String scopeValue;

    private Long reviewerId;

    /** 越小越先匹配：author 10 / dynasty 20 / content_type 30 / global 90 */
    private Integer priority;

    private Integer enabled;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
