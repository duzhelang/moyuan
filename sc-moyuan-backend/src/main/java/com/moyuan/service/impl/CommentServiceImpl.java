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
import com.moyuan.entity.User;
import com.moyuan.entity.UserLike;
import com.moyuan.enums.TargetType;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.CommentMapper;
import com.moyuan.mapper.ForumPostMapper;
import com.moyuan.mapper.UserLikeMapper;
import com.moyuan.mapper.UserMapper;
import com.moyuan.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final CommentMapper commentMapper;
    private final ForumPostMapper forumPostMapper;
    private final UserLikeMapper userLikeMapper;
    private final UserMapper userMapper;

    private void fillUserInfo(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) return;
        List<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        for (Comment comment : comments) {
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                comment.setUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
                comment.setAvatar(user.getAvatar());
            }
        }
    }

    @Override
    public IPage<Comment> getCommentsByTarget(Long targetId, Integer targetType, int pageNum, int pageSize) {
        IPage<Comment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getTargetId, targetId)
                        .eq(Comment::getTargetType, targetType)
                        .eq(Comment::getStatus, 1)
                        .eq(Comment::getParentId, 0)
                        .orderByDesc(Comment::getCreateTime));
        fillUserInfo(page.getRecords());
        return page;
    }

    @Override
    @Transactional
    public Comment createComment(Long userId, CommentCreateRequest request) {
        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setUserId(userId);
        comment.setTargetId(request.getTargetId());
        comment.setTargetType(request.getTargetType());
        comment.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        comment.setReplyToUserId(request.getReplyToUserId());
        comment.setLikeCount(0);
        comment.setStatus(1);
        commentMapper.insert(comment);

        if (request.getTargetType() == 2) {
            forumPostMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                    .eq(ForumPost::getId, request.getTargetId())
                    .setSql("comment_count = comment_count + 1")
                    .set(ForumPost::getLastCommentTime, LocalDateTime.now()));
        }

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

        if (comment.getTargetType() == 2) {
            forumPostMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                    .eq(ForumPost::getId, comment.getTargetId())
                    .gt(ForumPost::getCommentCount, 0)
                    .setSql("comment_count = comment_count - 1"));
        }
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

    @Override
    public IPage<Comment> getCommentList(int pageNum, int pageSize, Integer status) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Comment::getStatus, status);
        }
        wrapper.orderByDesc(Comment::getCreateTime);
        IPage<Comment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillUserInfo(page.getRecords());
        return page;
    }

    @Override
    @Transactional
    public void auditComment(Long commentId, Integer status, String reason) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        comment.setStatus(status);
        comment.setAuditReason(reason);
        comment.setAuditTime(LocalDateTime.now());
        commentMapper.updateById(comment);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        commentMapper.deleteById(commentId);

        if (comment.getTargetType() == 2) {
            forumPostMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                    .eq(ForumPost::getId, comment.getTargetId())
                    .gt(ForumPost::getCommentCount, 0)
                    .setSql("comment_count = comment_count - 1"));
        }
    }
}
