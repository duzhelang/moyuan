package com.moyuan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoetRecommendationResponse {
    private Long poetId;
    private String name;
    private String dynasty;
    private String avatar;
    private String description;
    private Double score;
}