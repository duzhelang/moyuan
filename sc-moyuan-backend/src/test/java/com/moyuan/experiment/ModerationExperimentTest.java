package com.moyuan.experiment;

import com.moyuan.service.AiModerationService;
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

/**
 * 论文内容风控评测实验（2026-11-08）。
 *
 * 走生产链路：AiModerationService.analyze() -> ai_module_config(content_moderation) 的提示词
 * -> 默认模型（deepseek-chat）-> JSON 解析 -> ModerationResult。
 * 评测集 150 条（正常 70 / 广告 20 / 辱骂 20 / 低俗 15 / 抄袭 15 / 违法 10），人工标注三级风险。
 *
 * 统计口径：
 *  - 二分类（是否需人工复核）：medium+high 视为正例，与标注正例比较，得 Acc/P/R/FPR/FNR；
 *  - 三分类：riskLevel 与标注等级完全一致的比例 + 3x3 混淆矩阵；
 *  - 分类别：标注类别是否出现在 categories 中；
 *  - 运营口径：建单量（medium+high）与限流量（high）占全量比例，即人工复核量相对全量审核的下降；
 *  - 时延：单条 analyze 的耗时 P50/P95/均值。
 *
 * 运行：mvn -Dtest=ModerationExperimentTest test
 * 先跑小样本：mvn -Dtest=ModerationExperimentTest -Dlimit=15 test
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("dev")
class ModerationExperimentTest {

    private static final Path SAMPLE_CSV = Paths.get(
            "d:\\A-xiangmu\\moyuan-master2\\experiments\\风控实验_20261108\\moderation_samples.csv");
    private static final Path OUT_CSV = Paths.get(
            "d:\\A-xiangmu\\moyuan-master2\\experiments\\风控实验_20261108\\moderation_result.csv");
    private static final Path OUT_MD = Paths.get(
            "d:\\A-xiangmu\\moyuan-master2\\experiments\\风控实验_20261108\\moderation_summary.md");

    @Autowired
    private AiModerationService aiModerationService;

    @Test
    void moderationExperiment() throws Exception {
        int limit = Integer.getInteger("limit", Integer.MAX_VALUE);
        List<String[]> samples = new ArrayList<>();
        for (String line : Files.readAllLines(SAMPLE_CSV, StandardCharsets.UTF_8)) {
            line = line.replace("\uFEFF", "");
            if (line.isBlank() || line.startsWith("id,")) {
                continue;
            }
            String[] f = line.split(",", 4);
            if (f.length < 4) {
                continue;
            }
            samples.add(f);
            if (samples.size() >= limit) {
                break;
            }
        }
        log.info("[moderation] 样本 {} 条", samples.size());

        List<String> rows = new ArrayList<>();
        rows.add("id,expect_level,expect_cat,pred_level,cats,reason,level_match,cat_hit,ms");

        int tp = 0, fp = 0, tn = 0, fn = 0, levelExact = 0, catHit = 0, catTotal = 0;
        int predHigh = 0;
        Map<String, int[]> confusion = new LinkedHashMap<>();
        Map<String, int[]> byCat = new LinkedHashMap<>();
        List<Long> latencies = new ArrayList<>();

        for (String[] s : samples) {
            String id = s[0].trim();
            String expectLevel = s[1].trim();
            String expectCat = s[2].trim();
            String content = s[3].trim();

            long t0 = System.nanoTime();
            AiModerationService.ModerationResult r;
            String err = "";
            try {
                r = aiModerationService.analyze("comment", null, content);
            } catch (Exception e) {
                err = e.getClass().getSimpleName();
                r = new AiModerationService.ModerationResult("low", List.of(), "");
            }
            long ms = (System.nanoTime() - t0) / 1_000_000;
            latencies.add(ms);

            String predLevel = r.riskLevel();
            String cats = String.join("|", r.categories());
            boolean expectRisky = !"low".equals(expectLevel);
            boolean predRisky = !"low".equals(predLevel);
            if (expectRisky && predRisky) {
                tp++;
            } else if (!expectRisky && predRisky) {
                fp++;
            } else if (!expectRisky) {
                tn++;
            } else {
                fn++;
            }
            boolean match = expectLevel.equals(predLevel);
            if (match) {
                levelExact++;
            }
            if ("high".equals(predLevel)) {
                predHigh++;
            }
            confusion.computeIfAbsent(expectLevel, k -> new int[3])[idx(predLevel)]++;

            boolean hit;
            if ("none".equals(expectCat)) {
                hit = "low".equals(predLevel);
            } else {
                hit = r.categories().contains(expectCat);
                catTotal++;
                if (hit) {
                    catHit++;
                }
            }
            byCat.computeIfAbsent(expectCat, k -> new int[2]);
            byCat.get(expectCat)[0]++;
            if (hit) {
                byCat.get(expectCat)[1]++;
            }

            rows.add(String.join(",", id, expectLevel, expectCat, predLevel,
                    esc(cats), esc(r.reason()), String.valueOf(match), String.valueOf(hit),
                    String.valueOf(ms)));
            log.info("[moderation] {}/{} {} expect={} pred={} cats={} {}ms {}",
                    rows.size() - 1, samples.size(), id, expectLevel, predLevel, cats, ms, err);
            Thread.sleep(200);
        }

        int n = samples.size();
        double acc = (tp + tn) * 1.0 / n;
        double precision = tp + fp == 0 ? 0 : tp * 1.0 / (tp + fp);
        double recall = tp + fn == 0 ? 0 : tp * 1.0 / (tp + fn);
        double fpr = fp + tn == 0 ? 0 : fp * 1.0 / (fp + tn);
        double fnr = tp + fn == 0 ? 0 : fn * 1.0 / (tp + fn);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);

        latencies.sort(Long::compareTo);
        long p50 = latencies.get((int) Math.ceil(0.50 * n) - 1);
        long p95 = latencies.get((int) Math.ceil(0.95 * n) - 1);
        double mean = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        long max = latencies.get(n - 1);

        StringBuilder md = new StringBuilder();
        md.append("# 内容风控评测结果（").append(n).append(" 条）\n\n");
        md.append("模型：content_moderation 未绑定专用模型，走系统默认模型 deepseek-chat；提示词取自 ai_module_config.content_moderation。\n\n");
        md.append("## 二分类（medium+high 视为需人工复核）\n\n");
        md.append("| 指标 | 值 |\n|---|---|\n");
        md.append(String.format("| 准确率 Acc | %.3f |%n", acc));
        md.append(String.format("| 精确率 P | %.3f |%n", precision));
        md.append(String.format("| 召回率 R | %.3f |%n", recall));
        md.append(String.format("| F1 | %.3f |%n", f1));
        md.append(String.format("| 误报率 FPR | %.3f |%n", fpr));
        md.append(String.format("| 漏报率 FNR | %.3f |%n", fnr));
        md.append(String.format("%n混淆：TP=%d FP=%d TN=%d FN=%d%n%n", tp, fp, tn, fn));
        md.append("## 三分类一致率\n\n");
        md.append(String.format("等级完全一致 %d/%d = %.3f%n%n", levelExact, n, levelExact * 1.0 / n));
        md.append("| 标注\\预测 | low | medium | high |\n|---|---|---|---|\n");
        for (String lv : List.of("low", "medium", "high")) {
            int[] row = confusion.getOrDefault(lv, new int[3]);
            md.append(String.format("| %s | %d | %d | %d |%n", lv, row[0], row[1], row[2]));
        }
        md.append("\n## 分类别命中（标注类别是否出现在 categories）\n\n");
        md.append("| 类别 | 条数 | 命中 | 命中率 |\n|---|---|---|---|\n");
        for (Map.Entry<String, int[]> e : byCat.entrySet()) {
            md.append(String.format("| %s | %d | %d | %.3f |%n",
                    e.getKey(), e.getValue()[0], e.getValue()[1],
                    e.getValue()[1] * 1.0 / e.getValue()[0]));
        }
        md.append("\n## 运营口径\n\n");
        md.append(String.format("建单量（medium+high）%d/%d = %.1f%%，人工复核量相对全量审核下降 %.1f%%；限流量（high）%d/%d = %.1f%%。%n",
                tp + fp, n, (tp + fp) * 100.0 / n, 100 - (tp + fp) * 100.0 / n,
                predHigh, n, predHigh * 100.0 / n));
        md.append(String.format("漏放（标注需复核但判 low，无人复核）%d 条，占标注正例 %.1f%%。%n%n", fn, fn * 100.0 / (tp + fn)));
        md.append("## 时延（单条 analyze，含模型调用）\n\n");
        md.append(String.format("P50 %d ms，P95 %d ms，均值 %.0f ms，最大 %d ms。%n", p50, p95, mean, max));
        md.append(String.format("分类别命中率合计 %d/%d = %.3f。%n", catHit, catTotal, catTotal == 0 ? 0 : catHit * 1.0 / catTotal));

        Files.createDirectories(OUT_CSV.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(OUT_CSV, StandardCharsets.UTF_8)) {
            for (String row : rows) {
                w.write(row);
                w.newLine();
            }
        }
        Files.writeString(OUT_MD, md.toString(), StandardCharsets.UTF_8);
        log.info("[moderation] 完成 Acc={} P={} R={} FPR={} 复核量={}/{} 时延P50={}ms P95={}ms",
                String.format("%.3f", acc), String.format("%.3f", precision), String.format("%.3f", recall),
                String.format("%.3f", fpr), tp + fp, n, p50, p95);
    }

    private int idx(String level) {
        return switch (level) {
            case "low" -> 0;
            case "medium" -> 1;
            default -> 2;
        };
    }

    private String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace(",", "，").replace("\n", " ");
    }
}
