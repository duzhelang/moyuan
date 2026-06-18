package com.moyuan.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.moyuan.entity.OperationLog;
import com.moyuan.mapper.OperationLogMapper;
import com.moyuan.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "oldPassword", "newPassword", "token", "secret"
    );

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    @Pointcut("execution(* com.moyuan.controller.*.*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperationLog operationLog = new OperationLog();

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                operationLog.setMethod(request.getMethod() + " " + request.getRequestURI());
                operationLog.setIp(request.getRemoteAddr());
            }

            operationLog.setOperation(joinPoint.getSignature().getName());
            operationLog.setParams(sanitizeParams(joinPoint.getArgs()));

            try {
                operationLog.setUserId(SecurityUtil.getCurrentUserId());
                operationLog.setUsername(SecurityUtil.getCurrentUsername());
            } catch (Exception ignored) {
            }

            Object result = joinPoint.proceed();
            operationLog.setStatus(1);
            return result;
        } catch (Throwable e) {
            operationLog.setStatus(0);
            operationLog.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            operationLog.setDuration((int) (System.currentTimeMillis() - startTime));
            try {
                operationLogMapper.insert(operationLog);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }
    }

    private String sanitizeParams(Object[] args) {
        try {
            List<Object> sanitized = new ArrayList<>();
            for (Object arg : args) {
                if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) {
                    continue;
                }
                sanitized.add(maskSensitiveFields(arg));
            }
            return objectMapper.writeValueAsString(sanitized);
        } catch (Exception e) {
            return "[序列化失败]";
        }
    }

    private Object maskSensitiveFields(Object obj) {
        try {
            String json = objectMapper.writeValueAsString(obj);
            ObjectNode node = objectMapper.readValue(json, ObjectNode.class);
            for (String field : SENSITIVE_FIELDS) {
                if (node.has(field)) {
                    node.put(field, "***");
                }
            }
            return node;
        } catch (Exception e) {
            return obj;
        }
    }
}
