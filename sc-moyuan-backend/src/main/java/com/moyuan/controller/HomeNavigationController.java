package com.moyuan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.common.R;
import com.moyuan.entity.HomeNavigation;
import com.moyuan.mapper.HomeNavigationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "首页导航接口")
@RestController
@RequestMapping("/api/home-navigation")
@RequiredArgsConstructor
public class HomeNavigationController {

    private final HomeNavigationMapper homeNavigationMapper;

    @Operation(summary = "获取首页导航列表")
    @GetMapping
    public R<List<HomeNavigation>> list(@RequestParam(required = false) String type) {
        LambdaQueryWrapper<HomeNavigation> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) {
            wrapper.eq(HomeNavigation::getType, type);
        }
        wrapper.eq(HomeNavigation::getStatus, 1)
               .orderByAsc(HomeNavigation::getSortOrder);
        return R.success(homeNavigationMapper.selectList(wrapper));
    }
}
