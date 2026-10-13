package com.moyuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyuan.entity.Rhyme;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RhymeMapper extends BaseMapper<Rhyme> {

    /**
     * 幂等插入韵脚字。
     * 依赖唯一键 uk_rhyme_char_group_tone(character, rhyme_group, tone_type)，
     * 已存在则忽略。character 是 MySQL 关键字，必须使用反引号包裹。
     * create_time 由 SQL 内的 NOW() 填充（自定义 @Insert 不走 MyBatis-Plus 自动填充）。
     *
     * @param rhyme 待插入的韵脚实体
     * @return 实际写入行数（0 表示已存在被忽略）
     */
    @Insert("INSERT IGNORE INTO rhyme (`character`, rhyme_group, tone_type, rhyme_category, sort_order, create_time) "
            + "VALUES (#{r.character}, #{r.rhymeGroup}, #{r.toneType}, #{r.rhymeCategory}, #{r.sortOrder}, NOW())")
    int insertIgnore(@Param("r") Rhyme rhyme);
}
