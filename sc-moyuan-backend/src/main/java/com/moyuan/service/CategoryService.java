package com.moyuan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {
    List<Category> getAllCategories();
    Category getCategoryDetail(Long id);
}
