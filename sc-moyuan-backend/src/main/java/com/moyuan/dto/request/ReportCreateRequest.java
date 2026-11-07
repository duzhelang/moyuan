package com.moyuan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户提交举报。targetUserId 由服务端按内容反查，不接受前端传值。
 */
@Data
public class ReportCreateRequest {

    @NotBlank(message = "举报对象类型不能为空")
    private String targetType;

    @NotNull(message = "举报对象ID不能为空")
    private Long targetId;

    @NotBlank(message = "举报原因不能为空")
    private String reasonType;

    @Size(max = 1000, message = "举报说明不能超过1000字")
    private String reason;

    @Size(max = 2000, message = "证据内容过长")
    private String evidence;
}
