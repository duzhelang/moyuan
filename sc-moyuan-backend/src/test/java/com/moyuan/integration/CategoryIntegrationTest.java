package com.moyuan.integration;

import com.moyuan.entity.Category;
import com.moyuan.mapper.CategoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryIntegrationTest {

    @Autowired
    private CategoryMapper categoryMapper;

    @Test
    void 查询所有分类() {
        List<Category> categories = categoryMapper.selectList(null);
        
        assertNotNull(categories);
        assertEquals(4, categories.size());
        
        Category firstCategory = categories.get(0);
        assertNotNull(firstCategory.getId());
        assertNotNull(firstCategory.getName());
        assertNotNull(firstCategory.getSortOrder());
    }

    @Test
    void 根据ID查询分类() {
        Category category = categoryMapper.selectById(1L);
        
        assertNotNull(category);
        assertEquals(1L, category.getId());
        assertEquals("五言绝句", category.getName());
        assertEquals(1, category.getSortOrder());
    }

    @Test
    void 插入新分类() {
        Category newCategory = new Category();
        newCategory.setName("新分类");
        newCategory.setDescription("测试新分类");
        newCategory.setSortOrder(5);
        
        int result = categoryMapper.insert(newCategory);
        
        assertEquals(1, result);
        assertNotNull(newCategory.getId());
        
        Category savedCategory = categoryMapper.selectById(newCategory.getId());
        assertNotNull(savedCategory);
        assertEquals("新分类", savedCategory.getName());
    }

    @Test
    void 更新分类() {
        Category category = categoryMapper.selectById(1L);
        assertNotNull(category);
        
        category.setDescription("更新后的描述");
        int result = categoryMapper.updateById(category);
        
        assertEquals(1, result);
        
        Category updatedCategory = categoryMapper.selectById(1L);
        assertEquals("更新后的描述", updatedCategory.getDescription());
    }

    @Test
    void 删除分类() {
        int result = categoryMapper.deleteById(1L);
        assertEquals(1, result);
        
        Category deletedCategory = categoryMapper.selectById(1L);
        assertNull(deletedCategory);
    }
}