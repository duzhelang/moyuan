package com.moyuan.experiment;

import com.moyuan.ai.AiModelRegistry;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 论文 9.5.3 的配对补充实验（不改动 guard_compare.csv，结果另存）。
 *
 * 设计：从已有 guard_compare.csv 中取出「基线不合格」的条目，以同一批基线输出为共同起点，
 * 对每条各做两种处理：
 *   A. 盲重写：用原始生成提示词再生成一次（无任何失败信息）
 *   B. 反馈重写：把 checkCouplet 得到的 issues 拼进重写提示词再生成（即 Guard 的第二步）
 * 基线合格的条目直接判过，因此配对后的最终合格率 = 39 + 各自修复数。
 *
 * 两条链路起步于同一条不合格下联，故可用 McNemar 精确检验比较 A 与 B。
 * 重写提示词与系统提示词逐字复刻 AiServiceImpl（该方法为 private，无法直接调用）。
 *
 * 运行：mvn -Dtest=PaperGuardPairedExperimentTest test
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("dev")
class PaperGuardPairedExperimentTest {

    private static final Path BASE_CSV = Paths.get(
            "d:\\A-xiangmu\\moyuan-master2\\experiments\\格律与路由实验_20261106\\guard_compare.csv");
    private static final Path OUT_CSV = Paths.get(
            "d:\\A-xiangmu\\moyuan-master2\\experiments\\格律与路由实验_20261106\\guard_paired_20261106.csv");

    private static final String MODULE_CODE = "couplet";

    /** 与 AiServiceImpl.COUPLET_SYSTEM_PROMPT 逐字一致 */
    private static final String SYSTEM_PROMPT =
            "你是「古今诗话」平台的对联大师，精通中国古典对联艺术。"
                    + "你的职责是：根据用户给出的上联，创作工整的下联。"
                    + "创作要求：1.严格遵循对联的平仄规则（上联仄收，下联平收）；2.词性相对、结构相当；3.意境相承、内容相关；4.语言典雅、对仗工稳。"
                    + "输出格式：先给出下联，再简要说明对仗思路。";

    private static final Pattern PUNCT = Pattern.compile("[，。！？；、,.!?;\\n\\r]+");

    @Autowired
    private AiModelRegistry aiModelRegistry;
    @Autowired
    private PingZeService pingZeService;

    @Test
    void pairedExperiment() throws Exception {
        List<String[]> failed = new ArrayList<>();
        for (String line : Files.readAllLines(BASE_CSV, StandardCharsets.UTF_8)) {
            if (line.startsWith("#") || line.startsWith("上联,")) {
                continue;
            }
            String[] f = line.split(",");
            if (f.length < 6) {
                continue;
            }
            if ("false".equalsIgnoreCase(f[2].trim())) {
                failed.add(f);
            }
        }
        log.info("[paired] 基线不合格条目 {} 条", failed.size());

        List<String> out = new ArrayList<>();
        out.add("上联,基线下联,基线问题,盲写下联,盲写合格,反馈下联,反馈合格,盲写耗时ms,反馈耗时ms");

        int blindFix = 0;
        int guardFix = 0;
        long blindMs = 0;
        long guardMs = 0;

        for (int i = 0; i < failed.size(); i++) {
            String upper = failed.get(i)[0];
            String baseLower = failed.get(i)[1];
            Map<String, Object> baseCheck = checkCouplet(upper, baseLower);
            String issues = String.join("; ", issuesOf(baseCheck));

            // A. 盲重写：原始提示词再生成一次
            String blindLower = "";
            boolean blindOk = false;
            long t0 = System.nanoTime();
            try {
                String raw = aiModelRegistry.chatByModule(buildCoupletPrompt(upper), MODULE_CODE, SYSTEM_PROMPT);
                blindLower = extractLowerCouplet(raw);
                blindOk = Boolean.TRUE.equals(checkCouplet(upper, raw).get("passed"));
            } catch (Exception e) {
                log.warn("[paired] 第{}条盲写失败：{}", i + 1, e.getMessage());
                blindLower = "ERROR";
            }
            long bMs = (System.nanoTime() - t0) / 1_000_000;
            blindMs += bMs;

            Thread.sleep(300);

            // B. 反馈重写：把基线输出的 issues 反馈给模型
            String guardLower = "";
            boolean guardOk = false;
            long t1 = System.nanoTime();
            try {
                String raw = aiModelRegistry.chatByModule(
                        buildCoupletRewritePrompt(upper, baseCheck), MODULE_CODE, SYSTEM_PROMPT);
                guardLower = extractLowerCouplet(raw);
                guardOk = Boolean.TRUE.equals(checkCouplet(upper, raw).get("passed"));
            } catch (Exception e) {
                log.warn("[paired] 第{}条反馈重写失败：{}", i + 1, e.getMessage());
                guardLower = "ERROR";
            }
            long gMs = (System.nanoTime() - t1) / 1_000_000;
            guardMs += gMs;

            if (blindOk) {
                blindFix++;
            }
            if (guardOk) {
                guardFix++;
            }

            out.add(String.join(",", escape(upper), escape(baseLower), escape(issues),
                    escape(blindLower), String.valueOf(blindOk),
                    escape(guardLower), String.valueOf(guardOk),
                    String.valueOf(bMs), String.valueOf(gMs)));

            log.info("[paired] {}/{} upper={} blind={} guard={}", i + 1, failed.size(), upper, blindOk, guardOk);
            Thread.sleep(300);
        }

        int n = failed.size();
        out.add(String.format("# 汇总,基线不合格=%d,盲写修复=%d,反馈修复=%d,盲写均耗时=%.0fms,反馈均耗时=%.0fms",
                n, blindFix, guardFix,
                n > 0 ? blindMs * 1.0 / n : 0, n > 0 ? guardMs * 1.0 / n : 0));

        Files.createDirectories(OUT_CSV.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(OUT_CSV, StandardCharsets.UTF_8)) {
            for (String line : out) {
                w.write(line);
                w.newLine();
            }
        }
        log.info("[paired] 完成：盲写修复 {}/{}, 反馈修复 {}/{}", blindFix, n, guardFix, n);
    }

    // ================= 以下方法逐字复刻 AiServiceImpl 的私有实现 =================

    private String buildCoupletPrompt(String upperCouplet) {
        return "请为以下上联对出下联：\n\n上联：" + upperCouplet + "\n\n请给出下联，并简要说明对仗思路。";
    }

    private String buildCoupletRewritePrompt(String upper, Map<String, Object> check) {
        return "上联：" + upper + "\n\n你上一版下联存在以下问题：\n- " + String.join("\n- ", issuesOf(check))
                + "\n请重新对出下联：上下联字数相等，上联仄收、下联平收，词性相对、结构相当，"
                + "避免三平尾与三仄尾。先给出下联，再简要说明对仗思路。";
    }

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

    private String extractLowerCouplet(String raw) {
        if (raw == null || raw.isEmpty()) {
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

    private String lastTone(String text) {
        Map<String, Object> check = pingZeService.checkTone(text);
        List<Map<String, Object>> lines = linesOf(check);
        if (lines.isEmpty()) {
            return "";
        }
        String pattern = String.valueOf(lines.get(lines.size() - 1).get("pattern"));
        return pattern.isEmpty() ? "" : pattern.substring(pattern.length() - 1);
    }

    @SuppressWarnings("unchecked")
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

    private static String escape(String value) {
        return value == null ? "" : value.replace(",", "，").replace("\"", "'");
    }
}
