package com.moyuan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.Dynasty;

import java.util.List;

public interface DynastyService extends IService<Dynasty> {
    List<Dynasty> getAllDynasties();
    Dynasty getDynastyDetail(Long id);
}
