package com.moyuan.controller;

import com.moyuan.entity.Dynasty;
import com.moyuan.service.DynastyService;
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

@WebMvcTest(DynastyController.class)
class DynastyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DynastyService dynastyService;

    @Test
    void getAllDynasties_返回朝代列表() throws Exception {
        Dynasty dynasty1 = new Dynasty();
        dynasty1.setId(1L);
        dynasty1.setName("唐");
        dynasty1.setDescription("唐朝");

        Dynasty dynasty2 = new Dynasty();
        dynasty2.setId(2L);
        dynasty2.setName("宋");
        dynasty2.setDescription("宋朝");

        List<Dynasty> dynasties = Arrays.asList(dynasty1, dynasty2);

        when(dynastyService.getAllDynasties()).thenReturn(dynasties);

        mockMvc.perform(get("/api/dynasties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("唐"))
                .andExpect(jsonPath("$.data[0].description").value("唐朝"))
                .andExpect(jsonPath("$.data[1].name").value("宋"))
                .andExpect(jsonPath("$.data[1].description").value("宋朝"));
    }

    @Test
    void getDynastyDetail_返回朝代详情() throws Exception {
        Dynasty dynasty = new Dynasty();
        dynasty.setId(1L);
        dynasty.setName("唐");
        dynasty.setDescription("唐朝是中国历史上最繁荣的朝代之一");

        when(dynastyService.getDynastyDetail(1L)).thenReturn(dynasty);

        mockMvc.perform(get("/api/dynasties/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("唐"))
                .andExpect(jsonPath("$.data.description").value("唐朝是中国历史上最繁荣的朝代之一"));
    }

    @Test
    void getAllDynasties_空列表返回空数组() throws Exception {
        when(dynastyService.getAllDynasties()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/dynasties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
