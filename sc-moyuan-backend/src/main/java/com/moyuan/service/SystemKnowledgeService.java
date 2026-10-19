package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.SystemKnowledge;

import java.util.List;

public interface SystemKnowledgeService extends IService<SystemKnowledge> {

    /**
     * 获取启用中的知识条目（按 sort_order、id 升序）
     */
    List<SystemKnowledge> getEnabledList();

    /**
     * 管理端分页查询知识条目
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param category 分类筛选（可选）
     * @param keyword  关键字（匹配标题、条目标识或关键词，可选）
     * @return 分页结果
     */
    IPage<SystemKnowledge> getAdminList(int pageNum, int pageSize, String category, String keyword);

    /**
     * 新建知识条目（写入后清空知识库缓存）
     */
    SystemKnowledge createKnowledge(SystemKnowledge knowledge);

    /**
     * 更新知识条目（写入后清空知识库缓存）
     */
    SystemKnowledge updateKnowledge(Long id, SystemKnowledge knowledge);

    /**
     * 删除知识条目（逻辑删除，删除后清空知识库缓存）
     */
    void deleteKnowledge(Long id);

    /**
     * 启用/停用知识条目（变更后清空知识库缓存）
     */
    void updateShow(Long id, Integer isShow);
}
