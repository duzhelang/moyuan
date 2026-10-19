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
import com.moyuan.entity.Category;
import com.moyuan.entity.UserFavorite;
import com.moyuan.entity.UserLike;
import com.moyuan.enums.TargetType;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.PoemMapper;
import com.moyuan.mapper.PoetMapper;
import com.moyuan.mapper.DynastyMapper;
import com.moyuan.mapper.CategoryMapper;
import com.moyuan.mapper.UserFavoriteMapper;
import com.moyuan.mapper.UserLikeMapper;
import com.moyuan.service.PoemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoemServiceImpl extends ServiceImpl<PoemMapper, Poem> implements PoemService {

    private final PoemMapper poemMapper;
    private final PoetMapper poetMapper;
    private final DynastyMapper dynastyMapper;
    private final CategoryMapper categoryMapper;
    private final UserLikeMapper userLikeMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final CacheManager cacheManager;

    @Override
    @Cacheable(value = "poems", key = "'list:' + #dynastyId + ':' + #poetId + ':' + #categoryId + ':' + #keyword + ':' + #pageNum + ':' + #pageSize")
    public IPage<Poem> getPoemList(int pageNum, int pageSize, Long dynastyId, Long poetId, Long categoryId, String keyword) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, 1);
        // 外文诗（poem_type='foreign'）不进入古籍诗词列表
        wrapper.ne(Poem::getPoemType, "foreign");
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
    public IPage<Poem> getAdminPoemList(int pageNum, int pageSize, String keyword, Integer auditStatus) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        // 外文诗不在后台诗词列表中展示
        wrapper.ne(Poem::getPoemType, "foreign");
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Poem::getTitle, keyword).or()
                    .like(Poem::getContent, keyword));
        }
        if (auditStatus != null) {
            wrapper.eq(Poem::getAuditStatus, auditStatus);
        }
        wrapper.orderByDesc(Poem::getCreateTime);
        IPage<Poem> page = poemMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        populatePoetAndDynasty(page.getRecords());
        return page;
    }

    @Override
    public IPage<Poem> getModernPoems(int pageNum, int pageSize, Boolean isOriginal, Boolean hasCertifiedPoet, String sortBy, String keyword) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, 1);
        wrapper.eq(Poem::getDynastyId, 13);
        // 现代创作页签仅展示现代诗，排除古籍与外文诗
        wrapper.eq(Poem::getPoemType, "modern");
        // 可选关键字：按标题或正文模糊匹配（首页时代画像据此解析已入库现代诗的真实 id）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Poem::getTitle, keyword).or().like(Poem::getContent, keyword));
        }
        if (isOriginal != null) {
            wrapper.eq(Poem::getIsOriginal, isOriginal ? 1 : 0);
        }
        if (hasCertifiedPoet != null && hasCertifiedPoet) {
            wrapper.isNotNull(Poem::getPoetId);
        }
        if ("popular".equals(sortBy)) {
            wrapper.orderByDesc(Poem::getViewCount);
        } else if ("likes".equals(sortBy)) {
            wrapper.orderByDesc(Poem::getLikeCount);
        } else {
            wrapper.orderByDesc(Poem::getCreateTime);
        }
        IPage<Poem> page = poemMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        populatePoetAndDynasty(page.getRecords());
        return page;
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
        // 回填诗人姓名/头像与朝代名称，供详情页「诗人简介」展示
        if (poem.getPoetId() != null) {
            Poet poet = poetMapper.selectById(poem.getPoetId());
            if (poet != null) {
                poem.setPoetName(poet.getName());
                poem.setPoetAvatar(poet.getAvatar());
            }
        }
        if (poem.getDynastyId() != null) {
            Dynasty dynasty = dynastyMapper.selectById(poem.getDynastyId());
            if (dynasty != null) {
                poem.setDynastyName(dynasty.getName());
            }
        }
        return poem;
    }

    @Override
    @Transactional
    public void toggleLike(Long userId, Long poemId) {
        boolean exists = userLikeMapper.selectCount(
                new LambdaQueryWrapper<UserLike>()
                        .eq(UserLike::getUserId, userId)
                        .eq(UserLike::getTargetId, poemId)
                        .eq(UserLike::getTargetType, TargetType.POEM.getCode())) > 0;

        if (exists) {
            // 取消点赞
            userLikeMapper.delete(new LambdaQueryWrapper<UserLike>()
                    .eq(UserLike::getUserId, userId)
                    .eq(UserLike::getTargetId, poemId)
                    .eq(UserLike::getTargetType, TargetType.POEM.getCode()));
            poemMapper.update(null, new LambdaUpdateWrapper<Poem>()
                    .eq(Poem::getId, poemId)
                    .gt(Poem::getLikeCount, 0)
                    .setSql("like_count = like_count - 1"));
        } else {
            // 点赞
            UserLike like = new UserLike();
            like.setUserId(userId);
            like.setTargetId(poemId);
            like.setTargetType(TargetType.POEM.getCode());
            userLikeMapper.insert(like);
            poemMapper.update(null, new LambdaUpdateWrapper<Poem>()
                    .eq(Poem::getId, poemId)
                    .setSql("like_count = like_count + 1"));
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
                new LambdaQueryWrapper<Poem>().in(Poem::getId, poemIds).eq(Poem::getStatus, 1)
                        .ne(Poem::getPoemType, "foreign"));
        populatePoetAndDynasty(page.getRecords());
        return page;
    }

    @Override
    public List<Poem> listForeignPoems(String keyword, String author, int limit) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getPoemType, "foreign");
        wrapper.eq(Poem::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Poem::getTitle, keyword);
        }
        if (StringUtils.hasText(author)) {
            wrapper.like(Poem::getAuthor, author);
        }
        wrapper.orderByAsc(Poem::getId);
        wrapper.last("LIMIT " + Math.max(1, limit));
        return poemMapper.selectList(wrapper);
    }

    private void populatePoetAndDynasty(List<Poem> poems) {
        if (poems == null || poems.isEmpty()) {
            return;
        }

        // 收集所有需要查询的ID
        Set<Long> poetIds = poems.stream()
                .map(Poem::getPoetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> dynastyIds = poems.stream()
                .map(Poem::getDynastyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> categoryIds = poems.stream()
                .map(Poem::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 批量查询诗人、朝代和分类
        Map<Long, String> poetNameMap = new HashMap<>();
        Map<Long, String> dynastyNameMap = new HashMap<>();
        Map<Long, String> categoryNameMap = new HashMap<>();

        if (!poetIds.isEmpty()) {
            poetMapper.selectBatchIds(poetIds).forEach(
                    poet -> poetNameMap.put(poet.getId(), poet.getName()));
        }
        if (!dynastyIds.isEmpty()) {
            dynastyMapper.selectBatchIds(dynastyIds).forEach(
                    dynasty -> dynastyNameMap.put(dynasty.getId(), dynasty.getName()));
        }
        if (!categoryIds.isEmpty()) {
            categoryMapper.selectBatchIds(categoryIds).forEach(
                    category -> categoryNameMap.put(category.getId(), category.getName()));
        }

        // 填充数据
        for (Poem poem : poems) {
            if (poem.getPoetId() != null) {
                poem.setPoetName(poetNameMap.get(poem.getPoetId()));
            }
            if (poem.getDynastyId() != null) {
                poem.setDynastyName(dynastyNameMap.get(poem.getDynastyId()));
            }
            if (poem.getCategoryId() != null) {
                poem.setCategoryName(categoryNameMap.get(poem.getCategoryId()));
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
            // 审核通过公开后，确保其诗人可见（静默收录可能将诗人建为隐藏）
            activatePoetIfHidden(poem.getPoetId());
        }
        updateById(poem);
    }

    /** 诗人若为隐藏状态（status!=1）则置为可见，并清理诗人缓存 */
    private void activatePoetIfHidden(Long poetId) {
        if (poetId == null) {
            return;
        }
        Poet poet = poetMapper.selectById(poetId);
        if (poet != null && !Integer.valueOf(1).equals(poet.getStatus())) {
            poet.setStatus(1);
            poetMapper.updateById(poet);
            if (cacheManager.getCache("poets") != null) {
                cacheManager.getCache("poets").clear();
            }
        }
    }

    @Override
    public void clearAllCache() {
        if (cacheManager.getCache("poems") != null) {
            cacheManager.getCache("poems").clear();
            log.info("诗词缓存已清除");
        }
    }
}
