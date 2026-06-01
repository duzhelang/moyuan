package com.moyuan.controller;

import com.moyuan.entity.Category;
import com.moyuan.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    void getAllCategories_返回分类列表() throws Exception {
        Category category1 = new Category();
        category1.setId(1L);
        category1.setName("唐诗");

        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("宋词");

        List<Category> categories = Arrays.asList(category1, category2);

        when(categoryService.getAllCategories()).thenReturn(categories);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("唐诗"))
                .andExpect(jsonPath("$.data[1].name").value("宋词"));
    }

    @Test
    void getCategoryDetail_返回分类详情() throws Exception {
        Category category = new Category();
        category.setId(1L);
        category.setName("唐诗");
        category.setDescription("唐代诗歌");

        when(categoryService.getCategoryDetail(1L)).thenReturn(category);

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("唐诗"))
                .andExpect(jsonPath("$.data.description").value("唐代诗歌"));
    }
}