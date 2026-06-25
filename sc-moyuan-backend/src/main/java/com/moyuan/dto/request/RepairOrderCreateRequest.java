package com.moyuan.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RepairOrderCreateRequest {
    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "描述不能为空")
    private String description;

    @NotBlank(message = "类别不能为空")
    private String category;

    private Integer priority;

    private String images;
}
