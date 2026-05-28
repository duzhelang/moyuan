package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.entity.Poem;
import com.moyuan.mapper.PoemMapper;
import com.moyuan.service.PoemService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PoemServiceImpl extends ServiceImpl<PoemMapper, Poem> implements PoemService {

    @Override
    public List<Poem> getModernPoems() {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, 1)
               .eq(Poem::getDeleted, 0)
               .orderByDesc(Poem::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<Poem> getPoemsByCategory(String category) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, 1)
               .eq(Poem::getDeleted, 0)
               .like(Poem::getTitle, category)
               .or()
               .like(Poem::getContent, category)
               .orderByDesc(Poem::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<Poem> getFeaturedPoems() {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, 1)
               .eq(Poem::getDeleted, 0)
               .eq(Poem::getIsFeatured, 1)
               .orderByDesc(Poem::getCreateTime);
        return list(wrapper);
    }
}
