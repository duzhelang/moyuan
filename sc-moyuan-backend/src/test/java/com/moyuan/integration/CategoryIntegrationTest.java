package com.moyuan.integration;

import com.moyuan.ai.AiModelRegistry;
import com.moyuan.entity.Category;
import com.moyuan.mapper.CategoryMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分类表（category）基于 H2 内存库的集成测试。
 * 数据由 src/test/resources/db/schema-h2.sql 与 data-h2.sql 初始化。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CategoryIntegrationTest {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 启动加载完整上下文时会触发 AI 模型缓存初始化（查询 ai_module_config 表），
     * 该表不在测试 schema 中，故替换为 mock 跳过初始化副作用。
     */
    @MockBean
    private AiModelRegistry aiModelRegistry;

    @Test
    @Order(1)
    void 查询所有分类() {
        List<Category> categories = categoryMapper.selectList(null);

        assertNotNull(categories);
        assertEquals(8, categories.size());

        Category firstCategory = categories.get(0);
        assertNotNull(firstCategory.getId());
        assertNotNull(firstCategory.getName());
        assertNotNull(firstCategory.getSortOrder());
    }

    @Test
    @Order(2)
    void 根据ID查询分类() {
        Category category = categoryMapper.selectById(1L);

        assertNotNull(category);
        assertEquals(1L, category.getId());
        assertEquals("中国古典诗词", category.getName());
        assertEquals(1, category.getSortOrder());
    }

    @Test
    @Order(3)
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
    @Order(4)
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
    @Order(5)
    void 删除分类() {
        // 使用临时插入的数据，避免删除 seed 数据影响其他用例
        Category newCategory = new Category();
        newCategory.setName("待删除分类");
        newCategory.setSortOrder(9);
        categoryMapper.insert(newCategory);

        int result = categoryMapper.deleteById(newCategory.getId());
        assertEquals(1, result);

        Category deletedCategory = categoryMapper.selectById(newCategory.getId());
        assertNull(deletedCategory);
    }
}
