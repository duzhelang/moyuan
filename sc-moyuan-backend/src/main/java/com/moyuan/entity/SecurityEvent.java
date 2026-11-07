package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 安全事件：登录失败 / 越权 403 / 限流触发。
 * 401、403 由 Spring Security 过滤器直接写出响应，不经过 MVC，
 * 所以只能在 EntryPoint / AccessDeniedHandler 里显式记录。
 */
@Data
@TableName("security_event")
public class SecurityEvent {

    public static final String TYPE_LOGIN_FAIL = "login_fail";
    public static final String TYPE_FORBIDDEN = "forbidden";
    public static final String TYPE_RATE_LIMIT = "rate_limit";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String type;

    private String detail;

    private String ip;

    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
