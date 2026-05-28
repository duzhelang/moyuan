package com.moyuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyuan.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
