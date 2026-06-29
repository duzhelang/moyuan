package com.moyuan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.entity.Dynasty;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.DynastyMapper;
import com.moyuan.service.impl.DynastyServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynastyServiceImplTest {

    @Mock
    private DynastyMapper dynastyMapper;

    @InjectMocks
    private DynastyServiceImpl dynastyService;

    @Test
    void getAllDynasties_返回排序后的朝代列表() {
        Dynasty dynasty1 = new Dynasty();
        dynasty1.setId(1L);
        dynasty1.setName("唐");
        dynasty1.setSortOrder(1);

        Dynasty dynasty2 = new Dynasty();
        dynasty2.setId(2L);
        dynasty2.setName("宋");
        dynasty2.setSortOrder(2);

        List<Dynasty> expectedDynasties = Arrays.asList(dynasty1, dynasty2);

        when(dynastyMapper.selectList(any())).thenReturn(expectedDynasties);

        List<Dynasty> actualDynasties = dynastyService.getAllDynasties();

        assertNotNull(actualDynasties);
        assertEquals(2, actualDynasties.size());
        assertEquals("唐", actualDynasties.get(0).getName());
        assertEquals("宋", actualDynasties.get(1).getName());
    }

    @Test
    void getAllDynasties_空列表返回空() {
        when(dynastyMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Dynasty> actualDynasties = dynastyService.getAllDynasties();

        assertNotNull(actualDynasties);
        assertEquals(0, actualDynasties.size());
    }

    @Test
    void getDynastyDetail_存在时返回朝代() {
        Dynasty dynasty = new Dynasty();
        dynasty.setId(1L);
        dynasty.setName("唐");
        dynasty.setDescription("唐朝");

        when(dynastyMapper.selectById(1L)).thenReturn(dynasty);

        Dynasty actualDynasty = dynastyService.getDynastyDetail(1L);

        assertNotNull(actualDynasty);
        assertEquals(1L, actualDynasty.getId());
        assertEquals("唐", actualDynasty.getName());
        assertEquals("唐朝", actualDynasty.getDescription());
    }

    @Test
    void getDynastyDetail_不存在时抛出异常() {
        when(dynastyMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            dynastyService.getDynastyDetail(999L);
        });
    }
}
