package com.moyuan.exception;

import com.moyuan.common.R;
import com.moyuan.common.ResultCode;
import com.moyuan.entity.SecurityEvent;
import com.moyuan.service.SecurityEventService;
import com.moyuan.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final SecurityEventService securityEventService;

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        // AI 限流触发属于安全审计事件，单独计数（不影响接口错误率）
        if (ResultCode.AI_RATE_LIMITED.getCode().equals(e.getCode())) {
            securityEventService.record(SecurityEvent.TYPE_RATE_LIMIT, e.getMessage(), null,
                    SecurityUtil.getCurrentUserIdOrNull());
        }
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败：{}", message);
        return R.error(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败：{}", message);
        return R.error(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        // 静态资源/未映射路径缺失属于正常 404，不应作为系统故障记录 ERROR 日志
        return R.error(ResultCode.NOT_FOUND.getCode(), ResultCode.NOT_FOUND.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        // 上传体积由 application.yml 的 spring.servlet.multipart 控制，这里只做友好提示
        log.warn("上传文件超过大小限制：{}", e.getMessage());
        return R.error(ResultCode.PARAM_ERROR.getCode(), "上传文件不能超过 10MB，请压缩后重试");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.error(ResultCode.INTERNAL_ERROR.getCode(), ResultCode.INTERNAL_ERROR.getMessage());
    }
}
