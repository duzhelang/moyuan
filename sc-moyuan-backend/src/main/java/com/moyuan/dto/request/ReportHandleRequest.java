package com.moyuan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员/审核员处理举报。
 */
@Data
public class ReportHandleRequest {

    /** processing / accepted / rejected */
    @NotBlank(message = "处理状态不能为空")
    private String status;

    @Size(max = 500, message = "处理说明不能超过500字")
    private String handleNote;

    /** none / hide / delete；仅在 accepted 时生效 */
    private String action;
}
