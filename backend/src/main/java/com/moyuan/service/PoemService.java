package com.moyuan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.Poem;

import java.util.List;

public interface PoemService extends IService<Poem> {

    List<Poem> getModernPoems();

    List<Poem> getPoemsByCategory(String category);

    List<Poem> getFeaturedPoems();
}
