package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.common.ResultCode;
import com.moyuan.entity.Poem;
import com.moyuan.entity.Poet;
import com.moyuan.entity.Dynasty;
import com.moyuan.entity.UserFavorite;
import com.moyuan.entity.UserLike;
import com.moyuan.enums.TargetType;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.PoemMapper;
import com.moyuan.mapper.PoetMapper;
import com.moyuan.mapper.DynastyMapper;
import com.moyuan.mapper.UserFavoriteMapper;
import com.moyuan.mapper.UserLikeMapper;
import com.moyuan.service.PoemService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PoemServiceImpl extends ServiceImpl<PoemMapper, Poem> implements PoemService {

    private final PoemMapper poemMapper;
    private final PoetMapper poetMapper;
    private final DynastyMapper dynastyMapper;
    private final UserLikeMapper userLikeMapper;
    private final UserFavoriteMapper userFavoriteMapper;

    @Override
    @Cacheable(value = "poems", key = "'list:' + #dynastyId + ':' + #poetId + ':' + #categoryId + ':' + #keyword + ':' + #pageNum + ':' + #pageSize")
    public IPage<Poem> getPoemList(int pageNum, int pageSize, Long dynastyId, Long poetId, Long categoryId, String keyword) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, 1);
        if (dynastyId != null) wrapper.eq(Poem::getDynastyId, dynastyId);
        if (poetId != null) wrapper.eq(Poem::getPoetId, poetId);
        if (categoryId != null) wrapper.eq(Poem::getCategoryId, categoryId);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Poem::getTitle, keyword).or().like(Poem::getContent, keyword));
        }
        wrapper.orderByDesc(Poem::getCreateTime);
        return poemMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Poem getPoemDetail(Long id) {
        Poem poem = poemMapper.selectById(id);
        if (poem == null || poem.getStatus() != 1) {
            throw new BusinessException(ResultCode.POEM_NOT_FOUND);
        }
        poemMapper.update(null, new LambdaUpdateWrapper<Poem>()
                .eq(Poem::getId, id)
                .setSql("view_count = view_count + 1"));
        poem.setViewCount(poem.getViewCount() + 1);
        return poem;
    }

    @Override
    @Transactional
    public void toggleLike(Long userId, Long poemId) {
        try {
            UserLike like = new UserLike();
            like.setUserId(userId);
            like.setTargetId(poemId);
            like.setTargetType(TargetType.POEM.getCode());
            userLikeMapper.insert(like);
            poemMapper.update(null, new LambdaUpdateWrapper<Poem>()
                    .eq(Poem::getId, poemId)
                    .setSql("like_count = like_count + 1"));
        } catch (DuplicateKeyException e) {
            userLikeMapper.delete(new LambdaQueryWrapper<UserLike>()
                    .eq(UserLike::getUserId, userId)
                    .eq(UserLike::getTargetId, poemId)
                    .eq(UserLike::getTargetType, TargetType.POEM.getCode()));
            poemMapper.update(null, new LambdaUpdateWrapper<Poem>()
                    .eq(Poem::getId, poemId)
                    .gt(Poem::getLikeCount, 0)
                    .setSql("like_count = like_count - 1"));
        }
    }

    @Override
    public boolean isLike(Long userId, Long poemId) {
        return userLikeMapper.selectCount(
                new LambdaQueryWrapper<UserLike>()
                        .eq(UserLike::getUserId, userId)
                        .eq(UserLike::getTargetId, poemId)
                        .eq(UserLike::getTargetType, TargetType.POEM.getCode())) > 0;
    }

    @Override
    public IPage<Poem> getFavorites(Long userId, int pageNum, int pageSize) {
        List<Long> poemIds = userFavoriteMapper.selectList(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getTargetType, TargetType.POEM.getCode()))
                .stream().map(UserFavorite::getTargetId).toList();
        if (poemIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }
        IPage<Poem> page = poemMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Poem>().in(Poem::getId, poemIds).eq(Poem::getStatus, 1));
        populatePoetAndDynasty(page.getRecords());
        return page;
    }

    private void populatePoetAndDynasty(List<Poem> poems) {
        for (Poem poem : poems) {
            if (poem.getPoetId() != null) {
                Poet poet = poetMapper.selectById(poem.getPoetId());
                if (poet != null) {
                    poem.setPoetName(poet.getName());
                }
            }
            if (poem.getDynastyId() != null) {
                Dynasty dynasty = dynastyMapper.selectById(poem.getDynastyId());
                if (dynasty != null) {
                    poem.setDynastyName(dynasty.getName());
                }
            }
        }
    }

    @Override
    @Transactional
    public void createPoemWithAudit(Poem poem) {
        poem.setAuditStatus(0);
        poem.setStatus(0);
        save(poem);
    }

    @Override
    @Transactional
    public void auditPoem(Long poemId, Integer status, String reason) {
        Poem poem = getById(poemId);
        if (poem == null) {
            throw new BusinessException(ResultCode.POEM_NOT_FOUND);
        }
        poem.setAuditStatus(status);
        poem.setAuditReason(reason);
        poem.setAuditTime(java.time.LocalDateTime.now());
        if (status == 1) {
            poem.setStatus(1);
        }
        updateById(poem);
    }
}
