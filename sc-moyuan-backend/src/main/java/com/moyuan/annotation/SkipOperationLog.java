package com.moyuan.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记在 Controller 方法上，表示跳过操作日志 / 访问日志切面。
 * <p>主要用于返回 {@code SseEmitter} 的流式接口：切面对返回值序列化必然失败且无意义。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipOperationLog {
}
