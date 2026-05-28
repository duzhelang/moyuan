package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.entity.Category;
import com.moyuan.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "分类接口")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取所有分类列表")
    @GetMapping
    public R<List<Category>> getAllCategories() {
        return R.success(categoryService.getAllCategories());
    }

    @Operation(summary = "获取分类详情")
    @GetMapping("/{id}")
    public R<Category> getCategoryDetail(@PathVariable Long id) {
        return R.success(categoryService.getCategoryDetail(id));
    }
}
