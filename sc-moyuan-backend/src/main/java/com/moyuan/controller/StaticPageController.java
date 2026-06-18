package com.moyuan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.common.R;
import com.moyuan.entity.StaticPage;
import com.moyuan.service.StaticPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "静态页面接口")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StaticPageController {

    private final StaticPageService staticPageService;

    // ========== 公开接口（前台页面使用） ==========

    @Operation(summary = "根据pageKey获取静态页面内容")
    @GetMapping("/static-pages/{pageKey}")
    public R<StaticPage> getByPageKey(@PathVariable String pageKey) {
        LambdaQueryWrapper<StaticPage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StaticPage::getPageKey, pageKey)
               .eq(StaticPage::getStatus, 1);
        StaticPage page = staticPageService.getOne(wrapper);
        return R.success(page);
    }

    // ========== 管理接口（后台管理使用） ==========

    @Operation(summary = "获取静态页面列表（管理端）")
    @GetMapping("/admin/static-pages")
    public R<List<StaticPage>> list() {
        LambdaQueryWrapper<StaticPage> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(StaticPage::getId);
        return R.success(staticPageService.list(wrapper));
    }

    @Operation(summary = "根据ID获取静态页面详情（管理端）")
    @GetMapping("/admin/static-pages/{id}")
    public R<StaticPage> getById(@PathVariable Long id) {
        return R.success(staticPageService.getById(id));
    }

    @Operation(summary = "更新静态页面内容（管理端）")
    @PutMapping("/admin/static-pages/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody StaticPage staticPage) {
        staticPage.setId(id);
        staticPageService.updateById(staticPage);
        return R.success();
    }
}
