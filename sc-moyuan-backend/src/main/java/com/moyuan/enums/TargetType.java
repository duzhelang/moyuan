package com.moyuan.enums;

import lombok.Getter;

@Getter
public enum TargetType {
    POEM(1, "诗词"),
    POST(2, "帖子"),
    COMMENT(3, "评论");

    private final int code;
    private final String description;

    TargetType(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
