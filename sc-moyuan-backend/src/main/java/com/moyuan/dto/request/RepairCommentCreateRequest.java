package com.moyuan.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RepairCommentCreateRequest {
    @NotBlank(message = "评论内容不能为空")
    private String content;

    private String images;

    private Integer isInternal;
}
