package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.Poet;

public interface PoetService extends IService<Poet> {
    IPage<Poet> getPoetList(int pageNum, int pageSize, Long dynastyId, String keyword, String poetType);
    Poet getPoetDetail(Long id);
}
