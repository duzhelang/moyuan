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
    private static final int MAX_SUGGESTION_SIZE = 10;

    @Override
    public Map<String, Object> smartSearch(Long dynastyId, Long categoryId, Long poetId,
                                            String keyword, String sortBy, int pageNum, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        
        // 阶段1: 精确匹配
        IPage<Poem> exactResults = exactSearch(dynastyId, categoryId, poetId, keyword, sortBy, pageNum, pageSize);
        if (exactResults.getTotal() > 0) {
            result.put("list", exactResults.getRecords());
            result.put("total", exactResults.getTotal());
            result.put("searchLevel", "exact");
            result.put("message", "找到 " + exactResults.getTotal() + " 条结果");
            return result;
        }

        // 阶段2: 模糊匹配
        if (StringUtils.hasText(keyword)) {
            IPage<Poem> fuzzyResults = fuzzySearch(dynastyId, categoryId, poetId, keyword, sortBy, pageNum, pageSize);
            if (fuzzyResults.getTotal() > 0) {
                result.put("list", fuzzyResults.getRecords());
                result.put("total", fuzzyResults.getTotal());
                result.put("searchLevel", "fuzzy");
                result.put("message", "模糊匹配找到 " + fuzzyResults.getTotal() + " 条结果");
                return result;
            }

            // 阶段3: 拼音匹配
            IPage<Poem> pinyinResults = pinyinSearch(dynastyId, categoryId, poetId, keyword, sortBy, pageNum, pageSize);
            if (pinyinResults.getTotal() > 0) {
                result.put("list", pinyinResults.getRecords());
                result.put("total", pinyinResults.getTotal());
                result.put("searchLevel", "pinyin");
                result.put("message", "拼音匹配找到 " + pinyinResults.getTotal() + " 条结果");
                return result;
            }
        }

        // 阶段4: 无结果，返回空并提示
        result.put("list", Collections.emptyList());
        result.put("total", 0);
        result.put("searchLevel", "none");
        result.put("message", "本地未找到相关诗词");
        result.put("suggestExternal", true);
        
        return result;
    }

    private IPage<Poem> exactSearch(Long dynastyId, Long categoryId, Long poetId,
                                     String keyword, String sortBy, int pageNum, int pageSize) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, 1);
        
        if (dynastyId != null) wrapper.eq(Poem::getDynastyId, dynastyId);
        if (poetId != null) wrapper.eq(Poem::getPoetId, poetId);
        if (categoryId != null) wrapper.eq(Poem::getCategoryId, categoryId);
        
        if (StringUtils.hasText(keyword)) {
            List<Long> matchedPoetIds = findPoetIdsByName(keyword);
            wrapper.and(w -> w.like(Poem::getTitle, keyword)
                    .or().like(Poem::getContent, keyword)
                    .or().in(!matchedPoetIds.isEmpty(), Poem::getPoetId, matchedPoetIds));
        }
        
        applySort(wrapper, sortBy);
        return poemMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    private IPage<Poem> fuzzySearch(Long dynastyId, Long categoryId, Long poetId,
                                     String keyword, String sortBy, int pageNum, int pageSize) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, 1);
        
        if (dynastyId != null) wrapper.eq(Poem::getDynastyId, dynastyId);
        if (poetId != null) wrapper.eq(Poem::getPoetId, poetId);
        if (categoryId != null) wrapper.eq(Poem::getCategoryId, categoryId);
        
        // 模糊匹配: 拆分关键词进行多条件匹配
        String[] keywords = keyword.split("\\s+");
        List<Long> matchedPoetIds = findPoetIdsByName(keyword);
        wrapper.and(w -> {
            for (String kw : keywords) {
                w.or().like(Poem::getTitle, kw)
                  .or().like(Poem::getContent, kw);
                if (!matchedPoetIds.isEmpty()) {
                    w.or().in(Poem::getPoetId, matchedPoetIds);
                }
            }
        });
        
        applySort(wrapper, sortBy);
        return poemMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    private IPage<Poem> pinyinSearch(Long dynastyId, Long categoryId, Long poetId,
                                      String keyword, String sortBy, int pageNum, int pageSize) {
        // 先获取所有符合条件的诗词（限制数量避免内存问题）
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, 1);
        
        if (dynastyId != null) wrapper.eq(Poem::getDynastyId, dynastyId);
        if (poetId != null) wrapper.eq(Poem::getPoetId, poetId);
        if (categoryId != null) wrapper.eq(Poem::getCategoryId, categoryId);
        
        wrapper.last("LIMIT 1000");
        List<Poem> allPoems = poemMapper.selectList(wrapper);
        
        // 批量查询诗人名称并填充到 Poem 对象
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
        allPoems.forEach(p -> p.setPoetName(poetNameMap.get(p.getPoetId())));
        
        // 拼音匹配过滤
        List<Poem> matchedPoems = allPoems.stream()
                .filter(poem -> PinyinUtil.matchesPinyin(poem.getTitle(), keyword) ||
                               PinyinUtil.matchesPinyin(poem.getPoetName(), keyword))
                .skip((long) (pageNum - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        
        // 构造分页结果
        Page<Poem> page = new Page<>(pageNum, pageSize);
        page.setRecords(matchedPoems);
        page.setTotal(matchedPoems.size());
        return page;
    }

    private List<Long> findPoetIdsByName(String keyword) {
        LambdaQueryWrapper<Poet> poetWrapper = new LambdaQueryWrapper<>();
        poetWrapper.like(Poet::getName, keyword).select(Poet::getId);
        return poetMapper.selectList(poetWrapper).stream()
                .map(Poet::getId)
                .collect(Collectors.toList());
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
        
        // 从诗词标题中获取建议
        LambdaQueryWrapper<Poem> titleWrapper = new LambdaQueryWrapper<>();
        titleWrapper.eq(Poem::getStatus, 1)
                .like(Poem::getTitle, keyword)
                .select(Poem::getTitle)
                .last("LIMIT " + limit);
        poemMapper.selectList(titleWrapper)
                .forEach(poem -> suggestions.add(poem.getTitle()));
        
        // 从诗人名字中获取建议
        LambdaQueryWrapper<Poet> poetWrapper = new LambdaQueryWrapper<>();
        poetWrapper.eq(Poet::getStatus, 1)
                .like(Poet::getName, keyword)
                .select(Poet::getName)
                .last("LIMIT " + limit);
        poetMapper.selectList(poetWrapper)
                .forEach(poet -> suggestions.add(poet.getName()));
        
        List<String> result = new ArrayList<>(suggestions).subList(0, Math.min(limit, suggestions.size()));
        
        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
        
        return result;
    }

    @Override
    public List<String> getHotSearches(int limit) {
        Set<Object> hotSearches = redisTemplate.opsForZSet().reverseRange(HOT_SEARCH_KEY, 0, limit - 1);
        if (hotSearches != null && !hotSearches.isEmpty()) {
            return hotSearches.stream().map(Object::toString).collect(Collectors.toList());
        }
        
        // 默认热门搜索
        return Arrays.asList("李白", "杜甫", "苏轼", "静夜思", "春晓", "登鹳雀楼", "相思", "将进酒");
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
        redisTemplate.expire(key, 30, TimeUnit.DAYS);
        
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
