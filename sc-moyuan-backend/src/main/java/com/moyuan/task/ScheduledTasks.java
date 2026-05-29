package com.moyuan.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final RedisTemplate<String, Object> redisTemplate;

    @Scheduled(cron = "0 0 0 * * ?")
    public void refreshDailyPoem() {
        redisTemplate.delete("poems::daily");
        log.info("每日推荐诗词已刷新，日期：{}", LocalDate.now());
    }
}
