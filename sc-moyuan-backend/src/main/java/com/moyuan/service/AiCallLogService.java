package com.moyuan.service;

import com.moyuan.entity.AiCallLog;
import com.moyuan.mapper.AiCallLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI 调用日志采集：异步写入 ai_call_log。
 *
 * 与 ApiMetricInterceptor / SearchLogService 保持同一套约定：
 * 单线程 + 有界队列，满了丢弃，异常吞掉，绝不影响 AI 调用本身。
 */
@Slf4j
@Service
public class AiCallLogService {

    private static final int QUEUE_CAPACITY = 1000;
    private static final int MAX_ERROR_LENGTH = 250;
    private static final int MAX_MODEL_LENGTH = 60;

    private final AiCallLogMapper mapper;
    private final BlockingQueue<AiCallLog> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicLong dropped = new AtomicLong();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ai-call-log-writer");
        t.setDaemon(true);
        return t;
    });

    public AiCallLogService(AiCallLogMapper mapper) {
        this.mapper = mapper;
        writer.execute(this::drain);
    }

    public void success(String endpoint, String model, String moduleCode,
                        long durationMs, Long ttftMs, Long userId) {
        record(endpoint, model, moduleCode, "success", null, durationMs, ttftMs, userId);
    }

    public void error(String endpoint, String model, String moduleCode,
                      long durationMs, Long ttftMs, String errorMsg, Long userId) {
        record(endpoint, model, moduleCode, "error", errorMsg, durationMs, ttftMs, userId);
    }

    private void record(String endpoint, String model, String moduleCode, String status,
                        String errorMsg, long durationMs, Long ttftMs, Long userId) {
        try {
            AiCallLog row = new AiCallLog();
            row.setEndpoint(endpoint);
            row.setModel(truncate(model, MAX_MODEL_LENGTH));
            row.setModuleCode(truncate(moduleCode, MAX_MODEL_LENGTH));
            row.setStatus(status);
            row.setErrorMsg(truncate(errorMsg, MAX_ERROR_LENGTH));
            row.setDurationMs((int) Math.min(Math.max(durationMs, 0), 600_000));
            row.setTtftMs(ttftMs == null ? null : (int) Math.min(Math.max(ttftMs, 0), 600_000));
            row.setUserId(userId);
            if (!queue.offer(row)) {
                dropped.incrementAndGet();
            }
        } catch (Exception e) {
            log.debug("AI 调用日志入队失败: {}", e.getMessage());
        }
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
                AiCallLog row = queue.take();
                mapper.insert(row);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                long n = dropped.incrementAndGet();
                if (n % 100 == 1) {
                    log.warn("AI 调用日志写入失败（累计 {} 条）: {}", n, e.getMessage());
                }
            }
        }
    }
}
