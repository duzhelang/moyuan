package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.common.ResultCode;
import com.moyuan.entity.Dynasty;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.DynastyMapper;
import com.moyuan.service.DynastyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DynastyServiceImpl extends ServiceImpl<DynastyMapper, Dynasty> implements DynastyService {

    private final DynastyMapper dynastyMapper;

    @Override
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
}
