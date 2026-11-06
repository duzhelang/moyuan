package com.moyuan.experiment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.entity.Poem;
import com.moyuan.mapper.PoemMapper;
import com.moyuan.service.AiService;
import com.moyuan.service.PingZeService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 论文第 9 章实验脚本：AI 对对联的「提示词约束」与「校验前置（Guard）」对照。
 * 运行方式：mvn -Dtest=PaperGuardExperimentTest test
 * 结果输出到 experiments/格律与路由实验_20261106/。
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("dev")
class PaperGuardExperimentTest {

    private static final Path OUT_DIR = Paths.get("d:\\A-xiangmu\\moyuan-master2\\experiments\\格律与路由实验_20261106");
    private static final long SEED = 20261106L;
    private static final int SAMPLE_SIZE = 50;
    private static final String MODEL = "deepseek";
    private static final String MODULE_CODE = "couplet";
    private static final Pattern PUNCT = Pattern.compile("[，。！？；、,.!?;\\n\\r]+");

    @Autowired
    private AiService aiService;
    @Autowired
    private PingZeService pingZeService;
    @Autowired
    private PoemMapper poemMapper;

    @Test
    void guardExperiment() throws Exception {
        List<String> upperCouplets = sampleUpperCouplets();
        List<String> lines = new ArrayList<>();
        lines.add("上联,基线下联,基线合格,Guard下联,Guard合格,是否重写,基线耗时ms,Guard耗时ms");

        int basePass = 0;
        int guardPass = 0;
        int rewritten = 0;
        long baseMsSum = 0;
        long guardMsSum = 0;

        for (int i = 0; i < upperCouplets.size(); i++) {
            String upper = upperCouplets.get(i);
            if (i > 0) {
                Thread.sleep(300); // 控制调用频率
            }

            long t0 = System.nanoTime();
            String baseResult;
            try {
                baseResult = aiService.matchCouplet(upper, MODEL, MODULE_CODE);
            } catch (Exception e) {
                log.warn("[guard] 第{}条基线调用失败：{}", i + 1, e.getMessage());
                continue;
            }
            long baseMs = (System.nanoTime() - t0) / 1_000_000;
            String baseLower = extractLowerCouplet(baseResult);
            boolean baseOk = isCoupletPassed(upper, baseLower);
            if (baseOk) {
                basePass++;
            }

            Thread.sleep(300);
            long t1 = System.nanoTime();
            Map<String, Object> guardResult;
            try {
                guardResult = aiService.matchCoupletChecked(upper, MODEL, MODULE_CODE);
            } catch (Exception e) {
                log.warn("[guard] 第{}条 Guard 调用失败：{}", i + 1, e.getMessage());
                continue;
            }
            long guardMs = (System.nanoTime() - t1) / 1_000_000;
            String guardRaw = String.valueOf(guardResult.get("result"));
            String guardLower = extractLowerCouplet(guardRaw);
            Map<String, Object> check = castMap(guardResult.get("check"));
            boolean guardOk = Boolean.TRUE.equals(check.get("passed"));
            boolean wasRewritten = Boolean.TRUE.equals(guardResult.get("rewritten"));
            if (guardOk) {
                guardPass++;
            }
            if (wasRewritten) {
                rewritten++;
            }
            baseMsSum += baseMs;
            guardMsSum += guardMs;

            lines.add(String.join(",", escape(upper), escape(baseLower), String.valueOf(baseOk),
                    escape(guardLower), String.valueOf(guardOk), String.valueOf(wasRewritten),
                    String.valueOf(baseMs), String.valueOf(guardMs)));
            log.info("[guard] {}/{} upper={} basePass={} guardPass={} rewritten={}",
                    i + 1, upperCouplets.size(), upper, baseOk, guardOk, wasRewritten);
        }

        int n = baseMsSum > 0 ? lines.size() - 1 : 0;
        lines.add(String.format("# 汇总,基线合格=%d/%d,Guard合格=%d/%d,重写=%d,基线平均耗时=%.0fms,Guard平均耗时=%.0fms",
                basePass, n, guardPass, n, rewritten,
                n > 0 ? baseMsSum * 1.0 / n : 0, n > 0 ? guardMsSum * 1.0 / n : 0));
        write("guard_compare.csv", lines);
        log.info("[experiment] Guard 对照完成：基线 {}/{}, Guard {}/{}, 重写 {}",
                basePass, n, guardPass, n, rewritten);
    }

    /** 抽取 50 条七言、末字为仄声的诗句作为上联 */
    private List<String> sampleUpperCouplets() {
        List<Poem> poems = poemMapper.selectList(new LambdaQueryWrapper<Poem>()
                .eq(Poem::getStatus, 1)
                .eq(Poem::getPoemType, "classical")
                .select(Poem::getId, Poem::getTitle, Poem::getContent));
        List<String> candidates = new ArrayList<>();
        for (Poem poem : poems) {
            for (String sentence : PUNCT.split(poem.getContent() == null ? "" : poem.getContent())) {
                String trimmed = sentence.trim();
                if (trimmed.isEmpty() || countChinese(trimmed) != 7) {
                    continue;
                }
                String pattern = String.valueOf(linesOf(pingZeService.checkTone(trimmed)).get(0).get("pattern"));
                if (pattern.endsWith("仄")) {
                    candidates.add(trimmed);
                }
            }
        }
        Collections.shuffle(candidates, new Random(SEED));
        return candidates.subList(0, Math.min(SAMPLE_SIZE, candidates.size()));
    }

    /** 复现 AiServiceImpl 的下联提取口径 */
    private String extractLowerCouplet(String raw) {
        if (raw == null) {
            return "";
        }
        for (String line : raw.split("\\R")) {
            String text = line.replaceAll("[#*`_>]", "").trim();
            text = text.replaceFirst("^(下联|对句|应答)[:：]", "").trim();
            if (text.isEmpty() || !text.matches(".*[\\u4E00-\\u9FA5].*")) {
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

    /** 与生产代码 checkCouplet 等价的判定：字数相等 + 下联平收 + 无三平/三仄尾 */
    private boolean isCoupletPassed(String upper, String lower) {
        if (lower.isEmpty()) {
            return false;
        }
        if (countChinese(upper) != countChinese(lower)) {
            return false;
        }
        Map<String, Object> check = pingZeService.checkTone(lower);
        List<Map<String, Object>> lines = linesOf(check);
        if (lines.isEmpty()) {
            return false;
        }
        String pattern = String.valueOf(lines.get(0).get("pattern"));
        if (!pattern.endsWith("平")) {
            return false;
        }
        String issues = String.valueOf(check.get("issues"));
        return !issues.contains("三平尾") && !issues.contains("三仄尾");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> linesOf(Map<String, Object> check) {
        return (List<Map<String, Object>>) check.get("lines");
    }

    private static int countChinese(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (c >= '\u4E00' && c <= '\u9FA5') {
                count++;
            }
        }
        return count;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace(",", "，").replace("\"", "'");
    }

    private void write(String fileName, List<String> lines) throws Exception {
        Files.createDirectories(OUT_DIR);
        try (BufferedWriter writer = Files.newBufferedWriter(OUT_DIR.resolve(fileName), StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
}
