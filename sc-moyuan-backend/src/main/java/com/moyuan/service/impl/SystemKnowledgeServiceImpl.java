package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.entity.SystemKnowledge;
import com.moyuan.mapper.SystemKnowledgeMapper;
import com.moyuan.service.SystemKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemKnowledgeServiceImpl extends ServiceImpl<SystemKnowledgeMapper, SystemKnowledge> implements SystemKnowledgeService {

    private final SystemKnowledgeMapper systemKnowledgeMapper;

    @Override
    @Cacheable(value = "systemKnowledge", key = "'enabled'")
    public List<SystemKnowledge> getEnabledList() {
        LambdaQueryWrapper<SystemKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemKnowledge::getIsShow, 1)
               .orderByAsc(SystemKnowledge::getSortOrder)
               .orderByAsc(SystemKnowledge::getId);
        return systemKnowledgeMapper.selectList(wrapper);
    }

    @Override
    public IPage<SystemKnowledge> getAdminList(int pageNum, int pageSize, String category, String keyword) {
        LambdaQueryWrapper<SystemKnowledge> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category)) {
            wrapper.eq(SystemKnowledge::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SystemKnowledge::getTitle, keyword)
                    .or().like(SystemKnowledge::getCode, keyword)
                    .or().like(SystemKnowledge::getKeywords, keyword));
        }
        wrapper.orderByAsc(SystemKnowledge::getSortOrder).orderByAsc(SystemKnowledge::getId);
        return systemKnowledgeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @CacheEvict(value = "systemKnowledge", allEntries = true)
    public SystemKnowledge createKnowledge(SystemKnowledge knowledge) {
        save(knowledge);
        return knowledge;
    }

    @Override
    @CacheEvict(value = "systemKnowledge", allEntries = true)
    public SystemKnowledge updateKnowledge(Long id, SystemKnowledge knowledge) {
        knowledge.setId(id);
        updateById(knowledge);
        return getById(id);
    }

    @Override
    @CacheEvict(value = "systemKnowledge", allEntries = true)
    public void deleteKnowledge(Long id) {
        removeById(id);
    }

    @Override
    @CacheEvict(value = "systemKnowledge", allEntries = true)
    public void updateShow(Long id, Integer isShow) {
        SystemKnowledge knowledge = new SystemKnowledge();
        knowledge.setId(id);
        knowledge.setIsShow(isShow);
        updateById(knowledge);
    }
}
