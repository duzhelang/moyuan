package com.moyuan.common;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS("200", "操作成功"),
    BAD_REQUEST("400", "请求参数错误"),
    UNAUTHORIZED("401", "未登录或Token已过期"),
    FORBIDDEN("403", "没有操作权限"),
    NOT_FOUND("404", "资源不存在"),
    CONFLICT("409", "数据冲突"),
    ERROR("500", "操作失败"),
    INTERNAL_ERROR("500", "服务器内部错误"),
    PARAM_ERROR("400", "参数错误"),
    USER_NOT_FOUND("1001", "用户不存在"),
    USERNAME_EXISTS("1002", "用户名已存在"),
    EMAIL_EXISTS("1003", "邮箱已存在"),
    PASSWORD_ERROR("1004", "密码错误"),
    ACCOUNT_DISABLED("1005", "账号已被禁用"),
    POST_NOT_FOUND("2001", "帖子不存在"),
    ARTICLE_NOT_FOUND("2101", "文章不存在"),
    POEM_NOT_FOUND("3001", "诗词不存在"),
    POET_NOT_FOUND("3002", "诗人不存在"),
    DATA_HAS_CHILDREN("4001", "该分类下存在子分类"),
    ALREADY_LIKED("5001", "已点赞"),
    NOT_LIKED("5002", "未点赞"),
    ALREADY_FAVORITED("5003", "已收藏"),
    NOT_FAVORITED("5004", "未收藏");

    private final String code;
    private final String message;

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
