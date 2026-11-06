package com.moyuan.service.impl;

import com.moyuan.ai.AiModelRegistry;
import com.moyuan.ai.StreamCallback;
import com.moyuan.exception.BusinessException;
import com.moyuan.common.ResultCode;
import com.moyuan.service.AiService;
import com.moyuan.service.PingZeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiModelRegistry aiModelRegistry;
    private final PingZeService pingZeService;

    private static final String CHAT_SYSTEM_PROMPT =
            "你是「古今诗话」平台的AI诗词助手，专注于中国古典诗词领域。" +
            "你的职责是：回答用户关于古诗词的各类问题，包括诗词鉴赏、诗人生平、创作背景、格律知识、诗词推荐等。" +
            "回答要求：语言优美、引经据典、深入浅出，必要时引用原诗句加以说明。" +
            "若用户问题超出诗词范畴，礼貌地引导回诗词话题。";

    private static final String VISION_SYSTEM_PROMPT =
            "你是「古今诗话」平台的看图写诗AI，擅长从图像中捕捉意境并创作古典诗词。" +
            "你的职责是：仔细观察图片中的景物、色彩、氛围和情感，将其转化为古典诗词的意象。" +
            "创作要求：1.严格遵循古诗词的格律和韵律；2.意象选取要与画面高度契合；3.语言凝练典雅，富有诗意；4.直接输出诗词内容，附上标题，不需要额外解释。";

    private static final String DEFAULT_POEM_PROMPT =
            "请根据这张图片创作一首古诗词。要求：1. 诗词要符合古诗词的格律和韵律；2. 内容要与图片意境相符；" +
            "3. 请直接给出诗词内容，不需要额外解释。";

    /** 纯书名号包裹的标题行 */
    private static final Pattern TITLE_LINE = Pattern.compile("^[《【\\[].*[》】\\]]$");
    /** 「作者：」「译文：」等元信息行 */
    private static final Pattern META_LINE = Pattern.compile("^(作者|题目|译文|注释|说明|格律)[:：].*");

    private static final String ANALYZE_SYSTEM_PROMPT =
            "你是「古今诗话」平台的诗词鉴赏专家，擅长对古典诗词进行深入细致的学术分析。" +
            "你的职责是：从意境、修辞、情感、历史背景等多个维度全面解读诗词作品。" +
            "分析要求：结构清晰、引经据典、学术严谨但通俗易懂，帮助读者深入理解诗词之美。" +
            "重要：输出格式必须为纯文本，禁止使用任何Markdown标记符号（如#、*、-、`等）。" +
            "每个分析维度作为独立段落，段落之间用空行分隔。段落开头用「一、」「二、」等中文序号标注。" +
            "末尾单独一行给出评分，格式为「评分：X.X」（X.X为1.0到5.0之间的小数）。";

    private static final String COUPLET_SYSTEM_PROMPT =
            "你是「古今诗话」平台的对联大师，精通中国古典对联艺术。" +
            "你的职责是：根据用户给出的上联，创作工整的下联。" +
            "创作要求：1.严格遵循对联的平仄规则（上联仄收，下联平收）；2.词性相对、结构相当；3.意境相承、内容相关；4.语言典雅、对仗工稳。" +
            "输出格式：先给出下联，再简要说明对仗思路。";

    private static final String OCR_SYSTEM_PROMPT =
            "你是一个专业的古籍文字识别专家，擅长识别中国古典诗词、书法作品、碑帖拓片等图像中的文字。" +
            "你的任务是精确识别图片中的所有文字内容。" +
            "识别要求：1.逐字准确识别，不得遗漏或篡改任何文字；2.保留原文的段落和换行结构；3.对于无法确定的字，用[?]标注；" +
            "4.如果有印章、题款、落款等辅助信息，也需识别并标注；5.直接输出识别结果，不需要额外解释。";

    @Override
    public String chat(String message, String model) {
        return chat(message, model, null);
    }

    @Override
    public String chat(String message, String model, String moduleCode) {
        if (moduleCode != null && !moduleCode.isEmpty()) {
            return aiModelRegistry.chatByModule(message, moduleCode, CHAT_SYSTEM_PROMPT);
        }
        return aiModelRegistry.chat(message, model, CHAT_SYSTEM_PROMPT);
    }

    @Override
    public void streamChat(List<Map<String, Object>> messages, String moduleCode,
                           Double temperature, Integer maxTokens, StreamCallback callback) {
        aiModelRegistry.streamByModule(messages, moduleCode, temperature, maxTokens, callback);
    }

    @Override
    public String writePoemFromImage(MultipartFile image, String model) {
        return writePoemFromImage(image, model, null, null);
    }

    @Override
    public String writePoemFromImage(MultipartFile image, String model, String visionModel) {
        return writePoemFromImage(image, model, visionModel, null);
    }

    @Override
    public String writePoemFromImage(MultipartFile image, String model, String visionModel, String moduleCode) {
        return visionPoem(DEFAULT_POEM_PROMPT, image, model, visionModel, moduleCode);
    }

    @Override
    public Map<String, Object> writePoemFromImageChecked(MultipartFile image, String model,
                                                         String visionModel, String moduleCode) {
        String raw = writePoemFromImage(image, model, visionModel, moduleCode);
        String body = extractPoemBody(raw);
        Map<String, Object> check = pingZeService.checkTone(body);

        boolean rewritten = false;
        if (!isPassed(check)) {
            // 校验前置：把具体格律问题反馈给模型，要求重写一次
            raw = visionPoem(buildPoemRewritePrompt(check), image, model, visionModel, moduleCode);
            body = extractPoemBody(raw);
            check = pingZeService.checkTone(body);
            rewritten = true;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("poem", raw);
        result.put("check", check);
        result.put("rewritten", rewritten);
        return result;
    }

    /** 统一的视觉写诗调用，供首轮生成与重写复用 */
    private String visionPoem(String prompt, MultipartFile image, String model,
                              String visionModel, String moduleCode) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            if (moduleCode != null && !moduleCode.isEmpty()) {
                return aiModelRegistry.visionByModule(prompt, base64Image, moduleCode, visionModel, VISION_SYSTEM_PROMPT);
            }
            return aiModelRegistry.vision(prompt, base64Image, model, visionModel, VISION_SYSTEM_PROMPT);
        } catch (IOException e) {
            log.error("图片处理失败", e);
            throw new BusinessException(ResultCode.ERROR, "图片处理失败: " + e.getMessage());
        }
    }

    @Override
    public String analyzePoem(String poem, String model) {
        return analyzePoem(poem, model, null);
    }

    @Override
    public String analyzePoem(String poem, String model, String moduleCode) {
        String prompt = "请对以下古诗词进行详细分析。\n\n" +
                "分析维度：\n" +
                "一、意境与主题\n" +
                "二、修辞手法\n" +
                "三、关键词句赏析\n" +
                "四、情感表达\n" +
                "五、历史背景（如可考证）\n\n" +
                "格式要求：每个维度独立成段，段落之间空一行。末尾另起一行给出评分（格式：评分：X.X）。\n" +
                "禁止使用任何Markdown符号（#、*、-、`等），直接输出纯文本。\n\n" +
                "诗词内容：\n" + poem;
        if (moduleCode != null && !moduleCode.isEmpty()) {
            return aiModelRegistry.chatByModule(prompt, moduleCode, ANALYZE_SYSTEM_PROMPT);
        }
        return aiModelRegistry.chat(prompt, model, ANALYZE_SYSTEM_PROMPT);
    }

    @Override
    public String matchCouplet(String upperCouplet, String model) {
        return matchCouplet(upperCouplet, model, null);
    }

    @Override
    public String matchCouplet(String upperCouplet, String model, String moduleCode) {
        return chatCouplet(buildCoupletPrompt(upperCouplet), model, moduleCode);
    }

    @Override
    public Map<String, Object> matchCoupletChecked(String upperCouplet, String model, String moduleCode) {
        String raw = matchCouplet(upperCouplet, model, moduleCode);
        Map<String, Object> check = checkCouplet(upperCouplet, raw);

        boolean rewritten = false;
        if (!isPassed(check)) {
            raw = chatCouplet(buildCoupletRewritePrompt(upperCouplet, check), model, moduleCode);
            check = checkCouplet(upperCouplet, raw);
            rewritten = true;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("result", raw);
        result.put("check", check);
        result.put("rewritten", rewritten);
        return result;
    }

    private String buildCoupletPrompt(String upperCouplet) {
        return "请为以下上联对出下联：\n\n上联：" + upperCouplet + "\n\n请给出下联，并简要说明对仗思路。";
    }

    /** 统一的对联生成调用，供首轮生成与重写复用 */
    private String chatCouplet(String prompt, String model, String moduleCode) {
        if (moduleCode != null && !moduleCode.isEmpty()) {
            return aiModelRegistry.chatByModule(prompt, moduleCode, COUPLET_SYSTEM_PROMPT);
        }
        return aiModelRegistry.chat(prompt, model, COUPLET_SYSTEM_PROMPT);
    }

    @Override
    public Map<String, Object> checkTone(String text) {
        return pingZeService.checkTone(text);
    }

    @Override
    public String ocrImage(MultipartFile image, String model, String visionModel, String moduleCode) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(ResultCode.ERROR, "图片不能为空");
        }
        try {
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            String prompt = "请识别这张图片中的所有文字内容，逐字准确识别，保留原文的段落和换行结构。如有印章、题款等也需识别。";
            if (moduleCode != null && !moduleCode.isEmpty()) {
                return aiModelRegistry.visionByModule(prompt, base64Image, moduleCode, visionModel, OCR_SYSTEM_PROMPT);
            }
            return aiModelRegistry.vision(prompt, base64Image, model, visionModel, OCR_SYSTEM_PROMPT);
        } catch (IOException e) {
            log.error("图片处理失败", e);
            throw new BusinessException(ResultCode.ERROR, "图片处理失败: " + e.getMessage());
        }
    }

    // ==================== 格律校验前置辅助方法 ====================

    /** 提取诗词正文：剥离 Markdown 记号、标题行、作者/注释等元信息，只保留诗句 */
    private String extractPoemBody(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            String text = line.replaceAll("[#*`_>]", "").trim();
            if (text.isEmpty()) {
                continue;
            }
            if (TITLE_LINE.matcher(text).matches() || META_LINE.matcher(text).matches()) {
                continue;
            }
            if (!text.matches(".*[\\u4E00-\\u9FA5].*")) {
                continue;
            }
            lines.add(text);
        }
        // 首行若为不带标点的短标题、且其后各行均含标点，则判定该行为标题并剔除
        if (lines.size() >= 3) {
            String first = lines.get(0);
            boolean hasPunct = first.matches(".*[，。！？；、].*");
            if (!hasPunct && countChinese(first) <= 8) {
                boolean restAllPunct = true;
                for (int i = 1; i < lines.size(); i++) {
                    if (!lines.get(i).matches(".*[，。！？；、].*")) {
                        restAllPunct = false;
                        break;
                    }
                }
                if (restAllPunct) {
                    lines.remove(0);
                }
            }
        }
        return String.join("\n", lines).trim();
    }

    /** 诗体重写提示词：把具体格律问题反馈给模型 */
    private String buildPoemRewritePrompt(Map<String, Object> check) {
        List<String> issues = issuesOf(check);
        return "你上一版作品存在以下格律问题：\n- " + String.join("\n- ", issues)
                + "\n请严格按近体诗格律重写：保持句数与每句字数一致，二四六字平仄交替，偶数句押平声韵，"
                + "避免三平尾与三仄尾。保持与原图一致的意境，直接输出诗词，不要解释。";
    }

    /** 对联校验：字数相等、上联仄收、下联平收，并对下联做三平尾/三仄尾检查 */
    private Map<String, Object> checkCouplet(String upper, String rawLower) {
        List<String> issues = new ArrayList<>();
        String lower = extractLowerCouplet(rawLower);
        if (lower.isEmpty()) {
            issues.add("未能从输出中提取出下联");
        } else {
            int upperCount = countChinese(upper);
            int lowerCount = countChinese(lower);
            if (upperCount != lowerCount) {
                issues.add("上下联字数不等（上联 " + upperCount + " 字，下联 " + lowerCount + " 字）");
            }
            String upperLast = lastTone(upper);
            String lowerLast = lastTone(lower);
            if ("平".equals(upperLast)) {
                issues.add("上联末字应为仄声（当前为平声）");
            }
            if ("仄".equals(lowerLast)) {
                issues.add("下联末字应为平声（当前为仄声）");
            }
            issues.addAll(issuesOf(pingZeService.checkTone(lower)));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passed", issues.isEmpty());
        result.put("issues", issues);
        result.put("lower", lower);
        return result;
    }

    /** 对联重写提示词 */
    private String buildCoupletRewritePrompt(String upper, Map<String, Object> check) {
        return "上联：" + upper + "\n\n你上一版下联存在以下问题：\n- " + String.join("\n- ", issuesOf(check))
                + "\n请重新对出下联：上下联字数相等，上联仄收、下联平收，词性相对、结构相当，"
                + "避免三平尾与三仄尾。先给出下联，再简要说明对仗思路。";
    }

    /** 从 AI 输出中提取下联本体（首个含汉字的行，去掉「下联：」前缀与后续说明） */
    private String extractLowerCouplet(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        for (String line : raw.split("\\R")) {
            String text = line.replaceAll("[#*`_>]", "").trim();
            text = text.replaceFirst("^(下联|对句|应答)[:：]", "").trim();
            if (!StringUtils.hasText(text) || !text.matches(".*[\\u4E00-\\u9FA5].*")) {
                continue;
            }
            int end = text.indexOf('。');
            if (end > 0) {
                text = text.substring(0, end);
            }
            return text.trim();
        }
        return "";
    }

    /** 取文本最后一个汉字的平仄（用于判定联句收声） */
    private String lastTone(String text) {
        Map<String, Object> check = pingZeService.checkTone(text);
        List<Map<String, Object>> lines = linesOf(check);
        if (lines.isEmpty()) {
            return "";
        }
        String pattern = String.valueOf(lines.get(lines.size() - 1).get("pattern"));
        return pattern.isEmpty() ? "" : pattern.substring(pattern.length() - 1);
    }

    private boolean isPassed(Map<String, Object> check) {
        return check != null && Boolean.TRUE.equals(check.get("passed"));
    }

    private static List<String> issuesOf(Map<String, Object> check) {
        if (check == null) {
            return List.of();
        }
        Object issues = check.get("issues");
        if (issues instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> linesOf(Map<String, Object> check) {
        if (check == null) {
            return List.of();
        }
        Object lines = check.get("lines");
        if (lines instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private static int countChinese(String text) {
        if (text == null) {
            return 0;
        }
        int count = 0;
        for (char c : text.toCharArray()) {
            if (c >= '\u4E00' && c <= '\u9FA5') {
                count++;
            }
        }
        return count;
    }
}
