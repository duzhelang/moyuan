package com.moyuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyuan.entity.Poem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PoemMapper extends BaseMapper<Poem> {
}
