package com.moyuan.util;

import com.moyuan.entity.Poet;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class PoetDefaultAvatar {

    private static final String AVATAR_PATH = "/img/poet_avatars/";

    private static final Map<String, List<String>> AVATAR_POOL = Map.of(
            "cn_ancient_male", List.of(
                    AVATAR_PATH + "avatar_cn_ancient_male_01.jpg",
                    AVATAR_PATH + "avatar_cn_ancient_male_02.jpg",
                    AVATAR_PATH + "avatar_cn_ancient_male_03.jpg"
            ),
            "cn_ancient_female", List.of(
                    AVATAR_PATH + "avatar_cn_ancient_female_01.jpg"
            ),
            "cn_modern_male", List.of(
                    AVATAR_PATH + "avatar_cn_modern_male_01.jpg"
            ),
            "cn_modern_female", List.of(
                    AVATAR_PATH + "avatar_cn_modern_female_01.jpg"
            ),
            "west_classical_male", List.of(
                    AVATAR_PATH + "avatar_west_classical_male_01.jpg"
            ),
            "west_classical_female", List.of(
                    AVATAR_PATH + "avatar_west_classical_female_01.jpg"
            ),
            "west_romantic_male", List.of(
                    AVATAR_PATH + "avatar_west_romantic_male_01.jpg"
            ),
            "west_romantic_female", List.of(
                    AVATAR_PATH + "avatar_west_romantic_female_01.jpg"
            ),
            "west_modern_female", List.of(
                    AVATAR_PATH + "avatar_west_modern_female_01.jpg"
            )
    );

    private static final Map<String, String> STYLE_MAP = Map.of(
            "豪放派", AVATAR_PATH + "avatar_cn_ancient_male_01.jpg",
            "婉约派", AVATAR_PATH + "avatar_cn_ancient_female_01.jpg",
            "现实主义", AVATAR_PATH + "avatar_cn_ancient_male_02.jpg",
            "田园派", AVATAR_PATH + "avatar_cn_ancient_male_03.jpg",
            "新月派", AVATAR_PATH + "avatar_cn_modern_male_01.jpg",
            "浪漫主义", AVATAR_PATH + "avatar_west_romantic_male_01.jpg",
            "古典主义", AVATAR_PATH + "avatar_west_classical_male_01.jpg"
    );

    // 西方诗人名单（用于区分中西方）
    private static final java.util.Set<String> WESTERN_POETS = java.util.Set.of(
            "莎士比亚", "拜伦", "雪莱", "歌德", "狄金森", "泰戈尔",
            "普希金", "波德莱尔", "聂鲁达", "叶芝", "惠特曼",
            "松尾芭蕉", "但丁", "席勒", "海涅", "雨果",
            "彭斯", "朗费罗", "裴多菲", "纪伯伦"
    );

    // 已知女性诗人
    private static final java.util.Set<String> FEMALE_POETS = java.util.Set.of(
            "李清照", "上官婉儿", "唐婉", "蔡文姬", "卓文君",
            "薛涛", "鱼玄机", "刘采春", "谢道韫", "卫夫人",
            "林徽因", "冰心", "舒婷", "翟永明", "王安忆",
            "狄金森"
    );

    /**
     * 根据诗人信息智能分配预设头像
     */
    public static String getAvatar(Poet poet) {
        String name = poet.getName();
        String poetType = poet.getPoetType();
        Long dynastyId = poet.getDynastyId();

        // 第0级：精确流派匹配
        // 暂不支持，因为Poet没有style字段，跳过

        // 判断性别
        boolean isFemale = FEMALE_POETS.contains(name);

        // 判断是否西方诗人
        boolean isWestern = WESTERN_POETS.contains(name)
                || (dynastyId == null && !"modern".equals(poetType));

        // 判断时期
        boolean isModern = "modern".equals(poetType);

        // 第1级：地区 + 时期 + 性别
        String region = isWestern ? "west" : "cn";
        String period;
        if (isWestern) {
            period = isModern ? "modern" : "classical";
        } else {
            period = isModern ? "modern" : "ancient";
        }
        String gender = isFemale ? "female" : "male";

        String key = region + "_" + period + "_" + gender;
        List<String> candidates = AVATAR_POOL.get(key);
        if (candidates != null && !candidates.isEmpty()) {
            return randomPick(candidates);
        }

        // 第2级：性别 + 地区（忽略时期）
        String regionGenderKey = region + "_ancient_" + gender;
        List<String> regionGenderCandidates = AVATAR_POOL.entrySet().stream()
                .filter(e -> e.getKey().startsWith(region) && e.getKey().endsWith("_" + gender))
                .flatMap(e -> e.getValue().stream())
                .toList();
        if (!regionGenderCandidates.isEmpty()) {
            return randomPick(regionGenderCandidates);
        }

        // 第3级：同地区随机
        List<String> regionCandidates = AVATAR_POOL.entrySet().stream()
                .filter(e -> e.getKey().startsWith(region))
                .flatMap(e -> e.getValue().stream())
                .toList();
        if (!regionCandidates.isEmpty()) {
            return randomPick(regionCandidates);
        }

        // 最终兜底：全局随机
        List<String> all = AVATAR_POOL.values().stream()
                .flatMap(List::stream)
                .toList();
        return randomPick(all);
    }

    /**
     * 兜底方法：当没有诗人上下文时，随机返回一个头像
     */
    public static String getRandomAvatar() {
        List<String> all = AVATAR_POOL.values().stream()
                .flatMap(List::stream)
                .toList();
        return randomPick(all);
    }

    private static String randomPick(List<String> list) {
        int index = ThreadLocalRandom.current().nextInt(list.size());
        return list.get(index);
    }
}
