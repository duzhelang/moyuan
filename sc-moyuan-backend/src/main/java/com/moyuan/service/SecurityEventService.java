package com.moyuan.service;

import com.moyuan.entity.SecurityEvent;
import com.moyuan.mapper.SecurityEventMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 安全事件采集：登录失败 / 越权 403 / 限流触发。
 *
 * 注意：401 与 403 由 Spring Security 过滤器直接写出响应，不经过 MVC 与全局异常处理器，
 * 因此必须在 EntryPoint / AccessDeniedHandler 里显式调用本服务。
 */
@Slf4j
@Service
public class SecurityEventService {

    private static final int QUEUE_CAPACITY = 1000;
    private static final int MAX_DETAIL_LENGTH = 250;
    private static final int MAX_IP_LENGTH = 60;

    private final SecurityEventMapper mapper;
    private final BlockingQueue<SecurityEvent> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicLong dropped = new AtomicLong();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "security-event-writer");
        t.setDaemon(true);
        return t;
    });

    public SecurityEventService(SecurityEventMapper mapper) {
        this.mapper = mapper;
        writer.execute(this::drain);
    }

    public void record(String type, String detail, String ip, Long userId) {
        try {
            SecurityEvent event = new SecurityEvent();
            event.setType(type);
            event.setDetail(truncate(detail, MAX_DETAIL_LENGTH));
            event.setIp(truncate(ip, MAX_IP_LENGTH));
            event.setUserId(userId);
            if (!queue.offer(event)) {
                dropped.incrementAndGet();
            }
        } catch (Exception e) {
            log.debug("安全事件入队失败: {}", e.getMessage());
        }
    }

    /** 从请求里取真实 IP，兼容 Nginx 反代 */
    public void recordFromRequest(String type, String detail, HttpServletRequest request, Long userId) {
        record(type, detail, clientIp(request), userId);
    }

    public static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private void drain() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                SecurityEvent event = queue.take();
                mapper.insert(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                long n = dropped.incrementAndGet();
                if (n % 100 == 1) {
                    log.warn("安全事件写入失败（累计 {} 条）: {}", n, e.getMessage());
                }
            }
        }
    }
}
