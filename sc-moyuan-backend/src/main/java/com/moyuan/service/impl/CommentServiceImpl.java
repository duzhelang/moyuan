package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.common.ResultCode;
import com.moyuan.dto.request.CommentCreateRequest;
import com.moyuan.entity.Comment;
import com.moyuan.entity.ForumPost;
import com.moyuan.entity.UserLike;
import com.moyuan.enums.TargetType;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.CommentMapper;
import com.moyuan.mapper.ForumPostMapper;
import com.moyuan.mapper.UserLikeMapper;
import com.moyuan.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final CommentMapper commentMapper;
    private final ForumPostMapper forumPostMapper;
    private final UserLikeMapper userLikeMapper;

    @Override
    public IPage<Comment> getCommentsByPostId(Long postId, int pageNum, int pageSize) {
        return commentMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .eq(Comment::getStatus, 1)
                        .eq(Comment::getParentId, 0)
                        .orderByDesc(Comment::getCreateTime));
    }

    @Override
    @Transactional
    public Comment createComment(Long userId, CommentCreateRequest request) {
        ForumPost post = forumPostMapper.selectById(request.getPostId());
        if (post == null || post.getStatus() != 1) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setUserId(userId);
        comment.setPostId(request.getPostId());
        comment.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        comment.setReplyToUserId(request.getReplyToUserId());
        comment.setLikeCount(0);
        comment.setStatus(1);
        commentMapper.insert(comment);

        forumPostMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                .eq(ForumPost::getId, request.getPostId())
                .setSql("comment_count = comment_count + 1")
                .set(ForumPost::getLastCommentTime, LocalDateTime.now()));

        return comment;
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || !comment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        commentMapper.deleteById(commentId);

        forumPostMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                .eq(ForumPost::getId, comment.getPostId())
                .gt(ForumPost::getCommentCount, 0)
                .setSql("comment_count = comment_count - 1"));
    }

    @Override
    @Transactional
    public void toggleLike(Long userId, Long commentId) {
        try {
            UserLike like = new UserLike();
            like.setUserId(userId);
            like.setTargetId(commentId);
            like.setTargetType(TargetType.COMMENT.getCode());
            userLikeMapper.insert(like);
            commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId, commentId)
                    .setSql("like_count = like_count + 1"));
        } catch (DuplicateKeyException e) {
            userLikeMapper.delete(new LambdaQueryWrapper<UserLike>()
                    .eq(UserLike::getUserId, userId)
                    .eq(UserLike::getTargetId, commentId)
                    .eq(UserLike::getTargetType, TargetType.COMMENT.getCode()));
            commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getId, commentId)
                    .gt(Comment::getLikeCount, 0)
                    .setSql("like_count = like_count - 1"));
        }
    }
}
