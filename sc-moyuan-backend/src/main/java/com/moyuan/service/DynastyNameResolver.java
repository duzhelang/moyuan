package com.moyuan.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 朝代名称归一化解析器
 *
 * <p>上游（今日诗词、外部诗词 API、人工导入）给出的朝代写法与 {@code dynasty.name} 并不一致，
 * 典型差异是「唐代 / 宋朝」「北宋 / 魏晋南北朝」这类前后缀。原实现用全等匹配，
 * 只要差一个字就落成 NULL（曾导致 160 条外部诗朝代全空），因此这里统一做归一：</p>
 *
 * <pre>
 * 原文 → 去空白 → 去后缀（时期 / 朝代 / 年间 / 代 / 朝）→ 别名表 → 朝代名称表
 * </pre>
 *
 * <p>别名表存在数据库 {@code dynasty_alias}，可随时增补而不必改代码；解析结果按 5 分钟缓存，
 * 新增别名后最多 5 分钟生效，也可调用 {@link #refresh()} 立即生效。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynastyNameResolver {

    /** 别名 / 名称索引的缓存时长：朝代是低频变更的字典数据，5 分钟足够 */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    /** 按「长的先剥」排列，避免「魏晋南北朝时期」只被剥掉一个「期」字 */
    private static final String[] SUFFIXES = {"时期", "朝代", "年间", "代", "朝"};

    private final JdbcTemplate jdbcTemplate;

    private volatile Map<String, Long> aliasIndex = Collections.emptyMap();
    private volatile Map<String, Long> nameIndex = Collections.emptyMap();
    private volatile long loadedAt = 0L;

    /**
     * 把上游朝代写法解析为 {@code dynasty.id}。
     *
     * @param raw 原始写法，如「唐代」「宋」「北宋」「魏晋南北朝」
     * @return 匹配到的朝代 id；无法识别返回 {@code null}（调用方应记录原文并兜底）
     */
    public Long resolve(String raw) {
        String key = normalize(raw);
        if (key == null) {
            return null;
        }
        ensureLoaded();
        Long id = aliasIndex.get(key);
        if (id != null) {
            return id;
        }
        return nameIndex.get(key);
    }

    /**
     * 归一化朝代写法：去空白、剥掉「代 / 朝 / 时期」等后缀。
     * 别名表与名称表都用同一套规则建索引，保证两侧可比。
     */
    public String normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String s = raw.trim().replaceAll("[\\s\\u3000]", "");
        for (String suffix : SUFFIXES) {
            if (s.length() > suffix.length() && s.endsWith(suffix)) {
                s = s.substring(0, s.length() - suffix.length());
                break;
            }
        }
        return s.isEmpty() ? null : s;
    }

    /** 重新加载别名与朝代名称（新增别名后可立即调用） */
    public void refresh() {
        Map<String, Long> names = new HashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT `id`, `name` FROM `dynasty`");
            for (Map<String, Object> row : rows) {
                String key = normalize(row.get("name") == null ? null : String.valueOf(row.get("name")));
                if (key != null && row.get("id") instanceof Number) {
                    names.putIfAbsent(key, ((Number) row.get("id")).longValue());
                }
            }
        } catch (Exception e) {
            log.warn("加载朝代名称表失败，将沿用上次缓存: {}", e.getMessage());
        }

        Map<String, Long> aliases = new HashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT `alias`, `dynasty_id` FROM `dynasty_alias`");
            for (Map<String, Object> row : rows) {
                String key = normalize(row.get("alias") == null ? null : String.valueOf(row.get("alias")));
                if (key != null && row.get("dynasty_id") instanceof Number) {
                    aliases.put(key, ((Number) row.get("dynasty_id")).longValue());
                }
            }
        } catch (Exception e) {
            // 新库尚未执行 dynasty_alias 脚本时不该影响主流程，走名称表即可
            log.debug("朝代别名表不可用，仅使用朝代名称匹配: {}", e.getMessage());
        }

        this.nameIndex = Collections.unmodifiableMap(names);
        this.aliasIndex = Collections.unmodifiableMap(aliases);
        this.loadedAt = System.currentTimeMillis();
        log.info("朝代解析器已加载：名称 {} 条，别名 {} 条", names.size(), aliases.size());
    }

    private void ensureLoaded() {
        if (System.currentTimeMillis() - loadedAt > CACHE_TTL_MS) {
            synchronized (this) {
                if (System.currentTimeMillis() - loadedAt > CACHE_TTL_MS) {
                    refresh();
                }
            }
        }
    }
}
