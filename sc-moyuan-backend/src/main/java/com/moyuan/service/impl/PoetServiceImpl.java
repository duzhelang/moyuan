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
import com.moyuan.service.PoetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoetServiceImpl extends ServiceImpl<PoetMapper, Poet> implements PoetService {

    private final PoetMapper poetMapper;
    private final DynastyMapper dynastyMapper;
    private final RestTemplate restTemplate;

    @Value("${apihz.id}")
    private String apihzId;

    @Value("${apihz.key}")
    private String apihzKey;

    private static final String API_URL = "https://cn.apihz.cn/api/zici/poet.php";

    @Override
    @Cacheable(value = "poets", key = "'list:' + #dynastyId + ':' + #keyword + ':' + #pageNum + ':' + #pageSize + ':' + #poetType")
    public IPage<Poet> getPoetList(int pageNum, int pageSize, Long dynastyId, String keyword, String poetType) {
        LambdaQueryWrapper<Poet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poet::getStatus, 1);
        if (dynastyId != null) wrapper.eq(Poet::getDynastyId, dynastyId);
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

    @CacheEvict(value = "poets", key = "'detail:' + #poet.id")
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
            boolean needUpdate = false;

            if (!StringUtils.hasText(poet.getAvatar()) && poetData.containsKey("image") && poetData.get("image") != null) {
                poet.setAvatar((String) poetData.get("image"));
                needUpdate = true;
            }

            if (!StringUtils.hasText(poet.getPseudonym()) && poetData.containsKey("tag") && poetData.get("tag") != null) {
                poet.setPseudonym((String) poetData.get("tag"));
                needUpdate = true;
            }

            if (!StringUtils.hasText(poet.getBiography()) && poetData.containsKey("content") && poetData.get("content") != null) {
                String content = stripHtml((String) poetData.get("content"));
                poet.setBiography(content);
                extractYears(content, poet);
                extractBirthplace(content, poet);
                needUpdate = true;
            }

            if (!StringUtils.hasText(poet.getLifeStory()) && poetData.containsKey("rwsp") && poetData.get("rwsp") != null) {
                poet.setLifeStory(stripHtml((String) poetData.get("rwsp")));
                needUpdate = true;
            }

            if (!StringUtils.hasText(poet.getInfluence()) && poetData.containsKey("zycj") && poetData.get("zycj") != null) {
                poet.setInfluence(stripHtml((String) poetData.get("zycj")));
                needUpdate = true;
            }

            if (!StringUtils.hasText(poet.getAnecdotes()) && poetData.containsKey("ysdg") && poetData.get("ysdg") != null) {
                poet.setAnecdotes(stripHtml((String) poetData.get("ysdg")));
                needUpdate = true;
            }

            if (needUpdate) {
                poetMapper.updateById(poet);
                log.info("诗人数据已更新: {}", poet.getName());
            }
        } catch (Exception e) {
            log.error("懒加载诗人数据失败: {}", e.getMessage(), e);
        }
    }

    private String stripHtml(String html) {
        if (html == null) return null;
        return html.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
    }

    private void extractYears(String content, Poet poet) {
        // 匹配多种格式：701年－762年、约984年—约1053年、前340年-前278年
        Pattern pattern = Pattern.compile("(?:约|大约)?\\s*(?:前)?(\\d{3,4})\\s*年\\s*[－—\\-~～至]\\s*(?:约|大约)?\\s*(?:前)?(\\d{3,4})\\s*年");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            poet.setBirthYear(Integer.parseInt(matcher.group(1)));
            poet.setDeathYear(Integer.parseInt(matcher.group(2)));
        } else {
            // 尝试匹配只有出生年的情况：生于701年
            Pattern birthPattern = Pattern.compile("(?:生于|出生于|出生)\\s*(?:约|大约)?\\s*(?:前)?(\\d{3,4})\\s*年");
            Matcher birthMatcher = birthPattern.matcher(content);
            if (birthMatcher.find()) {
                poet.setBirthYear(Integer.parseInt(birthMatcher.group(1)));
            }
            // 尝试匹配只有去世年的情况：卒于762年、去世于762年
            Pattern deathPattern = Pattern.compile("(?:卒于|去世于|卒|逝世于)\\s*(?:约|大约)?\\s*(?:前)?(\\d{3,4})\\s*年");
            Matcher deathMatcher = deathPattern.matcher(content);
            if (deathMatcher.find()) {
                poet.setDeathYear(Integer.parseInt(deathMatcher.group(1)));
            }
        }
    }

    private void extractBirthplace(String content, Poet poet) {
        Pattern pattern = Pattern.compile("(?:生于|出生于|出生地[：:])\\s*(.{2,10}?)(?:[，,。]|\\s|$)");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            poet.setBirthplace(matcher.group(1).trim());
        }
    }
}
