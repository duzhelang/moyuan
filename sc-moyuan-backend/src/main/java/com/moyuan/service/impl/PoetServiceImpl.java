package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.common.ResultCode;
import com.moyuan.entity.Poet;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.PoetMapper;
import com.moyuan.service.PoetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PoetServiceImpl extends ServiceImpl<PoetMapper, Poet> implements PoetService {

    private final PoetMapper poetMapper;

    @Override
    public IPage<Poet> getPoetList(int pageNum, int pageSize, Long dynastyId, String keyword) {
        LambdaQueryWrapper<Poet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poet::getStatus, 1);
        if (dynastyId != null) wrapper.eq(Poet::getDynastyId, dynastyId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Poet::getName, keyword);
        }
        wrapper.orderByDesc(Poet::getCreateTime);
        return poetMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Poet getPoetDetail(Long id) {
        Poet poet = poetMapper.selectById(id);
        if (poet == null || poet.getStatus() != 1) {
            throw new BusinessException(ResultCode.POET_NOT_FOUND);
        }
        return poet;
    }
}
