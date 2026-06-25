package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.common.ResultCode;
import com.moyuan.entity.Dynasty;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.DynastyMapper;
import com.moyuan.service.DynastyService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DynastyServiceImpl extends ServiceImpl<DynastyMapper, Dynasty> implements DynastyService {

    private final DynastyMapper dynastyMapper;

    @Override
    @Cacheable(value = "dynasties", key = "'all'")
    public List<Dynasty> getAllDynasties() {
        return dynastyMapper.selectList(new LambdaQueryWrapper<Dynasty>().orderByAsc(Dynasty::getSortOrder));
    }

    @Override
    public Dynasty getDynastyDetail(Long id) {
        Dynasty dynasty = dynastyMapper.selectById(id);
        if (dynasty == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return dynasty;
    }

    @Override
    public Dynasty determineDynastyByYears(Integer birthYear, Integer deathYear) {
        if (birthYear == null && deathYear == null) {
            return null;
        }
        
        List<Dynasty> dynasties = getAllDynasties();
        
        // 使用生卒年的中间点来确定朝代
        Integer targetYear = null;
        if (birthYear != null && deathYear != null) {
            targetYear = (birthYear + deathYear) / 2;
        } else if (birthYear != null) {
            targetYear = birthYear + 30; // 估计寿命30年
        } else {
            targetYear = deathYear - 30; // 估计寿命30年
        }
        
        for (Dynasty dynasty : dynasties) {
            Integer startYear = dynasty.getStartYear();
            Integer endYear = dynasty.getEndYear();
            
            // 处理起始年份为null的情况（如先秦）
            if (startYear == null && endYear != null && targetYear <= endYear) {
                return dynasty;
            }
            
            // 处理结束年份为null的情况（如现代）
            if (startYear != null && endYear == null && targetYear >= startYear) {
                return dynasty;
            }
            
            // 正常范围判断
            if (startYear != null && endYear != null) {
                if (targetYear >= startYear && targetYear <= endYear) {
                    return dynasty;
                }
            }
        }
        
        return null;
    }
}
