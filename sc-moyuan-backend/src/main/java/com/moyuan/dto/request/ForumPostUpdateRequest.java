package com.moyuan.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForumPostUpdateRequest {
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    private String content;
    private String category;
}
