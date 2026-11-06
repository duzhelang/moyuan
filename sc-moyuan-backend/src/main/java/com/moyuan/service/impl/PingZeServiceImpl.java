package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.entity.Poem;
import com.moyuan.mapper.PoemMapper;
import com.moyuan.service.PingZeService;
import com.moyuan.service.PoemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 平仄服务实现。
 *
 * <p>核心思路：以字为粒度把拼音声调映射为「平/仄/中」，再按句拼接成平仄串；
 * 格律检索则是把全库古典诗词预先切句并生成平仄串，构建内存倒排索引后做通配子串匹配。
 * 索引懒加载并每 10 分钟自动重建，兼顾检索性能与数据新鲜度。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PingZeServiceImpl implements PingZeService {

    private final PoemMapper poemMapper;
    private final PoemService poemService;

    private static final String PING = "平";
    private static final String ZE = "仄";
    private static final String ANY = "中";

    /** 带声调拼音输出格式（数字表示声调） */
    private static final HanyuPinyinOutputFormat TONE_FORMAT = new HanyuPinyinOutputFormat();

    static {
        TONE_FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        TONE_FORMAT.setToneType(HanyuPinyinToneType.WITH_TONE_NUMBER);
        TONE_FORMAT.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    /** 句读切分：中英文标点与换行都视作句子边界 */
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("[，。！？；、,.!?;\\n\\r]+");
    /** 格律查询串：仅允许「平/仄/中」与空白、竖线分隔 */
    private static final Pattern PINGZE_QUERY = Pattern.compile("[平仄中\\s|]+");
    /** 常见入声字（普通话多读阴平/阳平，但在平水韵中属仄声） */
    private static final Set<Character> RU_SHENG = buildRuSheng();

    private static final int MIN_PINGZE_QUERY_LENGTH = 2;
    private static final int INDEX_TTL_MILLIS = 10 * 60 * 1000;
    private static final int STATUS_ACTIVE = 1;

    /** 平仄索引：每行一句，保留所属诗词引用与平仄串 */
    private record IndexedLine(Poem poem, String line, String pattern) {}

    private volatile List<IndexedLine> index = Collections.emptyList();
    private volatile long indexBuiltAt = 0L;

    @Override
    public boolean isPingZeQuery(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return false;
        }
        if (!PINGZE_QUERY.matcher(keyword).matches()) {
            return false;
        }
        long symbolCount = keyword.chars()
                .filter(c -> c == '平' || c == '仄' || c == '中')
                .count();
        return symbolCount >= MIN_PINGZE_QUERY_LENGTH;
    }

    @Override
    public Map<String, Object> checkTone(String text) {
        List<String> sentences = splitSentences(text);
        List<Map<String, Object>> lines = new ArrayList<>();
        List<String> allIssues = new ArrayList<>();

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            String pattern = patternOf(sentence);
            List<Map<String, Object>> chars = annotateChars(sentence);
            List<String> issues = checkSentence(pattern);
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("text", sentence);
            line.put("pattern", pattern);
            line.put("chars", chars);
            line.put("issues", issues);
            lines.add(line);
            for (String issue : issues) {
                allIssues.add("第" + (i + 1) + "句" + issue);
            }
        }

        // 偶数句押韵检查（近体诗偶数句末字须同韵且平收）
        List<String> rhymeIssues = checkRhyme(sentences);
        allIssues.addAll(rhymeIssues);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("passed", allIssues.isEmpty());
        result.put("genre", detectGenre(sentences));
        result.put("issues", allIssues);
        result.put("lines", lines);
        return result;
    }

    @Override
    public Map<String, Object> searchByPattern(String pattern, Long dynastyId, Long categoryId, Long poetId,
                                                Set<Long> poetNameIds, int pageNum, int pageSize) {
        String query = normalizeQuery(pattern);
        Map<Long, List<Map<String, Object>>> matchesByPoem = new LinkedHashMap<>();
        Map<Long, Poem> poemMap = new HashMap<>();

        if (query.isEmpty() || query.length() < MIN_PINGZE_QUERY_LENGTH) {
            return emptyResult();
        }

        for (IndexedLine indexed : ensureIndex()) {
            Poem poem = indexed.poem();
            if (!passFilter(poem, dynastyId, categoryId, poetId, poetNameIds)) {
                continue;
            }
            // 「中」为通配符，indexOf 无法处理，改用逐位比对
            int start = wildcardIndexOf(indexed.pattern(), query);
            if (start < 0) {
                continue;
            }
            Map<String, Object> match = new LinkedHashMap<>();
            match.put("poemId", poem.getId());
            match.put("line", indexed.line());
            match.put("plainLine", indexed.line());
            match.put("pattern", indexed.pattern());
            match.put("start", start);
            matchesByPoem.computeIfAbsent(poem.getId(), k -> new ArrayList<>()).add(match);
            poemMap.put(poem.getId(), poem);
        }

        // 命中的诗词按命中句数降序排列
        List<Map.Entry<Long, List<Map<String, Object>>>> entries = new ArrayList<>(matchesByPoem.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));

        int total = entries.size();
        int fromIndex = Math.max(0, (pageNum - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<Poem> pagePoems = new ArrayList<>();
        List<Map<String, Object>> pageMatches = new ArrayList<>();
        if (fromIndex < total) {
            for (int i = fromIndex; i < toIndex; i++) {
                Map.Entry<Long, List<Map<String, Object>>> entry = entries.get(i);
                pagePoems.add(poemMap.get(entry.getKey()));
                pageMatches.addAll(entry.getValue());
            }
        }
        poemService.populatePoetAndDynasty(pagePoems);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", pagePoems);
        result.put("total", total);
        result.put("pingzeMatches", pageMatches);
        log.debug("[pingze] pattern={}, poems={}", query, total);
        return result;
    }

    /** 按筛选条件过滤诗词 */
    private boolean passFilter(Poem poem, Long dynastyId, Long categoryId, Long poetId, Set<Long> poetNameIds) {
        if (dynastyId != null && !Objects.equals(poem.getDynastyId(), dynastyId)) {
            return false;
        }
        if (categoryId != null && !Objects.equals(poem.getCategoryId(), categoryId)) {
            return false;
        }
        if (poetId != null && !Objects.equals(poem.getPoetId(), poetId)
                && !(poetNameIds != null && poetNameIds.contains(poem.getPoetId()))) {
            return false;
        }
        if (poetId == null && poetNameIds != null && !poetNameIds.isEmpty()
                && !poetNameIds.contains(poem.getPoetId())) {
            return false;
        }
        return true;
    }

    /** 懒加载平仄索引，超过 TTL 后自动重建 */
    private List<IndexedLine> ensureIndex() {
        long now = System.currentTimeMillis();
        if (!index.isEmpty() && now - indexBuiltAt < INDEX_TTL_MILLIS) {
            return index;
        }
        synchronized (this) {
            if (!index.isEmpty() && System.currentTimeMillis() - indexBuiltAt < INDEX_TTL_MILLIS) {
                return index;
            }
            List<IndexedLine> built = buildIndex();
            index = built;
            indexBuiltAt = System.currentTimeMillis();
            log.info("[pingze] 平仄索引已构建，句数={}", built.size());
            return built;
        }
    }

    private List<IndexedLine> buildIndex() {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, STATUS_ACTIVE)
                .eq(Poem::getPoemType, "classical")
                .select(Poem::getId, Poem::getTitle, Poem::getContent,
                        Poem::getDynastyId, Poem::getCategoryId, Poem::getPoetId);
        List<Poem> poems = poemMapper.selectList(wrapper);
        List<IndexedLine> lines = new ArrayList<>();
        for (Poem poem : poems) {
            if (!StringUtils.hasText(poem.getContent())) {
                continue;
            }
            for (String sentence : splitSentences(poem.getContent())) {
                String pattern = patternOf(sentence);
                if (pattern.length() >= MIN_PINGZE_QUERY_LENGTH) {
                    lines.add(new IndexedLine(poem, sentence, pattern));
                }
            }
        }
        return lines;
    }

    /** 把查询串规整为纯「平仄中」序列 */
    private String normalizeQuery(String pattern) {
        if (!StringUtils.hasText(pattern)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            if (c == '平' || c == '仄' || c == '中') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 支持「中」通配的子串查找 */
    private int wildcardIndexOf(String text, String query) {
        int limit = text.length() - query.length();
        for (int i = 0; i <= limit; i++) {
            boolean matched = true;
            for (int j = 0; j < query.length(); j++) {
                char q = query.charAt(j);
                if (q == '中') {
                    continue;
                }
                if (text.charAt(i + j) != q) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return i;
            }
        }
        return -1;
    }

    private List<String> splitSentences(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }
        List<String> sentences = new ArrayList<>();
        for (String raw : SENTENCE_SPLIT.split(text)) {
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    /** 生成一句的平仄串（只统计汉字，非汉字跳过） */
    private String patternOf(String sentence) {
        StringBuilder sb = new StringBuilder();
        for (char c : sentence.toCharArray()) {
            if (!isChinese(c)) {
                continue;
            }
            sb.append(toneOf(c));
        }
        return sb.toString();
    }

    private List<Map<String, Object>> annotateChars(String sentence) {
        List<Map<String, Object>> chars = new ArrayList<>();
        for (char c : sentence.toCharArray()) {
            if (!isChinese(c)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("char", String.valueOf(c));
            item.put("pingze", toneOf(c));
            item.put("pinyin", pinyinWithoutTone(c));
            chars.add(item);
        }
        return chars;
    }

    /** 单字平仄判定：入声表优先，其次按普通话声调，多音平仄并存判为「中」 */
    private String toneOf(char c) {
        if (RU_SHENG.contains(c)) {
            return ZE;
        }
        String[] pinyinArray;
        try {
            pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, TONE_FORMAT);
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            return ANY;
        }
        if (pinyinArray == null || pinyinArray.length == 0) {
            return ANY;
        }
        boolean hasPing = false;
        boolean hasZe = false;
        for (String pinyin : pinyinArray) {
            if (pinyin == null || pinyin.isEmpty()) {
                continue;
            }
            char toneDigit = pinyin.charAt(pinyin.length() - 1);
            if (toneDigit == '1' || toneDigit == '2') {
                hasPing = true;
            } else if (toneDigit == '3' || toneDigit == '4') {
                hasZe = true;
            }
        }
        if (hasPing && hasZe) {
            return ANY;
        }
        if (hasPing) {
            return PING;
        }
        if (hasZe) {
            return ZE;
        }
        return ANY;
    }

    /** 单句校验：三平尾/三仄尾 + 二四六位平仄失替 */
    private List<String> checkSentence(String pattern) {
        List<String> issues = new ArrayList<>();
        if (pattern.length() < 3) {
            return issues;
        }
        if (pattern.endsWith("平平平")) {
            issues.add("句末出现三平尾，近体诗须避免");
        }
        if (pattern.endsWith("仄仄仄")) {
            issues.add("句末出现三仄尾，近体诗须避免");
        }
        // 一三五不论、二四六分明：只检查偶数位（索引 1、3、5）
        String previous = null;
        for (int i = 1; i < pattern.length(); i += 2) {
            String current = String.valueOf(pattern.charAt(i));
            if (ANY.equals(current)) {
                continue;
            }
            if (previous != null && previous.equals(current)) {
                issues.add("第" + (i + 1) + "字与上一节拍平仄失替");
            }
            previous = current;
        }
        return issues;
    }

    /** 偶数句押韵检查：以第 2 句末字为基准，要求同韵母且平收 */
    private List<String> checkRhyme(List<String> sentences) {
        List<String> issues = new ArrayList<>();
        if (sentences.size() < 2) {
            return issues;
        }
        String basePinyin = lastCharPinyin(sentences.get(1));
        if (basePinyin.isEmpty()) {
            return issues;
        }
        String baseFinal = rhymeClass(finalOf(basePinyin));
        String baseTone = toneOf(lastChineseChar(sentences.get(1)));
        if (!PING.equals(baseTone)) {
            issues.add("第2句末字非平声，近体诗偶数句须平收押韵");
        }
        for (int i = 3; i < sentences.size(); i += 2) {
            String pinyin = lastCharPinyin(sentences.get(i));
            if (pinyin.isEmpty()) {
                continue;
            }
            String finalPart = rhymeClass(finalOf(pinyin));
            if (!baseFinal.equals(finalPart)) {
                issues.add("第" + (i + 1) + "句末字与第2句不押韵（" + finalPart + " ≠ " + baseFinal + "）");
            }
        }
        return issues;
    }

    /** 判定体裁：四句/八句 + 五言/七言，其余归为「古风或自由体」 */
    private String detectGenre(List<String> sentences) {
        if (sentences.isEmpty()) {
            return "未知";
        }
        int lineCount = sentences.size();
        int charCount = patternOf(sentences.get(0)).length();
        boolean uniform = true;
        for (String sentence : sentences) {
            if (patternOf(sentence).length() != charCount) {
                uniform = false;
                break;
            }
        }
        if (!uniform) {
            return "古风或自由体";
        }
        if (charCount == 5 && lineCount == 4) {
            return "五言绝句";
        }
        if (charCount == 7 && lineCount == 4) {
            return "七言绝句";
        }
        if (charCount == 5 && lineCount == 8) {
            return "五言律诗";
        }
        if (charCount == 7 && lineCount == 8) {
            return "七言律诗";
        }
        return "古风或自由体";
    }

    private Map<String, Object> emptyResult() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", Collections.emptyList());
        result.put("total", 0);
        result.put("pingzeMatches", Collections.emptyList());
        return result;
    }

    private static boolean isChinese(char c) {
        return c >= '\u4E00' && c <= '\u9FA5';
    }

    private char lastChineseChar(String sentence) {
        for (int i = sentence.length() - 1; i >= 0; i--) {
            if (isChinese(sentence.charAt(i))) {
                return sentence.charAt(i);
            }
        }
        return 0;
    }

    private String lastCharPinyin(String sentence) {
        char last = lastChineseChar(sentence);
        if (last == 0) {
            return "";
        }
        return pinyinWithoutTone(last);
    }

    private String pinyinWithoutTone(char c) {
        try {
            String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, TONE_FORMAT);
            if (pinyinArray == null || pinyinArray.length == 0) {
                return "";
            }
            String pinyin = pinyinArray[0];
            if (!pinyin.isEmpty() && Character.isDigit(pinyin.charAt(pinyin.length() - 1))) {
                return pinyin.substring(0, pinyin.length() - 1);
            }
            return pinyin;
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            return "";
        }
    }

    /** 提取韵母（去声母，近似处理 y/w 的拼写规则，用于押韵比较） */
    private static String finalOf(String pinyin) {
        if (pinyin == null || pinyin.isEmpty()) {
            return "";
        }
        String p = pinyin.toLowerCase();
        String[] initials = {"zh", "ch", "sh", "b", "p", "m", "f", "d", "t", "n", "l", "g", "k", "h",
                "j", "q", "x", "r", "z", "c", "s"};
        for (String initial : initials) {
            if (p.startsWith(initial)) {
                return p.substring(initial.length());
            }
        }
        // y/w 开头的音节：yu→u（ü），yi→i，ye→ie，you→ou，wu→u 等，简化保留剩余部分
        if (p.startsWith("y")) {
            String rest = p.substring(1);
            if (rest.equals("u")) {
                return "u";
            }
            return rest;
        }
        if (p.startsWith("w")) {
            return p.substring(1);
        }
        return p;
    }

    /**
     * 把普通话韵母归并为可通押的韵类。
     * 近体诗按平水韵分韵部，与普通话韵母并非一一对应（如「流」liu 与「楼」lou 同属尤韵），
     * 此处按常见邻韵合并近似处理，仅用于押韵判定，不用于学术级格律分析。
     */
    private static String rhymeClass(String finalPart) {
        if (finalPart == null || finalPart.isEmpty()) {
            return "";
        }
        switch (finalPart) {
            case "ou": case "iu": return "尤";
            case "ong": case "iong": return "东";
            case "ang": case "iang": case "uang": return "阳";
            case "i": case "ei": case "ui": return "支";
            case "u": case "v": return "虞";
            case "ai": case "uai": return "灰";
            case "en": case "in": case "un": return "真";
            case "an": case "ian": case "uan": case "van": return "寒";
            case "ao": case "iao": return "萧";
            case "e": case "o": case "uo": return "歌";
            case "a": case "ia": case "ua": case "ie": return "麻";
            case "eng": case "ing": return "庚";
            default: return finalPart;
        }
    }

    private static Set<Character> buildRuSheng() {
        String chars = "一七八十百白伯薄泊博搏钵北国竹菊独读族鹿谷哭屋木目服福幅复绿玉足曲粟宿"
                + "得特则塞色力翼息直值食识织极急及级疾集习湿什执尺石只赤客拍迫窄格额赫麦脉"
                + "责摘革隔核各落洛络作错索鹤阁壑漠幕莫寞恶昨郭活夺脱括刮滑刷阔渴喝葛割挖拔"
                + "跋杀刹扎札察撤舌设热哲折节切结洁杰别灭列烈裂劣雪绝说血穴决诀缺阙月越阅悦"
                + "发法罚伐袜卒骨忽突窟术述出室日实失吉壹乙逸毕必笔匹密蜜漆悉膝戌恤律率戍拾"
                + "立粒泣吸入汁";
        Set<Character> set = new HashSet<>();
        for (char c : chars.toCharArray()) {
            set.add(c);
        }
        return set;
    }
}
