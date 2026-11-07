package com.moyuan.interceptor;

import com.moyuan.entity.ApiRequestMetric;
import com.moyuan.mapper.ApiRequestMetricMapper;
import com.moyuan.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 接口指标采集：把每个 /api/** 请求的路径、状态码、耗时异步写入 api_request_metric。
 *
 * 设计要点：
 * 1. 只做「记录」，不做统计，统计在 ApiMetricService 里按时间窗聚合；
 * 2. 写入走单线程 + 有界队列，队列满直接丢弃（丢弃数只计数，不阻塞请求）；
 * 3. 健康检查自身不记录，避免 30s 轮询把数据冲稀；
 * 4. 任何异常都吞掉，绝不影响业务请求。
 */
@Slf4j
@Component
public class ApiMetricInterceptor implements HandlerInterceptor {

    private static final String START_ATTR = "moyuan.api.metric.start";
    private static final int QUEUE_CAPACITY = 2000;
    private static final int MAX_DURATION_MS = 60_000;

    private final ApiRequestMetricMapper mapper;
    private final BlockingQueue<ApiRequestMetric> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicLong dropped = new AtomicLong();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "api-metric-writer");
        t.setDaemon(true);
        return t;
    });

    public ApiMetricInterceptor(ApiRequestMetricMapper mapper) {
        this.mapper = mapper;
        writer.execute(this::drain);
    }

    private void drain() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ApiRequestMetric metric = queue.take();
                mapper.insert(metric);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // 表缺失或数据库抖动时不要刷屏：每 100 次丢弃打一条
                long n = dropped.incrementAndGet();
                if (n % 100 == 1) {
                    log.warn("接口指标写入失败（累计 {} 条）: {}", n, e.getMessage());
                }
            }
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (shouldRecord(request)) {
            request.setAttribute(START_ATTR, System.nanoTime());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object start = request.getAttribute(START_ATTR);
        if (start == null) {
            return;
        }
        long durationMs = (System.nanoTime() - (Long) start) / 1_000_000;
        if (durationMs > MAX_DURATION_MS) {
            durationMs = MAX_DURATION_MS;
        }

        ApiRequestMetric metric = new ApiRequestMetric();
        metric.setMethod(request.getMethod());
        metric.setPath(request.getRequestURI());
        metric.setStatusCode(response.getStatus());
        metric.setDurationMs((int) durationMs);
        try {
            metric.setUserId(SecurityUtil.getCurrentUserIdOrNull());
        } catch (Exception ignore) {
            metric.setUserId(null);
        }

        if (!queue.offer(metric)) {
            dropped.incrementAndGet();
        }
    }

    private boolean shouldRecord(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/")) {
            return false;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        // 健康检查与指标查询自身不记账，避免自噪声
        if (uri.startsWith("/api/admin/system/")) {
            return false;
        }
        return true;
    }
}
