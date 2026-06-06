package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.entity.Rhyme;
import com.moyuan.service.RhymeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "韵脚查询接口")
@RestController
@RequestMapping("/api/rhyme")
@RequiredArgsConstructor
public class RhymeController {

    private final RhymeService rhymeService;

    @Operation(summary = "按汉字查询韵脚")
    @GetMapping("/query")
    public R<List<Rhyme>> queryByCharacter(@RequestParam String character) {
        if (character == null || character.trim().isEmpty()) {
            return R.error("400", "请输入要查询的汉字");
        }
        String ch = character.trim().substring(0, 1);
        List<Rhyme> results = rhymeService.queryByCharacter(ch);
        return R.success(results);
    }

    @Operation(summary = "按韵部查询同韵字")
    @GetMapping("/group")
    public R<List<Rhyme>> queryByRhymeGroup(@RequestParam String group) {
        if (group == null || group.trim().isEmpty()) {
            return R.error("400", "请输入韵部名称");
        }
        List<Rhyme> results = rhymeService.queryByRhymeGroup(group.trim());
        return R.success(results);
    }
}
