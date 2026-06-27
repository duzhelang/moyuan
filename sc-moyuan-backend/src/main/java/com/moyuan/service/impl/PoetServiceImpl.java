package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.common.ResultCode;
import com.moyuan.entity.Dynasty;
import com.moyuan.entity.Poet;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.DynastyMapper;
import com.moyuan.mapper.PoetMapper;
import com.moyuan.service.DynastyService;
import com.moyuan.service.PoetService;
import com.moyuan.util.PoetDataExtractor;
import com.moyuan.util.PoetDefaultAvatar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoetServiceImpl extends ServiceImpl<PoetMapper, Poet> implements PoetService {

    private final PoetMapper poetMapper;
    private final DynastyMapper dynastyMapper;
    private final DynastyService dynastyService;
    private final RestTemplate restTemplate;
    private final CacheManager cacheManager;

    @Value("${apihz.id}")
    private String apihzId;

    @Value("${apihz.key}")
    private String apihzKey;

    private static final String API_URL = "https://cn.apihz.cn/api/zici/poet.php";

    @Override
    @Cacheable(value = "poets", key = "'list:' + #dynastyId + ':' + #categoryId + ':' + #keyword + ':' + #pageNum + ':' + #pageSize + ':' + #poetType")
    public IPage<Poet> getPoetList(int pageNum, int pageSize, Long dynastyId, Long categoryId, String keyword, String poetType) {
        LambdaQueryWrapper<Poet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poet::getStatus, 1);
        if (dynastyId != null) wrapper.eq(Poet::getDynastyId, dynastyId);
        if (categoryId != null) {
            wrapper.inSql(Poet::getId,
                "SELECT DISTINCT poet_id FROM poem WHERE category_id = " + categoryId + " AND status = 1 AND deleted = 0");
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Poet::getName, keyword);
        }
        if ("ancient".equals(poetType)) {
            wrapper.eq(Poet::getPoetType, "ancient");
        } else if ("modern".equals(poetType)) {
            wrapper.eq(Poet::getPoetType, "modern");
        }
        wrapper.orderByDesc(Poet::getCreateTime);
        IPage<Poet> page = poetMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        page.getRecords().forEach(poet -> {
            if (poet.getDynastyId() != null) {
                Dynasty dynasty = dynastyMapper.selectById(poet.getDynastyId());
                if (dynasty != null) {
                    poet.setDynastyName(dynasty.getName());
                }
            }
        });
        return page;
    }

    @Override
    @Cacheable(value = "poets", key = "'detail:' + #id")
    public Poet getPoetDetail(Long id) {
        Poet poet = poetMapper.selectById(id);
        if (poet == null || poet.getStatus() != 1) {
            throw new BusinessException(ResultCode.POET_NOT_FOUND);
        }
        if (poet.getDynastyId() != null) {
            Dynasty dynasty = dynastyMapper.selectById(poet.getDynastyId());
            if (dynasty != null) {
                poet.setDynastyName(dynasty.getName());
            }
        }
        // 懒加载：如果诗人信息不完整，从API获取
        if (isPoetInfoIncomplete(poet)) {
            fetchAndSavePoetData(poet);
            // 手动清除缓存，确保下次获取最新数据
            if (cacheManager.getCache("poets") != null) {
                cacheManager.getCache("poets").evict("detail:" + id);
            }
        }
        return poet;
    }

    private boolean isPoetInfoIncomplete(Poet poet) {
        return !StringUtils.hasText(poet.getBiography())
                || !StringUtils.hasText(poet.getLifeStory())
                || !StringUtils.hasText(poet.getInfluence())
                || !StringUtils.hasText(poet.getAnecdotes())
                || !StringUtils.hasText(poet.getAvatar());
    }

    private void fetchAndSavePoetData(Poet poet) {
        try {
            String url = String.format("%s?id=%s&key=%s&name=%s&page=1", API_URL, apihzId, apihzKey, poet.getName());
            log.info("懒加载诗人数据: {}", poet.getName());

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !Integer.valueOf(200).equals(response.get("code"))) {
                log.warn("获取诗人数据失败: {}", poet.getName());
                return;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
            if (dataList == null || dataList.isEmpty()) {
                log.warn("未找到诗人数据: {}", poet.getName());
                return;
            }

            Map<String, Object> poetData = dataList.get(0);
            String returnedName = (String) poetData.get("name");
            if (returnedName != null && !returnedName.contains(poet.getName()) && !poet.getName().contains(returnedName)) {
                log.warn("API返回的诗人名字不匹配: 期望={}, 实际={}", poet.getName(), returnedName);
                return;
            }

            boolean needUpdate = false;

            if (!StringUtils.hasText(poet.getPseudonym()) && poetData.containsKey("tag") && poetData.get("tag") != null) {
                poet.setPseudonym((String) poetData.get("tag"));
                needUpdate = true;
            }

            if (!StringUtils.hasText(poet.getBiography()) && poetData.containsKey("content") && poetData.get("content") != null) {
                String content = PoetDataExtractor.stripHtml((String) poetData.get("content"));
                if (content.length() >= 10) {
                    poet.setBiography(content);
                    PoetDataExtractor.extractYears(content, poet);
                    PoetDataExtractor.extractBirthplace(content, poet);
                    needUpdate = true;
                }
            }

            // 如果没有朝代信息，根据生卒年确定朝代
            if (poet.getDynastyId() == null && (poet.getBirthYear() != null || poet.getDeathYear() != null)) {
                Dynasty dynasty = dynastyService.determineDynastyByYears(poet.getBirthYear(), poet.getDeathYear());
                if (dynasty != null) {
                    poet.setDynastyId(dynasty.getId());
                    needUpdate = true;
                }
            }

            // 头像赋值（放在朝代之后，确保智能分配时有朝代信息）
            if (!StringUtils.hasText(poet.getAvatar()) && poetData.containsKey("image") && poetData.get("image") != null) {
                poet.setAvatar((String) poetData.get("image"));
                needUpdate = true;
            } else if (!StringUtils.hasText(poet.getAvatar())) {
                poet.setAvatar(PoetDefaultAvatar.getAvatar(poet));
                needUpdate = true;
            }

            if (!StringUtils.hasText(poet.getLifeStory()) && poetData.containsKey("rwsp") && poetData.get("rwsp") != null) {
                String lifeStory = PoetDataExtractor.stripHtml((String) poetData.get("rwsp"));
                if (lifeStory.length() >= 20) {
                    poet.setLifeStory(lifeStory);
                    needUpdate = true;
                }
            }

            if (!StringUtils.hasText(poet.getInfluence()) && poetData.containsKey("zycj") && poetData.get("zycj") != null) {
                String influence = PoetDataExtractor.stripHtml((String) poetData.get("zycj"));
                if (influence.length() >= 10) {
                    poet.setInfluence(influence);
                    needUpdate = true;
                }
            }

            if (!StringUtils.hasText(poet.getAnecdotes()) && poetData.containsKey("ysdg") && poetData.get("ysdg") != null) {
                String anecdotes = PoetDataExtractor.stripHtml((String) poetData.get("ysdg"));
                if (anecdotes.length() >= 10) {
                    poet.setAnecdotes(anecdotes);
                    needUpdate = true;
                }
            }

            if (needUpdate) {
                poetMapper.updateById(poet);
                log.info("诗人数据已更新: {}", poet.getName());
            }
        } catch (Exception e) {
            log.error("懒加载诗人数据失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void clearAllCache() {
        if (cacheManager.getCache("poets") != null) {
            cacheManager.getCache("poets").clear();
            log.info("诗人缓存已清除");
        }
    }
}
