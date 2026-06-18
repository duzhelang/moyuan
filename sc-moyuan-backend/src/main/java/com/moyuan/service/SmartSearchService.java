package com.moyuan.service;

import java.util.List;
import java.util.Map;

public interface SmartSearchService {
    
    Map<String, Object> smartSearch(Long dynastyId, Long categoryId, Long poetId, 
                                     String keyword, String sortBy, int pageNum, int pageSize);
    
    List<String> getSearchSuggestions(String keyword, int limit);
    
    List<String> getHotSearches(int limit);
    
    void saveSearchHistory(Long userId, String keyword);
    
    List<String> getSearchHistory(Long userId, int limit);
    
    void clearSearchHistory(Long userId);
}
