package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.entity.Rhyme;
import com.moyuan.mapper.RhymeMapper;
import com.moyuan.service.RhymeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RhymeServiceImpl implements RhymeService {

    private final RhymeMapper rhymeMapper;

    @Override
    public List<Rhyme> queryByCharacter(String character) {
        return rhymeMapper.selectList(
                new LambdaQueryWrapper<Rhyme>()
                        .eq(Rhyme::getCharacter, character)
                        .orderByAsc(Rhyme::getSortOrder)
        );
    }

    @Override
    public List<Rhyme> queryByRhymeGroup(String rhymeGroup) {
        return rhymeMapper.selectList(
                new LambdaQueryWrapper<Rhyme>()
                        .eq(Rhyme::getRhymeGroup, rhymeGroup)
                        .orderByAsc(Rhyme::getSortOrder)
        );
    }
}
