package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.entity.Dynasty;
import com.moyuan.entity.Poem;
import com.moyuan.entity.Poet;
import com.moyuan.service.DynastyService;
import com.moyuan.service.ExternalPoemImportService;
import com.moyuan.service.PoemService;
import com.moyuan.service.PoetService;
import com.moyuan.util.PoetDefaultAvatar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 外部诗词收录服务实现
 *
 * <p>落库规则（保证不重复入库）：以"标题 + 诗人ID"为唯一判据。
 * 诗人按姓名精确匹配；不存在时新建——静默收录建为隐藏（status=0）以免污染诗人列表，显式导入直接可见。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalPoemImportServiceImpl implements ExternalPoemImportService {

    private static final String SOURCE_EXTERNAL = "external";
    private static final String POEM_TYPE_ANCIENT = "ancient";
    private static final String POET_TYPE_ANCIENT = "ancient";
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_PENDING = 0;
    private static final int AUDIT_APPROVED = 1;
    private static final int AUDIT_PENDING = 0;
    /** 静默收录的待审核外部诗上限；达到后暂停收录，后台审核通过后自动释放容量 */
    private static final int MAX_PENDING_COLLECT = 100;

    private final PoemService poemService;
    private final PoetService poetService;
    private final DynastyService dynastyService;
    private final CacheManager cacheManager;

    @Override
    public Map<String, Object> importExternal(String title, String content, String author, String dynasty) {
        String trimmedTitle = title == null ? "" : title.trim();
        if (!StringUtils.hasText(trimmedTitle)) {
            return null;
        }

        Long dynastyId = resolveDynastyId(dynasty);
        Poet poet = resolvePoet(author, dynastyId, STATUS_PUBLISHED);
        Long poetId = poet == null ? null : poet.getId();
        // 诗人此前若由静默收录创建为"隐藏"，用户显式导入时将其一并公开
        if (poet != null && !Integer.valueOf(STATUS_PUBLISHED).equals(poet.getStatus())) {
            poet.setStatus(STATUS_PUBLISHED);
            poetService.updateById(poet);
            poetService.clearAllCache();
        }

        Poem existing = findExisting(trimmedTitle, poetId);
        Map<String, Object> result = new HashMap<>();
        if (existing != null) {
            boolean changed = false;
            if (existing.getPoetId() == null && poetId != null) {
                existing.setPoetId(poetId);
                changed = true;
            }
            if (existing.getDynastyId() == null && dynastyId != null) {
                existing.setDynastyId(dynastyId);
                changed = true;
            }
            // 静默收录的记录处于"待审核"；用户显式导入时应将其公开，保持诗人详情页原有可见效果。
            // 仅提升"待审核"记录：已被审核拒绝(auditStatus=2)的记录不得因显式导入而复活。
            boolean pendingAudit = existing.getAuditStatus() == null
                    || Integer.valueOf(AUDIT_PENDING).equals(existing.getAuditStatus());
            boolean promoted = SOURCE_EXTERNAL.equals(existing.getSource())
                    && !Integer.valueOf(STATUS_PUBLISHED).equals(existing.getStatus())
                    && pendingAudit;
            if (promoted) {
                existing.setStatus(STATUS_PUBLISHED);
                existing.setAuditStatus(AUDIT_APPROVED);
                existing.setAuditTime(LocalDateTime.now());
                changed = true;
            }
            if (changed) {
                poemService.updateById(existing);
                clearPoemCache();
            }
            result.put("id", existing.getId());
            result.put("imported", promoted);
            return result;
        }

        Poem newPoem = buildPoem(trimmedTitle, content, poetId, dynastyId, STATUS_PUBLISHED, AUDIT_APPROVED);
        poemService.save(newPoem);
        clearPoemCache();
        result.put("id", newPoem.getId());
        result.put("imported", true);
        return result;
    }

    @Override
    public int collectPending(List<Map<String, Object>> poems) {
        if (poems == null || poems.isEmpty()) {
            return 0;
        }
        // 容量控制：待审核的外部诗达到上限后暂停收录（后台审核通过、数量回落后自动恢复收录）
        int room = MAX_PENDING_COLLECT - countPendingExternal();
        if (room <= 0) {
            log.info("外部诗词待审核已达上限 {} 条，暂停静默收录", MAX_PENDING_COLLECT);
            return 0;
        }
        int collected = 0;
        for (Map<String, Object> item : poems) {
            if (collected >= room) {
                break;
            }
            try {
                if (item == null) {
                    continue;
                }
                String title = item.get("title") == null ? null : String.valueOf(item.get("title"));
                if (!StringUtils.hasText(title)) {
                    continue;
                }
                String trimmedTitle = title.trim();
                String content = item.get("content") == null ? "" : String.valueOf(item.get("content"));
                String author = item.get("author") == null ? "" : String.valueOf(item.get("author"));
                String dynasty = item.get("dynasty") == null ? "" : String.valueOf(item.get("dynasty"));

                Long dynastyId = resolveDynastyId(dynasty);
                Poet poet = resolvePoet(author, dynastyId, STATUS_PENDING);
                Long poetId = poet == null ? null : poet.getId();

                // 已收录（无论待审核还是已上架）则跳过，保证幂等不重复
                if (findExisting(trimmedTitle, poetId) != null) {
                    continue;
                }
                // 待审核记录不进入 status=1 的公开列表，因此无需清理诗词列表缓存
                poemService.save(buildPoem(trimmedTitle, content, poetId, dynastyId, STATUS_PENDING, AUDIT_PENDING));
                collected++;
            } catch (Exception e) {
                log.warn("外部诗词静默收录单条失败，已跳过: title={}, error={}", item.get("title"), e.getMessage());
            }
        }
        if (collected > 0) {
            log.info("外部诗词静默收录完成，新增待审核 {} 条", collected);
        }
        return collected;
    }

    /** 统计当前处于"待审核"状态的静默收录外部诗数量 */
    private int countPendingExternal() {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getSource, SOURCE_EXTERNAL)
                .eq(Poem::getAuditStatus, AUDIT_PENDING);
        return Math.toIntExact(poemService.count(wrapper));
    }

    /** 新建诗词实体，统一填充各模式共用的默认字段 */
    private Poem buildPoem(String title, String content, Long poetId, Long dynastyId, int status, int auditStatus) {
        Poem poem = new Poem();
        poem.setTitle(title);
        poem.setContent(content != null ? content.trim() : "");
        poem.setPoetId(poetId);
        poem.setDynastyId(dynastyId);
        poem.setSource(SOURCE_EXTERNAL);
        poem.setViewCount(0);
        poem.setLikeCount(0);
        poem.setFavoriteCount(0);
        poem.setStatus(status);
        poem.setIsFeatured(0);
        poem.setIsOriginal(0);
        poem.setPoemType(POEM_TYPE_ANCIENT);
        poem.setAuditStatus(auditStatus);
        return poem;
    }

    /** 按标题 + 诗人查重；诗人未解析到时退化为"标题 + 诗人为空" */
    private Poem findExisting(String title, Long poetId) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getTitle, title);
        if (poetId != null) {
            wrapper.eq(Poem::getPoetId, poetId);
        } else {
            wrapper.isNull(Poem::getPoetId);
        }
        wrapper.last("LIMIT 1");
        return poemService.getOne(wrapper);
    }

    /** 朝代按名称精确匹配，不新建朝代 */
    private Long resolveDynastyId(String dynasty) {
        if (!StringUtils.hasText(dynasty)) {
            return null;
        }
        LambdaQueryWrapper<Dynasty> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dynasty::getName, dynasty.trim()).last("LIMIT 1");
        Dynasty entity = dynastyService.getOne(wrapper);
        return entity == null ? null : entity.getId();
    }

    /**
     * 诗人按姓名精确匹配；不存在时按指定状态新建。
     * 静默收录建为隐藏（status=0），避免无作品公开的诗人污染诗人列表；显式导入则直接可见。
     */
    private Poet resolvePoet(String author, Long dynastyId, int createStatus) {
        if (!StringUtils.hasText(author)) {
            return null;
        }
        String name = author.trim();
        LambdaQueryWrapper<Poet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poet::getName, name).last("LIMIT 1");
        Poet poet = poetService.getOne(wrapper);
        if (poet != null) {
            return poet;
        }
        Poet newPoet = new Poet();
        newPoet.setName(name);
        newPoet.setDynastyId(dynastyId);
        newPoet.setPoetType(POET_TYPE_ANCIENT);
        newPoet.setStatus(createStatus);
        // 与其它建号入口保持一致，自动建号时即智能分配预设头像
        newPoet.setAvatar(PoetDefaultAvatar.getAvatar(newPoet));
        poetService.save(newPoet);
        if (createStatus == STATUS_PUBLISHED) {
            poetService.clearAllCache();
        }
        return newPoet;
    }

    private void clearPoemCache() {
        if (cacheManager.getCache("poems") != null) {
            cacheManager.getCache("poems").clear();
        }
    }
}
