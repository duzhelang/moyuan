package com.moyuan.service;

import java.util.Map;

public interface PoetSyncService {
    Map<String, Object> syncPoetData(String poetName);
    Map<String, Object> syncAllPoets();
}