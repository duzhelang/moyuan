package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.dto.request.CommentCreateRequest;
import com.moyuan.dto.request.ForumPostCreateRequest;
import com.moyuan.dto.request.ForumPostUpdateRequest;
import com.moyuan.entity.Comment;
import com.moyuan.entity.ForumPost;
import com.moyuan.service.CommentService;
import com.moyuan.service.ForumPostService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "论坛接口")
@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumPostService forumPostService;
    private final CommentService commentService;
    private final SecurityUtil securityUtil;

    @Operation(summary = "获取帖子列表")
    @GetMapping("/posts")
    public R<Map<String, Object>> getPostList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        IPage<ForumPost> page = forumPostService.getPostList(pageNum, pageSize, category, keyword);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return R.success(result);
    }

    @Operation(summary = "获取帖子详情")
    @GetMapping("/posts/{id}")
    public R<ForumPost> getPostDetail(@PathVariable Long id) {
        return R.success(forumPostService.getPostDetail(id));
    }

    @Operation(summary = "创建帖子")
    @PostMapping("/posts")
    public R<ForumPost> createPost(@Valid @RequestBody ForumPostCreateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        return R.success(forumPostService.createPost(userId, request));
    }

    @Operation(summary = "更新帖子")
    @PutMapping("/posts/{id}")
    public R<ForumPost> updatePost(@PathVariable Long id, @Valid @RequestBody ForumPostUpdateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        return R.success(forumPostService.updatePost(userId, id, request));
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/posts/{id}")
    public R<Void> deletePost(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        forumPostService.deletePost(userId, id);
        return R.success();
    }

    @Operation(summary = "点赞/取消点赞帖子")
    @PostMapping("/posts/{id}/like")
    public R<Void> togglePostLike(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        forumPostService.toggleLike(userId, id);
        return R.success();
    }

    @Operation(summary = "检查是否点赞帖子")
    @GetMapping("/posts/{id}/like")
    public R<Map<String, Boolean>> isPostLike(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        Map<String, Boolean> result = new HashMap<>();
        result.put("liked", forumPostService.isLike(userId, id));
        return R.success(result);
    }

    @Operation(summary = "获取评论列表")
    @GetMapping("/comments")
    public R<Map<String, Object>> getComments(
            @RequestParam Long targetId,
            @RequestParam Integer targetType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        IPage<Comment> page = commentService.getCommentsByTarget(targetId, targetType, pageNum, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return R.success(result);
    }

    @Operation(summary = "创建评论")
    @PostMapping("/comments")
    public R<Comment> createComment(@Valid @RequestBody CommentCreateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        return R.success(commentService.createComment(userId, request));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/comments/{id}")
    public R<Void> deleteComment(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        commentService.deleteComment(userId, id);
        return R.success();
    }

    @Operation(summary = "点赞/取消点赞评论")
    @PostMapping("/comments/{id}/like")
    public R<Void> toggleCommentLike(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        commentService.toggleLike(userId, id);
        return R.success();
    }
}
