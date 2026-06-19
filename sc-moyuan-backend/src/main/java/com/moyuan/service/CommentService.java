package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.dto.request.CommentCreateRequest;
import com.moyuan.entity.Comment;

public interface CommentService extends IService<Comment> {
    IPage<Comment> getCommentsByTarget(Long targetId, Integer targetType, int pageNum, int pageSize);
    Comment createComment(Long userId, CommentCreateRequest request);
    void deleteComment(Long userId, Long commentId);
    void toggleLike(Long userId, Long commentId);

    IPage<Comment> getCommentList(int pageNum, int pageSize, Integer status);
    void auditComment(Long commentId, Integer status, String reason);
    void deleteCommentByAdmin(Long commentId);
}
