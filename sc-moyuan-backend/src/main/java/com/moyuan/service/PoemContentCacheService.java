package com.moyuan.service;

import com.moyuan.entity.PoemContentCache;

public interface PoemContentCacheService {
    PoemContentCache getCachedContent(String poemTitle, String poetName, String contentType);
    void saveCachedContent(String poemTitle, String poetName, String contentType, String content, String source);
}
