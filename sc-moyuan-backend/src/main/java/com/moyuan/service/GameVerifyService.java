package com.moyuan.service;

import com.moyuan.dto.response.SentenceRef;

import java.util.List;

/**
 * 三级真实性校验链 + 本地规则工具 + AI JSON 解析。
 */
public interface GameVerifyService {

    /**
     * 真实性校验（三级链）：本地库 → APIHZ → LLM 兜底。
     */
    boolean verifyReal(String sentence);

    /**
     * 限定范围的真实性校验：仅当诗句属于指定朝代时才判定通过。
     * dynastyId 为空时等价于 {@link #verifyReal(String)}。
     *
     * @param sentence    待校验诗句
     * @param dynastyId   限定朝代 ID，为空表示不限朝代
     * @param dynastyName 限定朝代名称（用于 LLM 兜底提示），可为空
     */
    boolean verifyReal(String sentence, Long dynastyId, String dynastyName);

    /**
     * super-topic 主题语义匹配（LLM）。
     */
    boolean verifyTopic(String sentence, String topic);

    /**
     * 解析 AI 返回的候选诗句 JSON 数组；解析失败返回空列表，不抛异常。
     */
    List<SentenceRef> parseCandidates(String raw);

    /**
     * 归一化：去除标点、空白等非汉字字符。
     */
    String normalize(String sentence);

    /**
     * 汉字个数（归一化后长度）。
     */
    int hanziCount(String sentence);

    /**
     * 目标字在归一化文本中的位置（1 起始），未出现返回 -1。
     */
    int indexOfHanzi(String sentence, char target);

    /**
     * 归一化后的首个汉字，无汉字返回 0。
     */
    char firstHanzi(String sentence);

    /**
     * 归一化后的末个汉字，无汉字返回 0。
     */
    char lastHanzi(String sentence);
}
