package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyuan.entity.Poem;
import com.moyuan.entity.Poet;
import com.moyuan.mapper.PoemMapper;
import com.moyuan.mapper.PoetMapper;
import com.moyuan.service.SmartSearchService;
import com.moyuan.util.PinyinUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartSearchServiceImpl implements SmartSearchService {

    private final PoemMapper poemMapper;
    private final PoetMapper poetMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SEARCH_HISTORY_KEY = "search:history:";
    private static final String HOT_SEARCH_KEY = "search:hot";
    private static final String SUGGESTION_CACHE_KEY = "search:suggestion:";
    private static final int MAX_HISTORY_SIZE = 20;
    private static final int MIN_KEYWORD_LENGTH = 2;
    private static final int DEFAULT_PINYIN_SEARCH_LIMIT = 200; // 默认拼音搜索范围
    private static final int MAX_PINYIN_SEARCH_LIMIT = 1000; // 最大拼音搜索范围
    private static final int STATUS_ACTIVE = 1;
    private static final int SUGGESTION_CACHE_MINUTES = 5;
    private static final int HISTORY_EXPIRE_DAYS = 30;

    @Value("${search.pinyin.limit:200}")
    private int pinyinSearchLimit;

    private static final double SCORE_TITLE_EXACT = 1.0;
    private static final double SCORE_TITLE_CONTAINS = 0.8;
    private static final double SCORE_TITLE_SPLIT_KEYWORD = 0.3;
    private static final double SCORE_CONTENT_CONTAINS = 0.5;
    private static final double SCORE_CONTENT_SPLIT_KEYWORD = 0.2;
    private static final double SCORE_POET_EXACT = 0.9;
    private static final double SCORE_POET_CONTAINS = 0.7;
    private static final double SCORE_POET_PARTIAL = 0.3;
    private static final double SCORE_PINYIN_BASE = 0.3;
    private static final double SCORE_PINYIN_TITLE_BONUS = 0.2;
    private static final double SCORE_PINYIN_POET_BONUS = 0.1;

    private static final List<String> DEFAULT_HOT_SEARCHES = Arrays.asList(
            "李白", "杜甫", "苏轼", "静夜思", "春晓", "登鹳雀楼", "相思", "将进酒"
    );

    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[\\p{P}\\s]+");

    @Override
    public Map<String, Object> smartSearch(Long dynastyId, Long categoryId, Long poetId,
                                            String poetName, String keyword, String sortBy, int pageNum, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        // 解析 poetName 为诗人 ID 列表（模糊匹配）
        Set<Long> poetNameIds = resolvePoetNameIds(poetName);
        
        if (!StringUtils.hasText(keyword)) {
            // 无关键词时按条件筛选
            IPage<Poem> pageResult = searchWithoutKeyword(dynastyId, categoryId, poetId, poetNameIds, sortBy, pageNum, pageSize);
            result.put("list", pageResult.getRecords());
            result.put("total", pageResult.getTotal());
            result.put("searchLevel", "exact");
            result.put("message", String.format("找到 %d 条结果", pageResult.getTotal()));
            return result;
        }
        
        // 综合搜索：同时匹配标题、内容、诗人名
        
        // 1. 搜索诗词（标题、内容）
        List<PoemWithScore> poemResults = searchPoems(dynastyId, categoryId, poetId, poetNameIds, keyword);
        List<PoemWithScore> allResults = new ArrayList<>(poemResults);
        
        // 2. 搜索诗人（按诗人名搜索其作品）
        List<PoemWithScore> poetResults = searchByPoetName(dynastyId, categoryId, keyword);
        allResults.addAll(poetResults);
        
        // 3. 拼音匹配（补充）
        List<PoemWithScore> pinyinResults = searchByPinyin(dynastyId, categoryId, poetId, poetNameIds, keyword);
        allResults.addAll(pinyinResults);
        
        // 去重并按评分排序
        Map<Long, PoemWithScore> uniqueResults = new LinkedHashMap<>();
        for (PoemWithScore pws : allResults) {
            PoemWithScore existing = uniqueResults.get(pws.poem().getId());
            if (existing == null || pws.score() > existing.score()) {
                uniqueResults.put(pws.poem().getId(), pws);
            }
        }
        
        // 分页
        int total = uniqueResults.size();
        List<PoemWithScore> sortedResults = new ArrayList<>(uniqueResults.values());
        sortedResults.sort((a, b) -> Double.compare(b.score(), a.score()));
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Poem> pageList = fromIndex < total 
            ? sortedResults.subList(fromIndex, toIndex).stream().map(pws -> pws.poem()).collect(Collectors.toList())
            : Collections.emptyList();
        
        result.put("list", pageList);
        result.put("total", total);
        result.put("searchLevel", "exact");
        result.put("message", String.format("找到 %d 条结果", total));
        result.put("suggestExternal", total == 0);
        
        return result;
    }
    
    /**
     * 将诗人名称模糊匹配为诗人 ID 列表
     */
    private Set<Long> resolvePoetNameIds(String poetName) {
        if (!StringUtils.hasText(poetName)) return Collections.emptySet();
        LambdaQueryWrapper<Poet> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Poet::getName, poetName).select(Poet::getId);
        return poetMapper.selectList(wrapper).stream().map(Poet::getId).collect(Collectors.toSet());
    }
    
    private IPage<Poem> searchWithoutKeyword(Long dynastyId, Long categoryId, Long poetId,
                                              Set<Long> poetNameIds, String sortBy, int pageNum, int pageSize) {
        LambdaQueryWrapper<Poem> wrapper = buildPoemWrapper(dynastyId, categoryId, poetId, poetNameIds);
        applySort(wrapper, sortBy);
        return poemMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }
    
    private List<PoemWithScore> searchPoems(Long dynastyId, Long categoryId, Long poetId, Set<Long> poetNameIds, String keyword) {
        LambdaQueryWrapper<Poem> wrapper = buildPoemWrapper(dynastyId, categoryId, poetId, poetNameIds);
        
        // 清理关键词：去除标点符号，拆分
        String cleanKeyword = PUNCTUATION_PATTERN.matcher(keyword).replaceAll(" ").trim();
        String[] keywords = cleanKeyword.split("\\s+");
        
        // 同时用原始关键词和拆分后的关键词搜索
        wrapper.and(w -> {
            // 用原始关键词搜索（可能包含标点）
            w.or().like(Poem::getTitle, keyword)
              .or().like(Poem::getContent, keyword);
            // 用拆分后的关键词搜索
            for (String kw : keywords) {
                if (kw.length() >= MIN_KEYWORD_LENGTH) {
                    w.or().like(Poem::getTitle, kw)
                      .or().like(Poem::getContent, kw);
                }
            }
        });
        
        List<Poem> poems = poemMapper.selectList(wrapper);
        List<PoemWithScore> results = new ArrayList<>();
        
        for (Poem poem : poems) {
            double score = calculateScore(poem, keyword, keywords);
            results.add(new PoemWithScore(poem, score));
        }
        
        return results;
    }
    
    private List<PoemWithScore> searchByPoetName(Long dynastyId, Long categoryId, String keyword) {
        // 清理关键词：去除标点符号
        String cleanKeyword = PUNCTUATION_PATTERN.matcher(keyword).replaceAll(" ").trim();
        String[] keywords = cleanKeyword.split("\\s+");
        
        Set<Long> allPoetIds = new HashSet<>();
        Map<Long, String> poetNameMap = new HashMap<>();
        collectPoetIds(keyword, allPoetIds, poetNameMap);
        for (String kw : keywords) {
            if (kw.length() >= MIN_KEYWORD_LENGTH) {
                collectPoetIds(kw, allPoetIds, poetNameMap);
            }
        }
        
        if (allPoetIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 查询这些诗人的作品
        LambdaQueryWrapper<Poem> wrapper = buildPoemWrapper(dynastyId, categoryId, null, Collections.emptySet());
        wrapper.in(Poem::getPoetId, allPoetIds);
        
        List<Poem> poems = poemMapper.selectList(wrapper);
        List<PoemWithScore> results = new ArrayList<>();
        
        for (Poem poem : poems) {
            String poetName = poetNameMap.get(poem.getPoetId());
            double score = calculatePoetScore(poetName, keyword);
            results.add(new PoemWithScore(poem, score));
        }
        
        return results;
    }
    
    private List<PoemWithScore> searchByPinyin(Long dynastyId, Long categoryId, Long poetId, Set<Long> poetNameIds, String keyword) {
        LambdaQueryWrapper<Poem> wrapper = buildPoemWrapper(dynastyId, categoryId, poetId, poetNameIds);
        
        // 使用可配置的拼音搜索限制，确保在合理范围内
        int effectiveLimit = Math.max(DEFAULT_PINYIN_SEARCH_LIMIT, Math.min(pinyinSearchLimit, MAX_PINYIN_SEARCH_LIMIT));
        List<Poem> allPoems = poemMapper.selectPage(new Page<>(1, effectiveLimit), wrapper).getRecords();
        
        // 批量查询诗人名称
        Set<Long> poetIds = allPoems.stream()
                .map(Poem::getPoetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> poetNameMap = new HashMap<>();
        if (!poetIds.isEmpty()) {
            LambdaQueryWrapper<Poet> poetWrapper = new LambdaQueryWrapper<>();
            poetWrapper.in(Poet::getId, poetIds).select(Poet::getId, Poet::getName);
            poetMapper.selectList(poetWrapper)
                    .forEach(p -> poetNameMap.put(p.getId(), p.getName()));
        }
        
        List<PoemWithScore> results = new ArrayList<>();
        for (Poem poem : allPoems) {
            String poetName = poetNameMap.get(poem.getPoetId());
            boolean titleMatch = PinyinUtil.matchesPinyin(poem.getTitle(), keyword);
            boolean poetMatch = PinyinUtil.matchesPinyin(poetName, keyword);
            
            if (titleMatch || poetMatch) {
                double score = SCORE_PINYIN_BASE;
                if (titleMatch) score += SCORE_PINYIN_TITLE_BONUS;
                if (poetMatch) score += SCORE_PINYIN_POET_BONUS;
                results.add(new PoemWithScore(poem, score));
            }
        }
        
        return results;
    }
    
    private double calculateScore(Poem poem, String keyword, String[] keywords) {
        double score = 0;
        String title = poem.getTitle();
        String content = poem.getContent();
        
        // 去除标点符号后比较
        String cleanKeyword = PUNCTUATION_PATTERN.matcher(keyword).replaceAll("");
        String cleanTitle = PUNCTUATION_PATTERN.matcher(title).replaceAll("");
        String cleanContent = content != null ? PUNCTUATION_PATTERN.matcher(content).replaceAll("") : "";
        
        // 标题完全匹配
        if (cleanTitle.equals(cleanKeyword)) {
            score += SCORE_TITLE_EXACT;
        }
        // 标题包含关键词
        else if (cleanTitle.contains(cleanKeyword)) {
            score += SCORE_TITLE_CONTAINS;
        }
        
        // 标题包含拆分后的关键词
        for (String kw : keywords) {
            if (kw.length() >= MIN_KEYWORD_LENGTH && cleanTitle.contains(kw)) {
                score += SCORE_TITLE_SPLIT_KEYWORD;
            }
        }
        
        // 内容包含关键词
        if (cleanContent.contains(cleanKeyword)) {
            score += SCORE_CONTENT_CONTAINS;
        }
        for (String kw : keywords) {
            if (kw.length() >= MIN_KEYWORD_LENGTH && cleanContent.contains(kw)) {
                score += SCORE_CONTENT_SPLIT_KEYWORD;
            }
        }
        
        return score;
    }
    
    private double calculatePoetScore(String poetName, String keyword) {
        if (poetName == null) return 0;
        
        if (poetName.equals(keyword)) {
            return SCORE_POET_EXACT;
        }
        if (poetName.contains(keyword)) {
            return SCORE_POET_CONTAINS;
        }
        return SCORE_POET_PARTIAL;
    }
    
    private record PoemWithScore(Poem poem, double score) {}

    private void collectPoetIds(String keyword, Set<Long> allPoetIds, Map<Long, String> poetNameMap) {
        LambdaQueryWrapper<Poet> poetWrapper = new LambdaQueryWrapper<>();
        poetWrapper.like(Poet::getName, keyword).select(Poet::getId, Poet::getName);
        poetMapper.selectList(poetWrapper).forEach(p -> {
            allPoetIds.add(p.getId());
            poetNameMap.put(p.getId(), p.getName());
        });
    }

    private LambdaQueryWrapper<Poem> buildPoemWrapper(Long dynastyId, Long categoryId, Long poetId, Set<Long> poetNameIds) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, STATUS_ACTIVE);
        if (dynastyId != null) wrapper.eq(Poem::getDynastyId, dynastyId);
        if (poetId != null && !poetNameIds.isEmpty()) {
            wrapper.and(w -> w.eq(Poem::getPoetId, poetId).or().in(Poem::getPoetId, poetNameIds));
        } else if (poetId != null) {
            wrapper.eq(Poem::getPoetId, poetId);
        } else if (!poetNameIds.isEmpty()) {
            wrapper.in(Poem::getPoetId, poetNameIds);
        }
        if (categoryId != null) wrapper.eq(Poem::getCategoryId, categoryId);
        return wrapper;
    }

    private void applySort(LambdaQueryWrapper<Poem> wrapper, String sortBy) {
        if ("popular".equals(sortBy)) {
            wrapper.orderByDesc(Poem::getViewCount);
        } else if ("likes".equals(sortBy)) {
            wrapper.orderByDesc(Poem::getLikeCount);
        } else {
            wrapper.orderByDesc(Poem::getCreateTime);
        }
    }

    @Override
    public List<String> getSearchSuggestions(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        
        String cacheKey = SUGGESTION_CACHE_KEY + keyword;
        @SuppressWarnings("unchecked")
        List<String> cached = (List<String>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        Set<String> suggestions = new LinkedHashSet<>();
        
        // 从诗词标题中获取建议（使用selectList避免selectCount查询）
        LambdaQueryWrapper<Poem> titleWrapper = new LambdaQueryWrapper<>();
        titleWrapper.eq(Poem::getStatus, STATUS_ACTIVE)
                .like(Poem::getTitle, keyword)
                .select(Poem::getTitle)
                .last("LIMIT " + limit);
        poemMapper.selectList(titleWrapper)
                .forEach(poem -> suggestions.add(poem.getTitle()));
        
        // 从诗人名字中获取建议（使用selectList避免selectCount查询）
        LambdaQueryWrapper<Poet> poetWrapper = new LambdaQueryWrapper<>();
        poetWrapper.eq(Poet::getStatus, STATUS_ACTIVE)
                .like(Poet::getName, keyword)
                .select(Poet::getName)
                .last("LIMIT " + limit);
        poetMapper.selectList(poetWrapper)
                .forEach(poet -> suggestions.add(poet.getName()));
        
        List<String> result = new ArrayList<>(suggestions).subList(0, Math.min(limit, suggestions.size()));
        
        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, result, SUGGESTION_CACHE_MINUTES, TimeUnit.MINUTES);
        
        return result;
    }

    @Override
    public List<String> getHotSearches(int limit) {
        Set<Object> hotSearches = redisTemplate.opsForZSet().reverseRange(HOT_SEARCH_KEY, 0, limit - 1);
        if (hotSearches != null && !hotSearches.isEmpty()) {
            return hotSearches.stream().map(Object::toString).collect(Collectors.toList());
        }
        
        return DEFAULT_HOT_SEARCHES;
    }

    @Override
    public void saveSearchHistory(Long userId, String keyword) {
        if (userId == null || !StringUtils.hasText(keyword)) {
            return;
        }
        
        String key = SEARCH_HISTORY_KEY + userId;
        redisTemplate.opsForList().remove(key, 1, keyword);
        redisTemplate.opsForList().leftPush(key, keyword);
        redisTemplate.opsForList().trim(key, 0, MAX_HISTORY_SIZE - 1);
        redisTemplate.expire(key, HISTORY_EXPIRE_DAYS, TimeUnit.DAYS);
        
        // 更新热门搜索
        redisTemplate.opsForZSet().incrementScore(HOT_SEARCH_KEY, keyword, 1);
    }

    @Override
    public List<String> getSearchHistory(Long userId, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }
        
        String key = SEARCH_HISTORY_KEY + userId;
        List<Object> history = redisTemplate.opsForList().range(key, 0, limit - 1);
        if (history != null && !history.isEmpty()) {
            return history.stream().map(Object::toString).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public void clearSearchHistory(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.delete(SEARCH_HISTORY_KEY + userId);
    }
}
