package com.moyuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyuan.entity.PoetProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PoetProfileMapper extends BaseMapper<PoetProfile> {

    @Update("UPDATE poet_profile SET work_count = work_count + 1 WHERE user_id = #{userId}")
    void incrementWorkCount(@Param("userId") Long userId);

    @Update("UPDATE poet_profile SET like_count = like_count + #{count} WHERE user_id = #{userId}")
    void incrementLikeCount(@Param("userId") Long userId, @Param("count") int count);
}
