package com.moyuan.service.impl;

import com.moyuan.ai.AiModelRegistry;
import com.moyuan.exception.BusinessException;
import com.moyuan.common.ResultCode;
import com.moyuan.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiModelRegistry aiModelRegistry;

    private static final String CHAT_SYSTEM_PROMPT =
            "你是「古今诗话」平台的AI诗词助手，专注于中国古典诗词领域。" +
            "你的职责是：回答用户关于古诗词的各类问题，包括诗词鉴赏、诗人生平、创作背景、格律知识、诗词推荐等。" +
            "回答要求：语言优美、引经据典、深入浅出，必要时引用原诗句加以说明。" +
            "若用户问题超出诗词范畴，礼貌地引导回诗词话题。";

    private static final String VISION_SYSTEM_PROMPT =
            "你是「古今诗话」平台的看图写诗AI，擅长从图像中捕捉意境并创作古典诗词。" +
            "你的职责是：仔细观察图片中的景物、色彩、氛围和情感，将其转化为古典诗词的意象。" +
            "创作要求：1.严格遵循古诗词的格律和韵律；2.意象选取要与画面高度契合；3.语言凝练典雅，富有诗意；4.直接输出诗词内容，附上标题，不需要额外解释。";

    private static final String ANALYZE_SYSTEM_PROMPT =
            "你是「古今诗话」平台的诗词鉴赏专家，擅长对古典诗词进行深入细致的学术分析。" +
            "你的职责是：从意境、修辞、情感、历史背景等多个维度全面解读诗词作品。" +
            "分析要求：结构清晰、引经据典、学术严谨但通俗易懂，帮助读者深入理解诗词之美。";

    private static final String COUPLET_SYSTEM_PROMPT =
            "你是「古今诗话」平台的对联大师，精通中国古典对联艺术。" +
            "你的职责是：根据用户给出的上联，创作工整的下联。" +
            "创作要求：1.严格遵循对联的平仄规则（上联仄收，下联平收）；2.词性相对、结构相当；3.意境相承、内容相关；4.语言典雅、对仗工稳。" +
            "输出格式：先给出下联，再简要说明对仗思路。";

    @Override
    public String chat(String message, String model) {
        return aiModelRegistry.chat(message, model, CHAT_SYSTEM_PROMPT);
    }

    @Override
    public String writePoemFromImage(MultipartFile image, String model) {
        return writePoemFromImage(image, model, null);
    }

    @Override
    public String writePoemFromImage(MultipartFile image, String model, String visionModel) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            String prompt = "请根据这张图片创作一首古诗词。要求：1. 诗词要符合古诗词的格律和韵律；2. 内容要与图片意境相符；3. 请直接给出诗词内容，不需要额外解释。";
            return aiModelRegistry.vision(prompt, base64Image, model, visionModel, VISION_SYSTEM_PROMPT);
        } catch (IOException e) {
            log.error("图片处理失败", e);
            throw new BusinessException(ResultCode.ERROR, "图片处理失败: " + e.getMessage());
        }
    }

    @Override
    public String analyzePoem(String poem, String model) {
        String prompt = "请对以下古诗词进行详细分析，包括：\n" +
                "1. 诗词的意境和主题\n" +
                "2. 使用的修辞手法\n" +
                "3. 关键词句的赏析\n" +
                "4. 作者的情感表达\n" +
                "5. 诗词的历史背景（如果可能）\n\n" +
                "诗词内容：\n" + poem;
        return aiModelRegistry.chat(prompt, model, ANALYZE_SYSTEM_PROMPT);
    }

    @Override
    public String matchCouplet(String upperCouplet, String model) {
        String prompt = "请为以下上联对出下联：\n\n" +
                "上联：" + upperCouplet + "\n\n" +
                "请给出下联，并简要说明对仗思路。";
        return aiModelRegistry.chat(prompt, model, COUPLET_SYSTEM_PROMPT);
    }
}