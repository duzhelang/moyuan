package com.moyuan.util;

import com.moyuan.entity.Poet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 诗人数据提取工具类
 * 用于从文本中提取生卒年、出生地等信息
 */
public class PoetDataExtractor {

    private PoetDataExtractor() {
    }

    /**
     * 从文本中提取生卒年
     * 支持格式：701年－762年、约984年—约1053年、前340年-前278年
     * 支持单独出生年：生于701年、生于前340年
     * 支持单独去世年：卒于762年、去世于762年
     */
    public static void extractYears(String content, Poet poet) {
        if (content == null || poet == null) {
            return;
        }

        Pattern pattern = Pattern.compile(
                "(?:约|大约)?\\s*(前)?(\\d{3,4})\\s*年\\s*[－—\\-~～至]\\s*(?:约|大约)?\\s*(前)?(\\d{3,4})\\s*年");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            int birthYear = Integer.parseInt(matcher.group(2));
            int deathYear = Integer.parseInt(matcher.group(4));
            if ("前".equals(matcher.group(1))) birthYear = -birthYear;
            if ("前".equals(matcher.group(3))) deathYear = -deathYear;
            poet.setBirthYear(birthYear);
            poet.setDeathYear(deathYear);
            return;
        }

        Pattern birthPattern = Pattern.compile(
                "(?:生于|出生于|出生)\\s*(?:约|大约)?\\s*(前)?(\\d{3,4})\\s*年");
        Matcher birthMatcher = birthPattern.matcher(content);
        if (birthMatcher.find()) {
            int birthYear = Integer.parseInt(birthMatcher.group(2));
            if ("前".equals(birthMatcher.group(1))) birthYear = -birthYear;
            poet.setBirthYear(birthYear);
        }

        Pattern deathPattern = Pattern.compile(
                "(?:卒于|去世于|卒|逝世于)\\s*(?:约|大约)?\\s*(前)?(\\d{3,4})\\s*年");
        Matcher deathMatcher = deathPattern.matcher(content);
        if (deathMatcher.find()) {
            int deathYear = Integer.parseInt(deathMatcher.group(2));
            if ("前".equals(deathMatcher.group(1))) deathYear = -deathYear;
            poet.setDeathYear(deathYear);
        }
    }

    /**
     * 从文本中提取出生地
     */
    public static void extractBirthplace(String content, Poet poet) {
        if (content == null || poet == null) {
            return;
        }

        Pattern pattern = Pattern.compile("(?:生于|出生于|出生地[：:])\\s*(.{2,10}?)(?:[，,。]|\\s|$)");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            poet.setBirthplace(matcher.group(1).trim());
        }
    }

    /**
     * 去除HTML标签
     */
    public static String stripHtml(String html) {
        if (html == null) return null;
        return html.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
    }
}
