package com.moyuan.controller;

import com.moyuan.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * 外部诗词源统一代理控制器
 *
 * <p>所有需要从浏览器侧直连外部诗源（PoetryDB / 今日诗词）的调用统一走后端代理，
 * 规避浏览器直连外站被 QUIC 中断 / CORS / 被墙等问题，前端只与自家后端交互。</p>
 */
@Tag(name = "外部诗词代理接口")
@RestController
@RequestMapping("/api/external-poetry")
@RequiredArgsConstructor
public class ExternalPoetryController {

    private final RestTemplate restTemplate;

    private static final String POETRYDB_BASE = "https://poetrydb.org";
    private static final String JINRISHICI_URL = "https://v2.jinrishici.com/one.json";

    // 今日诗词简单内存缓存（键 -> [缓存时刻, 数据]），60 分钟过期
    private static final long JINRISHICI_TTL_MS = 60L * 60 * 1000;
    private volatile long jinrishiciCachedAt = 0L;
    private volatile Object jinrishiciCache = null;

    @Operation(summary = "PoetryDB代理-随机英文诗词")
    @GetMapping("/poetrydb/random")
    public R<List<Object>> poetryDbRandom(@RequestParam(defaultValue = "20") int count) {
        String url = POETRYDB_BASE + "/random/" + count;
        try {
            Object body = restTemplate.getForObject(url, Object.class);
            return R.success(asList(body));
        } catch (Exception e) {
            return R.error("PoetryDB随机获取失败");
        }
    }

    @Operation(summary = "PoetryDB代理-按标题搜索英文诗词")
    @GetMapping("/poetrydb/title")
    public R<List<Object>> poetryDbTitle(@RequestParam String q) {
        String url = POETRYDB_BASE + "/title/" + URLEncoder.encode(q, StandardCharsets.UTF_8);
        try {
            Object body = restTemplate.getForObject(url, Object.class);
            return R.success(asList(body));
        } catch (Exception e) {
            return R.error("PoetryDB标题搜索失败");
        }
    }

    @Operation(summary = "PoetryDB代理-按作者搜索英文诗词")
    @GetMapping("/poetrydb/author")
    public R<List<Object>> poetryDbAuthor(@RequestParam String q) {
        String url = POETRYDB_BASE + "/author/" + URLEncoder.encode(q, StandardCharsets.UTF_8);
        try {
            Object body = restTemplate.getForObject(url, Object.class);
            return R.success(asList(body));
        } catch (Exception e) {
            return R.error("PoetryDB作者搜索失败");
        }
    }

    @Operation(summary = "PoetryDB代理-按标题和作者组合搜索")
    @GetMapping("/poetrydb/title-author")
    public R<List<Object>> poetryDbTitleAuthor(@RequestParam String title, @RequestParam String author) {
        String url = POETRYDB_BASE + "/title,author/"
                + URLEncoder.encode(title, StandardCharsets.UTF_8)
                + ";" + URLEncoder.encode(author, StandardCharsets.UTF_8);
        try {
            Object body = restTemplate.getForObject(url, Object.class);
            return R.success(asList(body));
        } catch (Exception e) {
            return R.error("PoetryDB组合搜索失败");
        }
    }

    @Operation(summary = "今日诗词代理-获取推荐中文古诗词")
    @GetMapping("/jinrishici")
    public R<Object> jinrishici() {
        long now = System.currentTimeMillis();
        if (jinrishiciCache != null && now - jinrishiciCachedAt < JINRISHICI_TTL_MS) {
            return R.success(jinrishiciCache);
        }
        try {
            Object body = restTemplate.getForObject(JINRISHICI_URL, Object.class);
            if (body == null) {
                return R.error("今日诗词获取失败");
            }
            jinrishiciCache = body;
            jinrishiciCachedAt = now;
            return R.success(body);
        } catch (Exception e) {
            return R.error("今日诗词获取失败");
        }
    }

    private List<Object> asList(Object body) {
        if (body instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) body;
            return list;
        }
        return Collections.emptyList();
    }
}