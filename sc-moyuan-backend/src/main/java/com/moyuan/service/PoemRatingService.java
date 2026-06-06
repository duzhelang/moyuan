package com.moyuan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.PoemRating;
import java.math.BigDecimal;
import java.util.Map;

public interface PoemRatingService extends IService<PoemRating> {
    void ratePoem(Long poemId, Long userId, BigDecimal score, String comment);
    PoemRating getAiRating(Long poemId);
    void requestAiRating(Long poemId);
    Map<String, Object> getPoemRatings(Long poemId);
    BigDecimal getAverageScore(Long poemId);
}
