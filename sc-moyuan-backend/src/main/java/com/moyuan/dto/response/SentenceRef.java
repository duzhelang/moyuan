package com.moyuan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 诗句引用（AI 候选 / AI 接句 / 起句）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SentenceRef {

    /** 诗句原文 */
    private String sentence;

    /** 诗题 */
    private String title;

    /** 作者 */
    private String author;

    /** 匹配/衔接说明 */
    private String matchDesc;
}
