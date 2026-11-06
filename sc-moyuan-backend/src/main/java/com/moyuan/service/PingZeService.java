package com.moyuan.service;

import java.util.Map;
import java.util.Set;

/**
 * 平仄服务：汉字平仄标注、格律校验与「格律检索」。
 *
 * <p>平仄判定基于 pinyin4j 的带声调拼音：一、二声为平，三、四声为仄；
 * 多音字若同时存在平、仄读音则判为「中」（可平可仄）；另以内置入声字表
 * 强制判仄，规避现代普通话「入声消失」导致的误判。</p>
 */
public interface PingZeService {

    /**
     * 标注并校验一段诗词文本。
     *
     * @param text 诗词文本（可含标点与换行）
     * @return {passed, genre, issues, lines:[{text, pattern, chars:[{char,pingze,pinyin}], issues}]}
     */
    Map<String, Object> checkTone(String text);

    /** 判断关键词是否为格律查询串（只由「平/仄/中」及分隔符构成，且至少含两个符号） */
    boolean isPingZeQuery(String keyword);

    /**
     * 按平仄模式检索诗句。
     *
     * @param pattern     格律查询串，如「平平仄仄平平仄」，「中」为通配符
     * @return {list, total, pingzeMatches:[{poemId, line, start, pattern, plainLine}]}
     */
    Map<String, Object> searchByPattern(String pattern, Long dynastyId, Long categoryId, Long poetId,
                                        Set<Long> poetNameIds, int pageNum, int pageSize);
}
