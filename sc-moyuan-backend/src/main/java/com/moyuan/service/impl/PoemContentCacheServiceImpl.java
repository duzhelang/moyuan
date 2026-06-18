package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.entity.PoemContentCache;
import com.moyuan.mapper.PoemContentCacheMapper;
import com.moyuan.service.PoemContentCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoemContentCacheServiceImpl implements PoemContentCacheService {

    private final PoemContentCacheMapper poemContentCacheMapper;

    @Override
    public PoemContentCache getCachedContent(String poemTitle, String poetName, String contentType) {
        LambdaQueryWrapper<PoemContentCache> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PoemContentCache::getPoemTitle, poemTitle)
               .eq(PoemContentCache::getPoetName, poetName)
               .eq(PoemContentCache::getContentType, contentType)
               .last("LIMIT 1");
        return poemContentCacheMapper.selectOne(wrapper);
    }

    @Override
    public void saveCachedContent(String poemTitle, String poetName, String contentType, String content, String source) {
        PoemContentCache existing = getCachedContent(poemTitle, poetName, contentType);
        if (existing != null) {
            existing.setContent(content);
            existing.setSource(source);
            poemContentCacheMapper.updateById(existing);
        } else {
            PoemContentCache cache = new PoemContentCache();
            cache.setPoemTitle(poemTitle);
            cache.setPoetName(poetName);
            cache.setContentType(contentType);
            cache.setContent(content);
            cache.setSource(source);
            poemContentCacheMapper.insert(cache);
        }
    }
}
