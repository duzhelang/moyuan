package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.dto.request.ForumPostCreateRequest;
import com.moyuan.dto.request.ForumPostUpdateRequest;
import com.moyuan.entity.ForumPost;

public interface ForumPostService extends IService<ForumPost> {
    IPage<ForumPost> getPostList(int pageNum, int pageSize, String category, String keyword);
    ForumPost getPostDetail(Long id);
    ForumPost createPost(Long userId, ForumPostCreateRequest request);
    ForumPost updatePost(Long userId, Long postId, ForumPostUpdateRequest request);
    void deletePost(Long userId, Long postId);
    void toggleLike(Long userId, Long postId);
    boolean isLike(Long userId, Long postId);
    IPage<ForumPost> getPostsByUserId(Long userId, int pageNum, int pageSize);
    void auditForumPost(Long postId, Integer status, String reason);
}
