package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.common.ResultCode;
import com.moyuan.dto.request.ForumPostCreateRequest;
import com.moyuan.dto.request.ForumPostUpdateRequest;
import com.moyuan.entity.ForumPost;
import com.moyuan.entity.UserLike;
import com.moyuan.enums.TargetType;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.ForumPostMapper;
import com.moyuan.mapper.UserLikeMapper;
import com.moyuan.service.ForumPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ForumPostServiceImpl extends ServiceImpl<ForumPostMapper, ForumPost> implements ForumPostService {

    private final ForumPostMapper forumPostMapper;
    private final UserLikeMapper userLikeMapper;

    @Override
    public IPage<ForumPost> getPostList(int pageNum, int pageSize, String category, String keyword) {
        LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumPost::getStatus, 1);
        if (StringUtils.hasText(category)) wrapper.eq(ForumPost::getCategory, category);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ForumPost::getTitle, keyword).or().like(ForumPost::getContent, keyword));
        }
        wrapper.orderByDesc(ForumPost::getIsTop).orderByDesc(ForumPost::getCreateTime);
        return forumPostMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public ForumPost getPostDetail(Long id) {
        ForumPost post = forumPostMapper.selectById(id);
        if (post == null || post.getStatus() != 1) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        forumPostMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                .eq(ForumPost::getId, id)
                .setSql("view_count = view_count + 1"));
        post.setViewCount(post.getViewCount() + 1);
        return post;
    }

    @Override
    @Transactional
    public ForumPost createPost(Long userId, ForumPostCreateRequest request) {
        ForumPost post = new ForumPost();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setUserId(userId);
        post.setCategory(request.getCategory());
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setIsTop(0);
        post.setIsFeatured(0);
        post.setStatus(1);
        forumPostMapper.insert(post);
        return post;
    }

    @Override
    @Transactional
    public ForumPost updatePost(Long userId, Long postId, ForumPostUpdateRequest request) {
        ForumPost post = forumPostMapper.selectById(postId);
        if (post == null || !post.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getContent() != null) post.setContent(request.getContent());
        if (request.getCategory() != null) post.setCategory(request.getCategory());
        forumPostMapper.updateById(post);
        return post;
    }

    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        ForumPost post = forumPostMapper.selectById(postId);
        if (post == null || !post.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        forumPostMapper.deleteById(postId);
    }

    @Override
    @Transactional
    public void toggleLike(Long userId, Long postId) {
        try {
            UserLike like = new UserLike();
            like.setUserId(userId);
            like.setTargetId(postId);
            like.setTargetType(TargetType.POST.getCode());
            userLikeMapper.insert(like);
            forumPostMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                    .eq(ForumPost::getId, postId)
                    .setSql("like_count = like_count + 1"));
        } catch (DuplicateKeyException e) {
            userLikeMapper.delete(new LambdaQueryWrapper<UserLike>()
                    .eq(UserLike::getUserId, userId)
                    .eq(UserLike::getTargetId, postId)
                    .eq(UserLike::getTargetType, TargetType.POST.getCode()));
            forumPostMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                    .eq(ForumPost::getId, postId)
                    .gt(ForumPost::getLikeCount, 0)
                    .setSql("like_count = like_count - 1"));
        }
    }

    @Override
    public boolean isLike(Long userId, Long postId) {
        return userLikeMapper.selectCount(
                new LambdaQueryWrapper<UserLike>()
                        .eq(UserLike::getUserId, userId)
                        .eq(UserLike::getTargetId, postId)
                        .eq(UserLike::getTargetType, TargetType.POST.getCode())) > 0;
    }

    @Override
    public IPage<ForumPost> getPostsByUserId(Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumPost::getUserId, userId)
               .orderByDesc(ForumPost::getCreateTime);
        return forumPostMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }
}
