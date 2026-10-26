package com.moyuan.dto.response;

import lombok.Data;

/**
 * 推荐条目。字段与前端 RecommendItem 约定一致，source 固定 local，
 * id 为 poem 主键，前端卡片据此直接跳 /poem/:id。
 */
@Data
public class RecommendItemDTO {
    private Long id;
    private String title;
    private String content;
    private String author;
    private String dynasty;
    private String source;
    private Double recommendScore;
    private String recommendReason;
}
