package com.moyuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyuan.entity.GameSentence;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对局有效诗句明细表 Mapper。仅继承 BaseMapper，CRUD 交由 LambdaQueryWrapper 完成。
 */
@Mapper
public interface GameSentenceMapper extends BaseMapper<GameSentence> {
}
