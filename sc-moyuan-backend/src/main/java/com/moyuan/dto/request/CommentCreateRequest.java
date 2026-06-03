package com.moyuan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentCreateRequest {
    @NotBlank(message = "评论内容不能为空")
    private String content;

    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    @NotNull(message = "目标类型不能为空")
    private Integer targetType;

    private Long parentId;
    private Long replyToUserId;
}
