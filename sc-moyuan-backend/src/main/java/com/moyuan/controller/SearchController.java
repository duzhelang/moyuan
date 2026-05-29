package com.moyuan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.common.R;
import com.moyuan.entity.ForumPost;
import com.moyuan.entity.Poem;
import com.moyuan.entity.Poet;
import com.moyuan.mapper.PoemMapper;
import com.moyuan.mapper.PoetMapper;
import com.moyuan.service.ForumPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "搜索接口")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final PoemMapper poemMapper;
    private final PoetMapper poetMapper;
    private final ForumPostService forumPostService;

    @Operation(summary = "全局搜索")
    @GetMapping
    public R<Map<String, Object>> search(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<Poem> poemWrapper = new LambdaQueryWrapper<>();
        poemWrapper.eq(Poem::getStatus, 1)
                .and(w -> w.like(Poem::getTitle, keyword).or().like(Poem::getContent, keyword))
                .last("LIMIT 10");
        result.put("poems", poemMapper.selectList(poemWrapper));

        LambdaQueryWrapper<Poet> poetWrapper = new LambdaQueryWrapper<>();
        poetWrapper.eq(Poet::getStatus, 1)
                .like(Poet::getName, keyword)
                .last("LIMIT 10");
        result.put("poets", poetMapper.selectList(poetWrapper));

        LambdaQueryWrapper<ForumPost> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.eq(ForumPost::getStatus, 1)
                .and(w -> w.like(ForumPost::getTitle, keyword).or().like(ForumPost::getContent, keyword))
                .last("LIMIT 10");
        result.put("posts", forumPostService.list(postWrapper));

        return R.success(result);
    }
}
