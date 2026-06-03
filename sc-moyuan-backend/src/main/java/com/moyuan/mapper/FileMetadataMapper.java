package com.moyuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyuan.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件元数据Mapper接口
 */
@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {
}
