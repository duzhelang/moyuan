package com.moyuan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.Dynasty;

import java.util.List;

public interface DynastyService extends IService<Dynasty> {
    List<Dynasty> getAllDynasties();
    Dynasty getDynastyDetail(Long id);
    
    /**
     * 根据生卒年确定朝代
     * @param birthYear 出生年份（可为null）
     * @param deathYear 去世年份（可为null）
     * @return 匹配的朝代，如果没有匹配则返回null
     */
    Dynasty determineDynastyByYears(Integer birthYear, Integer deathYear);
}
