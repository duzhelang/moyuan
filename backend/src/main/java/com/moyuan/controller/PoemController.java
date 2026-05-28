package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyuan.common.Result;
import com.moyuan.entity.Poem;
import com.moyuan.service.PoemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/poems")
public class PoemController {

    @Autowired
    private PoemService poemService;

    @GetMapping("/modern")
    public Result<List<Poem>> getModernPoems() {
        List<Poem> poems = poemService.getModernPoems();
        return Result.success(poems);
    }

    @GetMapping("/modern/page")
    public Result<IPage<Poem>> getModernPoemsPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Poem> page = new Page<>(pageNum, pageSize);
        IPage<Poem> poems = poemService.page(page);
        return Result.success(poems);
    }

    @GetMapping("/category/{category}")
    public Result<List<Poem>> getPoemsByCategory(@PathVariable String category) {
        List<Poem> poems = poemService.getPoemsByCategory(category);
        return Result.success(poems);
    }

    @GetMapping("/featured")
    public Result<List<Poem>> getFeaturedPoems() {
        List<Poem> poems = poemService.getFeaturedPoems();
        return Result.success(poems);
    }

    @GetMapping("/{id}")
    public Result<Poem> getPoemById(@PathVariable Long id) {
        Poem poem = poemService.getById(id);
        if (poem == null) {
            return Result.error("诗词不存在");
        }
        return Result.success(poem);
    }

    @PostMapping
    public Result<Poem> createPoem(@RequestBody Poem poem) {
        poemService.save(poem);
        return Result.success("诗词创建成功", poem);
    }

    @PutMapping("/{id}")
    public Result<Poem> updatePoem(@PathVariable Long id, @RequestBody Poem poem) {
        poem.setId(id);
        poemService.updateById(poem);
        return Result.success("诗词更新成功", poem);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePoem(@PathVariable Long id) {
        poemService.removeById(id);
        return Result.success("诗词删除成功", null);
    }

    @GetMapping("/search")
    public Result<List<Poem>> searchPoems(@RequestParam String keyword) {
        List<Poem> poems = poemService.getPoemsByCategory(keyword);
        return Result.success(poems);
    }
}
