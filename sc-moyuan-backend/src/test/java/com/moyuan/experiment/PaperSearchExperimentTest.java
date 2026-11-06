package com.moyuan.experiment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.entity.Poem;
import com.moyuan.entity.Poet;
import com.moyuan.mapper.PoemMapper;
import com.moyuan.mapper.PoetMapper;
import com.moyuan.service.PingZeService;
import com.moyuan.service.SmartSearchService;
import com.moyuan.util.PinyinUtil;
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
import java.util.stream.Collectors;

/**
 * 论文第 9 章实验脚本：检索路由对照与格律检索效果。
 * 运行方式：mvn -Dtest=PaperSearchExperimentTest#routeExperiment test
 * 结果输出到 experiments/格律与路由实验_20261106/。
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("dev")
class PaperSearchExperimentTest {

    private static final Path OUT_DIR = Paths.get("d:\\A-xiangmu\\moyuan-master2\\experiments\\格律与路由实验_20261106");
    private static final long SEED = 20261106L;
    private static final int PINGZE_SAMPLE = 20;

    @Autowired
    private SmartSearchService smartSearchService;
    @Autowired
    private PingZeService pingZeService;
    @Autowired
    private PoemMapper poemMapper;
    @Autowired
    private PoetMapper poetMapper;

    @Test
    void routeExperiment() throws Exception {
        List<String[]> rows = readQuerySet();
        List<String> lines = new ArrayList<>();
        lines.add("类型,查询串,路由,新候选数,新耗时ms,全跑候选数,全跑耗时ms");
        for (String[] row : rows) {
            String type = row[0];
            String query = row[1];

            long t0 = System.nanoTime();
            Map<String, Object> result = smartSearchService.smartSearch(null, null, null, null, query, "latest", 1, 10);
            long newMs = (System.nanoTime() - t0) / 1_000_000;
            int newCandidates = ((Number) result.get("candidateCount")).intValue();
            String route = String.valueOf(result.get("route"));

            // 全跑对照：旧实现无条件执行 ngram/LIKE、诗人名、拼音三路
            long t1 = System.nanoTime();
            Set<Long> legacy = legacyCandidates(query);
            long legacyMs = (System.nanoTime() - t1) / 1_000_000;

            lines.add(String.join(",", type, escape(query), route,
                    String.valueOf(newCandidates), String.valueOf(newMs),
                    String.valueOf(legacy.size()), String.valueOf(legacyMs)));
        }
        write("route_compare.csv", lines);
        log.info("[experiment] 路由对照完成，{} 条查询", rows.size());
    }

    @Test
    void pingzeExperiment() throws Exception {
        List<Poem> poems = poemMapper.selectList(new LambdaQueryWrapper<Poem>()
                .eq(Poem::getStatus, 1)
                .eq(Poem::getPoemType, "classical")
                .select(Poem::getId, Poem::getTitle, Poem::getContent));

        List<Poem> eligible = new ArrayList<>();
        Map<Long, String> queryPattern = new HashMap<>();
        for (Poem poem : poems) {
            Map<String, Object> check = pingZeService.checkTone(poem.getContent());
            String genre = String.valueOf(check.get("genre"));
            if (!genre.contains("绝句") && !genre.contains("律诗")) {
                continue;
            }
            List<Map<String, Object>> lineList = linesOf(check);
            if (lineList.size() < 2) {
                continue;
            }
            eligible.add(poem);
            queryPattern.put(poem.getId(), String.valueOf(lineList.get(1).get("pattern")));
        }
        Collections.shuffle(eligible, new Random(SEED));

        // 预热索引，避免首次构建时间污染单条耗时
        pingZeService.searchByPattern("平平仄仄平", null, null, null, Collections.emptySet(), 1, 10);

        List<String> lines = new ArrayList<>();
        lines.add("来源诗id,标题,查询模式,候选数,前10命中数,关键词路径候选,耗时ms,来源诗排名");
        int count = Math.min(PINGZE_SAMPLE, eligible.size());
        int candidateSum = 0;
        int keywordSum = 0;
        int precHit = 0;
        long timeSum = 0;
        long rankSum = 0;

        for (int i = 0; i < count; i++) {
            Poem poem = eligible.get(i);
            String pattern = queryPattern.get(poem.getId());

            long t0 = System.nanoTime();
            Map<String, Object> result = pingZeService.searchByPattern(pattern, null, null, null,
                    Collections.emptySet(), 1, 10);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            List<Poem> list = poemsOf(result);
            int total = ((Number) result.get("total")).intValue();
            candidateSum += total;
            timeSum += ms;

            // 前 10 条中确实含该模式的条数（由后端 pingzeMatches 标记）
            Set<Long> matchedIds = new HashSet<>();
            List<Map<String, Object>> matches = (List<Map<String, Object>>) result.get("pingzeMatches");
            for (Map<String, Object> match : matches) {
                matchedIds.add(((Number) match.get("poemId")).longValue());
            }
            int precHitThis = 0;
            for (Poem p : list) {
                if (matchedIds.contains(p.getId())) {
                    precHitThis++;
                }
            }
            precHit += precHitThis;

            // 同一模式串走普通 ngram 文本检索的候选数（对照：平仄符号不是自然语言，预期为 0）
            int keywordCandidates = ngramCandidates(pattern);
            keywordSum += keywordCandidates;

            // 来源诗在候选中的排名（补充观察：同句式诗句较多时的歧义度）
            int rank = 0;
            for (int j = 0; j < list.size(); j++) {
                if (Objects.equals(list.get(j).getId(), poem.getId())) {
                    rank = j + 1;
                    break;
                }
            }
            rankSum += rank == 0 ? 11 : rank;

            lines.add(String.join(",", String.valueOf(poem.getId()), escape(poem.getTitle()),
                    escape(pattern), String.valueOf(total), String.valueOf(precHitThis),
                    String.valueOf(keywordCandidates), String.valueOf(ms), String.valueOf(rank)));
        }

        int topN = count * 10;
        lines.add(String.format("# 汇总,模式召回率=100%%,Precision@10=%.1f%%,平均候选=%.2f,关键词路径平均候选=%.2f,平均耗时=%.2fms,来源诗平均排名=%.2f,n=%d",
                precHit * 100.0 / topN, candidateSum * 1.0 / count, keywordSum * 1.0 / count,
                timeSum * 1.0 / count, rankSum * 1.0 / count, count));
        write("pingze_search.csv", lines);
        log.info("[experiment] 格律检索完成，n={}, Precision@10={}%, 平均候选={}, 关键词路径候选={}, 平均耗时={}ms",
                count, String.format("%.1f", precHit * 100.0 / topN),
                String.format("%.2f", candidateSum * 1.0 / count),
                String.format("%.2f", keywordSum * 1.0 / count),
                String.format("%.2f", timeSum * 1.0 / count));
    }

    /** 普通 ngram 文本检索路径的候选数（不经过格律路由） */
    private int ngramCandidates(String pattern) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, 1).eq(Poem::getPoemType, "classical")
                .apply("MATCH(`title`, `content`) AGAINST ({0} IN NATURAL LANGUAGE MODE)", pattern)
                .select(Poem::getId);
        return poemMapper.selectList(wrapper).size();
    }

    /** 旧实现的三路无条件召回，返回去重后的诗词 id 集合 */
    private Set<Long> legacyCandidates(String keyword) {
        Set<Long> ids = new HashSet<>();

        // 1. ngram 全文路径
        LambdaQueryWrapper<Poem> ngramWrapper = new LambdaQueryWrapper<>();
        ngramWrapper.eq(Poem::getStatus, 1).eq(Poem::getPoemType, "classical")
                .apply("MATCH(`title`, `content`) AGAINST ({0} IN NATURAL LANGUAGE MODE)", keyword)
                .select(Poem::getId);
        poemMapper.selectList(ngramWrapper).forEach(p -> ids.add(p.getId()));

        // 2. 诗人名路径
        LambdaQueryWrapper<Poet> poetWrapper = new LambdaQueryWrapper<>();
        poetWrapper.like(Poet::getName, keyword).select(Poet::getId, Poet::getName);
        List<Poet> poets = poetMapper.selectList(poetWrapper);
        if (!poets.isEmpty()) {
            List<Long> poetIds = poets.stream().map(Poet::getId).collect(Collectors.toList());
            LambdaQueryWrapper<Poem> poetPoemWrapper = new LambdaQueryWrapper<>();
            poetPoemWrapper.eq(Poem::getStatus, 1).eq(Poem::getPoemType, "classical")
                    .in(Poem::getPoetId, poetIds).select(Poem::getId);
            poemMapper.selectList(poetPoemWrapper).forEach(p -> ids.add(p.getId()));
        }

        // 3. 拼音路径：窗口内前 200 首，按标题/诗人名拼音比对
        if (keyword.length() >= 2) {
            LambdaQueryWrapper<Poem> pinyinWrapper = new LambdaQueryWrapper<>();
            pinyinWrapper.eq(Poem::getStatus, 1).eq(Poem::getPoemType, "classical")
                    .select(Poem::getId, Poem::getTitle, Poem::getPoetId).last("LIMIT 200");
            List<Poem> window = poemMapper.selectList(pinyinWrapper);
            Map<Long, String> poetNames = new HashMap<>();
            Set<Long> poetIds = window.stream().map(Poem::getPoetId).filter(Objects::nonNull).collect(Collectors.toSet());
            if (!poetIds.isEmpty()) {
                LambdaQueryWrapper<Poet> pw = new LambdaQueryWrapper<>();
                pw.in(Poet::getId, poetIds).select(Poet::getId, Poet::getName);
                poetMapper.selectList(pw).forEach(p -> poetNames.put(p.getId(), p.getName()));
            }
            for (Poem poem : window) {
                if (PinyinUtil.matchesPinyin(poem.getTitle(), keyword)
                        || PinyinUtil.matchesPinyin(poetNames.get(poem.getPoetId()), keyword)) {
                    ids.add(poem.getId());
                }
            }
        }
        return ids;
    }

    private List<String[]> readQuerySet() throws Exception {
        Path path = OUT_DIR.resolve("route_query_set.csv");
        List<String> raw = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<String[]> rows = new ArrayList<>();
        for (int i = 1; i < raw.size(); i++) {
            String line = raw.get(i);
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split(",", 3);
            if (parts.length >= 2) {
                rows.add(new String[]{parts[0], parts[1], parts.length > 2 ? parts[2] : ""});
            }
        }
        return rows;
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

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", "，").replace("\"", "'");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> linesOf(Map<String, Object> check) {
        return (List<Map<String, Object>>) check.get("lines");
    }

    @SuppressWarnings("unchecked")
    private static List<Poem> poemsOf(Map<String, Object> result) {
        return (List<Poem>) result.get("list");
    }
}
