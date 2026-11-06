package com.moyuan.service;

import com.moyuan.ai.StreamCallback;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface AiService {

    String chat(String message, String model);

    String chat(String message, String model, String moduleCode);

    /**
     * 流式对话。{@code messages} 为完整的 system + 历史 + user 数组；
     * {@code moduleCode} 为空时回退默认模型；结果通过 {@link StreamCallback} 异步回调。
     */
    void streamChat(List<Map<String, Object>> messages, String moduleCode,
                    Double temperature, Integer maxTokens, StreamCallback callback);

    String writePoemFromImage(MultipartFile image, String model);

    String writePoemFromImage(MultipartFile image, String model, String visionModel);

    String writePoemFromImage(MultipartFile image, String model, String visionModel, String moduleCode);

    /**
     * 看图写诗 + 格律校验前置：生成后做句数/字数、平仄与押韵检查，
     * 不合格时带问题反馈重写一次。
     *
     * @return {poem, check:{passed,genre,issues,lines}, rewritten}
     */
    Map<String, Object> writePoemFromImageChecked(MultipartFile image, String model,
                                                  String visionModel, String moduleCode);

    String analyzePoem(String poem, String model);

    String analyzePoem(String poem, String model, String moduleCode);

    String matchCouplet(String upperCouplet, String model);

    String matchCouplet(String upperCouplet, String model, String moduleCode);

    /**
     * AI 对对联 + 格律校验前置：校验上下联字数、上联仄收、下联平收，
     * 不合格时带问题反馈重写一次。
     *
     * @return {result, check:{passed,issues}, rewritten}
     */
    Map<String, Object> matchCoupletChecked(String upperCouplet, String model, String moduleCode);

    /** 平仄标注与格律校验 */
    Map<String, Object> checkTone(String text);

    String ocrImage(MultipartFile image, String model, String visionModel, String moduleCode);
}
