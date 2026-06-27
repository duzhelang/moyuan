package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.entity.Dynasty;
import com.moyuan.entity.Poet;
import com.moyuan.mapper.DynastyMapper;
import com.moyuan.mapper.PoetMapper;
import com.moyuan.service.DynastyService;
import com.moyuan.service.PoetSyncService;
import com.moyuan.util.PoetDataExtractor;
import com.moyuan.util.PoetDefaultAvatar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoetSyncServiceImpl implements PoetSyncService {

    private final PoetMapper poetMapper;
    private final DynastyMapper dynastyMapper;
    private final DynastyService dynastyService;
    private final RestTemplate restTemplate;

    @Value("${apihz.id}")
    private String apihzId;

    @Value("${apihz.key}")
    private String apihzKey;

    private static final String API_URL = "https://cn.apihz.cn/api/zici/poet.php";

    @Override
    public Map<String, Object> syncPoetData(String poetName) {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = String.format("%s?id=%s&key=%s&name=%s&page=1", API_URL, apihzId, apihzKey, poetName);
            log.info("调用接口盒子API同步诗人数据: poetName={}", poetName);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !Integer.valueOf(200).equals(response.get("code"))) {
                String msg = response != null ? (String) response.get("msg") : "请求失败";
                result.put("success", false);
                result.put("message", msg);
                return result;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
            if (dataList == null || dataList.isEmpty()) {
                result.put("success", false);
                result.put("message", "未找到诗人数据");
                return result;
            }

            Map<String, Object> poetData = dataList.get(0);
            String name = (String) poetData.get("name");

            Poet poet = poetMapper.selectOne(
                    new LambdaQueryWrapper<Poet>().eq(Poet::getName, name));

            if (poet == null) {
                poet = new Poet();
                poet.setName(name);
                poet.setStatus(1);
            }

            String dynastyName = (String) poetData.get("dynasty");
            if (dynastyName != null) {
                Dynasty dynasty = dynastyMapper.selectOne(
                        new LambdaQueryWrapper<Dynasty>().eq(Dynasty::getName, dynastyName));
                if (dynasty != null) {
                    poet.setDynastyId(dynasty.getId());
                }
            }

            if (poetData.containsKey("tag") && poetData.get("tag") != null) {
                poet.setPseudonym((String) poetData.get("tag"));
            }

            if (poetData.containsKey("content") && poetData.get("content") != null) {
                String content = PoetDataExtractor.stripHtml((String) poetData.get("content"));
                poet.setBiography(content);
                PoetDataExtractor.extractYears(content, poet);
                PoetDataExtractor.extractBirthplace(content, poet);
            }

            // 如果没有从API获取到朝代，根据生卒年确定朝代
            if (poet.getDynastyId() == null && (poet.getBirthYear() != null || poet.getDeathYear() != null)) {
                Dynasty dynasty = dynastyService.determineDynastyByYears(poet.getBirthYear(), poet.getDeathYear());
                if (dynasty != null) {
                    poet.setDynastyId(dynasty.getId());
                }
            }

            // 头像赋值（放在朝代之后，确保智能分配时有朝代信息）
            if (poetData.containsKey("image") && poetData.get("image") != null) {
                poet.setAvatar((String) poetData.get("image"));
            } else if (!StringUtils.hasText(poet.getAvatar())) {
                poet.setAvatar(PoetDefaultAvatar.getAvatar(poet));
            }

            if (poetData.containsKey("rwsp") && poetData.get("rwsp") != null) {
                poet.setLifeStory(PoetDataExtractor.stripHtml((String) poetData.get("rwsp")));
            }

            if (poetData.containsKey("zycj") && poetData.get("zycj") != null) {
                poet.setInfluence(PoetDataExtractor.stripHtml((String) poetData.get("zycj")));
            }

            if (poetData.containsKey("ysdg") && poetData.get("ysdg") != null) {
                poet.setAnecdotes(PoetDataExtractor.stripHtml((String) poetData.get("ysdg")));
            }

            if (poet.getId() == null) {
                poetMapper.insert(poet);
            } else {
                poetMapper.updateById(poet);
            }

            result.put("success", true);
            result.put("message", "同步成功");
            result.put("poet", poet);
            return result;
        } catch (Exception e) {
            log.error("同步诗人数据失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "同步失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> syncAllPoets() {
        Map<String, Object> result = new HashMap<>();
        List<Poet> poets = poetMapper.selectList(
                new LambdaQueryWrapper<Poet>().eq(Poet::getStatus, 1));
        int successCount = 0;
        int failCount = 0;
        List<String> failedNames = new ArrayList<>();

        for (Poet poet : poets) {
            if (poet.getBiography() != null && !poet.getBiography().isEmpty()) {
                continue;
            }
            Map<String, Object> syncResult = syncPoetData(poet.getName());
            if (Boolean.TRUE.equals(syncResult.get("success"))) {
                successCount++;
            } else {
                failCount++;
                failedNames.add(poet.getName());
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        result.put("success", true);
        result.put("totalCount", poets.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failedNames", failedNames);
        return result;
    }

}