package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.Poet;

public interface PoetService extends IService<Poet> {
    IPage<Poet> getPoetList(int pageNum, int pageSize, Long dynastyId, Long categoryId, String keyword, String poetType);
    Poet getPoetDetail(Long id);
    void clearAllCache();

    /** 按最新规则回填/修正存量诗人头像，返回实际更新的记录数 */
    int refreshAvatars();
}
