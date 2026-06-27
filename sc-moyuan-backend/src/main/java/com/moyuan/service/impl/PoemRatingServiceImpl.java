package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.entity.Poem;
import com.moyuan.entity.PoemRating;
import com.moyuan.mapper.PoemRatingMapper;
import com.moyuan.service.AiService;
import com.moyuan.service.PoemRatingService;
import com.moyuan.service.PoemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoemRatingServiceImpl extends ServiceImpl<PoemRatingMapper, PoemRating> implements PoemRatingService {

    private final PoemService poemService;
    private final AiService aiService;

    @Value("${ai.default-model:zhipu}")
    private String defaultAiModel;

    @Override
    @Transactional
    public void ratePoem(Long poemId, Long userId, BigDecimal score, String comment) {
        PoemRating existing = getOne(new LambdaQueryWrapper<PoemRating>()
                .eq(PoemRating::getPoemId, poemId)
                .eq(PoemRating::getUserId, userId)
                .eq(PoemRating::getRatingType, 1));

        if (existing != null) {
            existing.setScore(score);
            existing.setComment(comment);
            updateById(existing);
        } else {
            PoemRating rating = new PoemRating();
            rating.setPoemId(poemId);
            rating.setUserId(userId);
            rating.setScore(score);
            rating.setRatingType(1);
            rating.setComment(comment);
            save(rating);
        }

        updatePoemAverageScore(poemId);
    }

    @Override
    public PoemRating getAiRating(Long poemId) {
        Page<PoemRating> page = page(new Page<>(1, 1), new LambdaQueryWrapper<PoemRating>()
                .eq(PoemRating::getPoemId, poemId)
                .eq(PoemRating::getRatingType, 2)
                .orderByDesc(PoemRating::getCreateTime));
        return page.getRecords().isEmpty() ? null : page.getRecords().get(0);
    }

    @Override
    @Transactional
    public void requestAiRating(Long poemId) {
        Poem poem = poemService.getById(poemId);
        if (poem == null) {
            throw new RuntimeException("诗词不存在");
        }

        String analysis = aiService.analyzePoem(poem.getContent(), defaultAiModel);
        BigDecimal score = parseAiScore(analysis);
        String summary = generateSummary(analysis);
        String cleanedAnalysis = cleanSpecialSymbols(analysis);

        PoemRating existing = getAiRating(poemId);
        if (existing != null) {
            existing.setScore(score);
            existing.setAiModel(defaultAiModel);
            existing.setAiSummary(summary);
            existing.setAiAnalysis(cleanedAnalysis);
            updateById(existing);
        } else {
            PoemRating rating = new PoemRating();
            rating.setPoemId(poemId);
            rating.setUserId(null);
            rating.setScore(score);
            rating.setRatingType(2);
            rating.setAiModel(defaultAiModel);
            rating.setAiSummary(summary);
            rating.setAiAnalysis(cleanedAnalysis);
            save(rating);
        }

        updatePoemAverageScore(poemId);
    }

    @Override
    public Map<String, Object> getPoemRatings(Long poemId) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<PoemRating> userRatings = list(new LambdaQueryWrapper<PoemRating>()
                    .eq(PoemRating::getPoemId, poemId)
                    .eq(PoemRating::getRatingType, 1)
                    .orderByDesc(PoemRating::getCreateTime));

            PoemRating aiRating = getAiRating(poemId);
            BigDecimal avgScore = getAverageScore(poemId);

            result.put("userRatings", userRatings != null ? userRatings : java.util.Collections.emptyList());
            result.put("aiRating", aiRating);
            result.put("averageScore", avgScore != null ? avgScore : BigDecimal.ZERO);
            result.put("ratingCount", userRatings != null ? userRatings.size() : 0);
        } catch (Exception e) {
            log.error("获取诗词评分失败, poemId={}", poemId, e);
            result.put("userRatings", java.util.Collections.emptyList());
            result.put("aiRating", null);
            result.put("averageScore", BigDecimal.ZERO);
            result.put("ratingCount", 0);
        }

        return result;
    }

    @Override
    public BigDecimal getAverageScore(Long poemId) {
        Poem poem = poemService.getById(poemId);
        return poem != null ? poem.getAvgScore() : null;
    }

    @Override
    public PoemRating getUserRating(Long poemId, Long userId) {
        return getOne(new LambdaQueryWrapper<PoemRating>()
                .eq(PoemRating::getPoemId, poemId)
                .eq(PoemRating::getUserId, userId)
                .eq(PoemRating::getRatingType, 1));
    }

    private void updatePoemAverageScore(Long poemId) {
        List<PoemRating> ratings = list(new LambdaQueryWrapper<PoemRating>()
                .eq(PoemRating::getPoemId, poemId));

        if (ratings.isEmpty()) {
            return;
        }

        BigDecimal total = ratings.stream()
                .map(PoemRating::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avg = total.divide(BigDecimal.valueOf(ratings.size()), 1, RoundingMode.HALF_UP);

        Poem poem = poemService.getById(poemId);
        if (poem != null) {
            poem.setAvgScore(avg);
            poem.setRatingCount(ratings.size());
            poemService.updateById(poem);
        }
    }

    private BigDecimal parseAiScore(String analysis) {
        try {
            Pattern pattern = Pattern.compile("评分[：:]\\s*(\\d+(\\.\\d+)?)");
            Matcher matcher = pattern.matcher(analysis);
            if (matcher.find()) {
                return new BigDecimal(matcher.group(1));
            }
            Pattern pattern2 = Pattern.compile("得分[：:]\\s*(\\d+(\\.\\d+)?)");
            Matcher matcher2 = pattern2.matcher(analysis);
            if (matcher2.find()) {
                return new BigDecimal(matcher2.group(1));
            }
        } catch (Exception e) {
            log.warn("解析AI评分失败，使用默认评分", e);
        }
        return new BigDecimal("3.5");
    }

    private String generateSummary(String analysis) {
        if (analysis == null || analysis.isEmpty()) {
            return "";
        }
        String[] paragraphs = analysis.split("\\n\\n+");
        StringBuilder summary = new StringBuilder();
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (!trimmed.isEmpty() && summary.length() < 200) {
                if (summary.length() > 0) {
                    summary.append("\n\n");
                }
                summary.append(trimmed);
            }
        }
        return summary.toString();
    }

    private String cleanSpecialSymbols(String text) {
        if (text == null) {
            return "";
        }
        return text
                   .replaceAll("\\*{1,2}([^*]+)\\*{1,2}", "$1")
                   .replaceAll("`([^`]+)`", "$1")
                   .replaceAll("(?m)^\\s*[-*+]\\s+", "")
                   .replaceAll("(?m)^#{1,6}\\s+", "")
                   .replaceAll("[~>]", "")
                   .replaceAll("(?m)^\\s*[-=]{3,}\\s*$", "")
                   .replaceAll("\\n{3,}", "\n\n")
                   .trim();
    }
}
