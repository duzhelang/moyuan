package com.moyuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyuan.entity.GameRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 游戏对局主表 Mapper。仅继承 BaseMapper，CRUD 交由 LambdaQueryWrapper 完成。
 */
@Mapper
public interface GameRecordMapper extends BaseMapper<GameRecord> {
}
