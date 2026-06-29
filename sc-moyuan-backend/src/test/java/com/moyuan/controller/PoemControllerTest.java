package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyuan.entity.Poem;
import com.moyuan.service.DynastyService;
import com.moyuan.service.PoemService;
import com.moyuan.service.PoetService;
import com.moyuan.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PoemController.class)
class PoemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PoemService poemService;
    @MockBean
    private UserService userService;
    @MockBean
    private PoetService poetService;
    @MockBean
    private DynastyService dynastyService;
    @MockBean
    private CacheManager cacheManager;
    @MockBean
    private RestTemplate restTemplate;

    @Test
    void getPoemList_返回分页结果() throws Exception {
        Page<Poem> page = new Page<>(1, 10);
        Poem poem1 = new Poem();
        poem1.setId(1L);
        poem1.setTitle("静夜思");
        poem1.setStatus(1);

        Poem poem2 = new Poem();
        poem2.setId(2L);
        poem2.setTitle("春晓");
        poem2.setStatus(1);

        page.setRecords(Arrays.asList(poem1, poem2));
        page.setTotal(2);

        when(poemService.getPoemList(anyInt(), anyInt(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/poems")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list.length()").value(2))
                .andExpect(jsonPath("$.data.list[0].title").value("静夜思"))
                .andExpect(jsonPath("$.data.list[1].title").value("春晓"))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void getPoemList_带筛选条件() throws Exception {
        Page<Poem> page = new Page<>(1, 10);
        Poem poem = new Poem();
        poem.setId(1L);
        poem.setTitle("静夜思");
        page.setRecords(Collections.singletonList(poem));
        page.setTotal(1);

        when(poemService.getPoemList(anyInt(), anyInt(), anyLong(), anyLong(), anyLong(), anyString()))
                .thenReturn(page);

        mockMvc.perform(get("/api/poems")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("dynastyId", "1")
                        .param("poetId", "1")
                        .param("categoryId", "1")
                        .param("keyword", "李白"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list.length()").value(1));
    }

    @Test
    void getPoemDetail_返回诗词详情() throws Exception {
        Poem poem = new Poem();
        poem.setId(1L);
        poem.setTitle("静夜思");
        poem.setContent("床前明月光，疑是地上霜。");
        poem.setViewCount(100);
        poem.setLikeCount(50);

        when(poemService.getPoemDetail(1L)).thenReturn(poem);

        mockMvc.perform(get("/api/poems/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("静夜思"))
                .andExpect(jsonPath("$.data.content").value("床前明月光，疑是地上霜。"))
                .andExpect(jsonPath("$.data.viewCount").value(100))
                .andExpect(jsonPath("$.data.likeCount").value(50));
    }

    @Test
    void getDailyPoems_返回每日推荐() throws Exception {
        Poem poem1 = new Poem();
        poem1.setId(1L);
        poem1.setTitle("静夜思");
        poem1.setStatus(1);
        poem1.setViewCount(1000);

        Poem poem2 = new Poem();
        poem2.setId(2L);
        poem2.setTitle("春晓");
        poem2.setStatus(1);
        poem2.setViewCount(800);

        when(poemService.list(any())).thenReturn(Arrays.asList(poem1, poem2));

        mockMvc.perform(get("/api/poems/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getModernPoems_返回现代诗词分页() throws Exception {
        Page<Poem> page = new Page<>(1, 10);
        Poem poem = new Poem();
        poem.setId(1L);
        poem.setTitle("现代诗");
        poem.setDynastyId(13L);
        page.setRecords(Collections.singletonList(poem));
        page.setTotal(1);

        when(poemService.getModernPoems(anyInt(), anyInt(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/poems/modern/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list.length()").value(1));
    }
}
