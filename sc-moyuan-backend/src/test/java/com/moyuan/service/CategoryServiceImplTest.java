package com.moyuan.service;

import com.moyuan.entity.Category;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.CategoryMapper;
import com.moyuan.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void getAllCategories_返回排序后的分类列表() {
        Category category1 = new Category();
        category1.setId(1L);
        category1.setName("唐诗");
        category1.setSortOrder(1);

        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("宋词");
        category2.setSortOrder(2);

        List<Category> expectedCategories = Arrays.asList(category1, category2);

        when(categoryMapper.selectList(any())).thenReturn(expectedCategories);

        List<Category> actualCategories = categoryService.getAllCategories();

        assertNotNull(actualCategories);
        assertEquals(2, actualCategories.size());
        assertEquals("唐诗", actualCategories.get(0).getName());
        assertEquals("宋词", actualCategories.get(1).getName());
    }

    @Test
    void getCategoryDetail_存在时返回分类() {
        Category category = new Category();
        category.setId(1L);
        category.setName("唐诗");
        category.setDescription("唐代诗歌");

        when(categoryMapper.selectById(1L)).thenReturn(category);

        Category actualCategory = categoryService.getCategoryDetail(1L);

        assertNotNull(actualCategory);
        assertEquals(1L, actualCategory.getId());
        assertEquals("唐诗", actualCategory.getName());
        assertEquals("唐代诗歌", actualCategory.getDescription());
    }

    @Test
    void getCategoryDetail_不存在时抛出异常() {
        when(categoryMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            categoryService.getCategoryDetail(999L);
        });
    }
}