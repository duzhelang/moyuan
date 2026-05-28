package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.entity.Dynasty;
import com.moyuan.service.DynastyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "朝代接口")
@RestController
@RequestMapping("/api/dynasties")
@RequiredArgsConstructor
public class DynastyController {

    private final DynastyService dynastyService;

    @Operation(summary = "获取所有朝代列表")
    @GetMapping
    public R<List<Dynasty>> getAllDynasties() {
        return R.success(dynastyService.getAllDynasties());
    }

    @Operation(summary = "获取朝代详情")
    @GetMapping("/{id}")
    public R<Dynasty> getDynastyDetail(@PathVariable Long id) {
        return R.success(dynastyService.getDynastyDetail(id));
    }
}
