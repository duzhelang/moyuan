package com.moyuan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyuan.entity.Poem;
import com.moyuan.entity.UserFavorite;
import com.moyuan.entity.UserLike;
import com.moyuan.enums.TargetType;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.*;
import com.moyuan.service.impl.PoemServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PoemServiceImplTest {

    @Mock
    private PoemMapper poemMapper;
    @Mock
    private PoetMapper poetMapper;
    @Mock
    private DynastyMapper dynastyMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private UserLikeMapper userLikeMapper;
    @Mock
    private UserFavoriteMapper userFavoriteMapper;
    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private PoemServiceImpl poemService;

    @Test
    void getPoemList_返回分页结果() {
        Page<Poem> page = new Page<>(1, 10);
        Poem poem = new Poem();
        poem.setId(1L);
        poem.setTitle("静夜思");
        poem.setStatus(1);
        page.setRecords(Collections.singletonList(poem));
        page.setTotal(1);

        when(poemMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        IPage<Poem> result = poemService.getPoemList(1, 10, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals("静夜思", result.getRecords().get(0).getTitle());
    }

    @Test
    void getPoemList_带筛选条件() {
        Page<Poem> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);

        when(poemMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        IPage<Poem> result = poemService.getPoemList(1, 10, 1L, 1L, 1L, "李白");

        assertNotNull(result);
        assertEquals(0, result.getRecords().size());
    }

    @Test
    void getPoemDetail_存在时返回诗词() {
        Poem poem = new Poem();
        poem.setId(1L);
        poem.setTitle("静夜思");
        poem.setStatus(1);
        poem.setViewCount(100);

        when(poemMapper.selectById(1L)).thenReturn(poem);

        Poem result = poemService.getPoemDetail(1L);

        assertNotNull(result);
        assertEquals("静夜思", result.getTitle());
        assertEquals(101, result.getViewCount());
    }

    @Test
    void getPoemDetail_不存在时抛出异常() {
        when(poemMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            poemService.getPoemDetail(999L);
        });
    }

    @Test
    void getPoemDetail_未审核时抛出异常() {
        Poem poem = new Poem();
        poem.setId(1L);
        poem.setStatus(0);

        when(poemMapper.selectById(1L)).thenReturn(poem);

        assertThrows(BusinessException.class, () -> {
            poemService.getPoemDetail(1L);
        });
    }

    @Test
    void toggleLike_未点赞时添加点赞() {
        when(userLikeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userLikeMapper.insert(any(UserLike.class))).thenReturn(1);

        poemService.toggleLike(1L, 1L);

        verify(userLikeMapper).insert(any(UserLike.class));
        verify(poemMapper).update(any(), any(LambdaQueryWrapper.class));
    }

    @Test
    void toggleLike_已点赞时取消点赞() {
        when(userLikeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(userLikeMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        poemService.toggleLike(1L, 1L);

        verify(userLikeMapper).delete(any(LambdaQueryWrapper.class));
        verify(poemMapper).update(any(), any(LambdaQueryWrapper.class));
    }

    @Test
    void isLike_已点赞返回true() {
        when(userLikeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        boolean result = poemService.isLike(1L, 1L);

        assertTrue(result);
    }

    @Test
    void isLike_未点赞返回false() {
        when(userLikeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        boolean result = poemService.isLike(1L, 1L);

        assertFalse(result);
    }

    @Test
    void getFavorites_有收藏时返回分页() {
        UserFavorite favorite = new UserFavorite();
        favorite.setTargetId(1L);

        when(userFavoriteMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(favorite));

        Page<Poem> page = new Page<>(1, 10);
        Poem poem = new Poem();
        poem.setId(1L);
        poem.setTitle("静夜思");
        page.setRecords(Collections.singletonList(poem));
        page.setTotal(1);

        when(poemMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        IPage<Poem> result = poemService.getFavorites(1L, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void getFavorites_无收藏时返回空() {
        when(userFavoriteMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        IPage<Poem> result = poemService.getFavorites(1L, 1, 10);

        assertNotNull(result);
        assertEquals(0, result.getRecords().size());
    }

    @Test
    void createPoemWithAudit_设置待审核状态() {
        Poem poem = new Poem();
        poem.setTitle("新诗");

        poemService.createPoemWithAudit(poem);

        assertEquals(0, poem.getAuditStatus());
        assertEquals(0, poem.getStatus());
        verify(poemMapper).insert(poem);
    }

    @Test
    void auditPoem_审核通过() {
        Poem poem = new Poem();
        poem.setId(1L);
        poem.setStatus(0);

        when(poemMapper.selectById(1L)).thenReturn(poem);

        poemService.auditPoem(1L, 1, "审核通过");

        assertEquals(1, poem.getAuditStatus());
        assertEquals(1, poem.getStatus());
        assertEquals("审核通过", poem.getAuditReason());
    }

    @Test
    void auditPoem_审核拒绝() {
        Poem poem = new Poem();
        poem.setId(1L);
        poem.setStatus(0);

        when(poemMapper.selectById(1L)).thenReturn(poem);

        poemService.auditPoem(1L, 2, "内容不当");

        assertEquals(2, poem.getAuditStatus());
        assertEquals(0, poem.getStatus());
        assertEquals("内容不当", poem.getAuditReason());
    }

    @Test
    void auditPoem_诗词不存在抛出异常() {
        when(poemMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            poemService.auditPoem(999L, 1, "审核通过");
        });
    }
}
