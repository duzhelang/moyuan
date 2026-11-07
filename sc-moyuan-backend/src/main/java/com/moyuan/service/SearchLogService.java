package com.moyuan.service;

import com.moyuan.entity.SearchLog;
import com.moyuan.mapper.SearchLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 搜索埋点采集：把关键词与命中数异步写入 search_log。
 *
 * 设计要点（与 ApiMetricInterceptor 保持一致）：
 * 1. 只记录「关键词 + 命中数」，聚合在 SearchMetricService 里做；
 * 2. 单线程 + 有界队列，队列满直接丢弃，绝不阻塞搜索请求；
 * 3. 关键词做 trim + 长度截断，避免超长串撑爆索引；
 * 4. 任何异常都吞掉，只按批打日志。
 */
@Slf4j
@Service
public class SearchLogService {

    private static final int QUEUE_CAPACITY = 2000;
    private static final int MAX_KEYWORD_LENGTH = 120;

    private final SearchLogMapper mapper;
    private final BlockingQueue<SearchLog> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicLong dropped = new AtomicLong();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "search-log-writer");
        t.setDaemon(true);
        return t;
    });

    public SearchLogService(SearchLogMapper mapper) {
        this.mapper = mapper;
        writer.execute(this::drain);
    }

    /**
     * 记录一次搜索。调用方在结果返回后调用，异常不影响业务。
     *
     * @param keyword  原始关键词
     * @param total    命中总数
     * @param route    命中路径
     * @param source   smart / global
     * @param userId   可为 null（未登录）
     */
    public void record(String keyword, int total, String route, String source, Long userId) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        String clean = keyword.trim();
        if (clean.length() > MAX_KEYWORD_LENGTH) {
            clean = clean.substring(0, MAX_KEYWORD_LENGTH);
        }
        try {
            SearchLog row = new SearchLog();
            row.setKeyword(clean);
            row.setResultCount(Math.max(total, 0));
            row.setRoute(route);
            row.setSource(source);
            row.setUserId(userId);
            if (!queue.offer(row)) {
                dropped.incrementAndGet();
            }
        } catch (Exception e) {
            log.debug("搜索埋点入队失败: {}", e.getMessage());
        }
    }

    private void drain() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                SearchLog row = queue.take();
                mapper.insert(row);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                long n = dropped.incrementAndGet();
                if (n % 100 == 1) {
                    log.warn("搜索埋点写入失败（累计 {} 条）: {}", n, e.getMessage());
                }
            }
        }
    }
}
