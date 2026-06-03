package com.moyuan.aspect;

import com.moyuan.entity.VisitLog;
import com.moyuan.mapper.VisitLogMapper;
import com.moyuan.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class VisitLogAspect {

    private final VisitLogMapper visitLogMapper;
    private final SecurityUtil securityUtil;

    @Pointcut("execution(* com.moyuan.controller.*.*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String path = request.getRequestURI();

                if (shouldLog(path)) {
                    VisitLog visitLog = new VisitLog();
                    visitLog.setIp(getClientIp(request));
                    visitLog.setUserAgent(request.getHeader("User-Agent"));
                    visitLog.setPath(path);
                    visitLog.setVisitDate(LocalDate.now());

                    try {
                        Long userId = securityUtil.getCurrentUserId();
                        visitLog.setUserId(userId);
                    } catch (Exception ignored) {
                    }

                    try {
                        visitLogMapper.insert(visitLog);
                    } catch (Exception e) {
                        log.error("保存访问日志失败", e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("记录访问日志异常", e);
        }

        return joinPoint.proceed();
    }

    private boolean shouldLog(String path) {
        if (path == null) return false;
        return path.startsWith("/api/") &&
               !path.contains("/admin/logs") &&
               !path.contains("/admin/stats");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
