package com.moyuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyuan.entity.RecommendationInteraction;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface RecommendationInteractionMapper extends BaseMapper<RecommendationInteraction> {

    /**
     * 批量写入曝光/点击，唯一键去重（同一 batch + 作品 + 事件只保留一条）。
     */
    @Insert("<script>"
            + "INSERT IGNORE INTO recommendation_interaction "
            + "(batch_id, user_id, page, strategy, item_id, event_type) VALUES "
            + "<foreach collection='itemIds' item='itemId' separator=','>"
            + "(#{batchId}, #{userId}, #{page}, #{strategy}, #{itemId}, #{eventType})"
            + "</foreach>"
            + "</script>")
    int batchInsert(@Param("batchId") String batchId,
                    @Param("userId") Long userId,
                    @Param("page") String page,
                    @Param("strategy") String strategy,
                    @Param("eventType") String eventType,
                    @Param("itemIds") List<Long> itemIds);

    @Select("SELECT SUM(CASE WHEN event_type = 'impression' THEN 1 ELSE 0 END) AS impressions, "
            + "SUM(CASE WHEN event_type = 'click' THEN 1 ELSE 0 END) AS clicks, "
            + "COUNT(DISTINCT user_id) AS users "
            + "FROM recommendation_interaction WHERE create_time >= #{since}")
    Map<String, Object> aggregateTotal(@Param("since") LocalDateTime since);

    @Select("SELECT strategy, "
            + "SUM(CASE WHEN event_type = 'impression' THEN 1 ELSE 0 END) AS impressions, "
            + "SUM(CASE WHEN event_type = 'click' THEN 1 ELSE 0 END) AS clicks "
            + "FROM recommendation_interaction WHERE create_time >= #{since} "
            + "GROUP BY strategy")
    List<Map<String, Object>> aggregateByStrategy(@Param("since") LocalDateTime since);

    @Select("SELECT DATE_FORMAT(create_time, '%m-%d') AS date, "
            + "SUM(CASE WHEN event_type = 'impression' THEN 1 ELSE 0 END) AS impressions, "
            + "SUM(CASE WHEN event_type = 'click' THEN 1 ELSE 0 END) AS clicks "
            + "FROM recommendation_interaction WHERE create_time >= #{since} "
            + "GROUP BY DATE_FORMAT(create_time, '%m-%d') ORDER BY date")
    List<Map<String, Object>> aggregateDaily(@Param("since") LocalDateTime since);
}
