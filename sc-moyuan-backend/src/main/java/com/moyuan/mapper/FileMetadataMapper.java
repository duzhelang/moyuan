package com.moyuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyuan.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 文件元数据Mapper接口
 */
@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {

    /**
     * 按文件类型聚合数量与体积，供后台资源分布统计，避免把全表记录加载进内存。
     */
    @Select("SELECT file_type AS fileType, COUNT(*) AS count, COALESCE(SUM(size), 0) AS size "
            + "FROM file_metadata GROUP BY file_type")
    List<Map<String, Object>> selectTypeStats();
}
