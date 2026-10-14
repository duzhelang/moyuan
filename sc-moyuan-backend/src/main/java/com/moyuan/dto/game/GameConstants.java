package com.moyuan.dto.game;

/**
 * 游戏模块常量与文案映射。
 * 所有提示文案由后端本地维护，不调用 LLM。
 */
public final class GameConstants {

    private GameConstants() {
    }

    /* ========== 玩法类型 ========== */
    public static final String TYPE_FEIHUA = "feihua";
    public static final String TYPE_CHAIN = "chain";

    /* ========== 飞花令模式 ========== */
    public static final String MODE_ANCIENT = "ancient";
    public static final String MODE_NORMAL = "normal";
    public static final String MODE_SUPER = "super";

    /* ========== 飞花令难度 ========== */
    public static final String LEVEL_EASY = "easy";
    public static final String LEVEL_MEDIUM = "medium";
    public static final String LEVEL_HARD = "hard";

    /* ========== 超级飞花令子类型 ========== */
    public static final String SUPER_CHAR = "char";
    public static final String SUPER_TOPIC = "topic";

    /* ========== 对局状态 ========== */
    public static final String STATUS_PLAYING = "playing";
    public static final String STATUS_OVER = "over";

    /* ========== 对局结果 ========== */
    public static final String RESULT_WIN = "win";
    public static final String RESULT_LOSE = "lose";
    public static final String RESULT_GIVEUP = "giveup";

    /* ========== 诗句来源 ========== */
    public static final String SOURCE_USER = "user";
    public static final String SOURCE_AI = "ai";

    /* ========== 计时 ========== */
    /** 单回合答题时长：30 秒 */
    public static final long ROUND_MILLIS = 30_000L;
    /** 墙钟超时阈值：30 秒 + 1 秒网络宽限（仅前端未上报有效时长时的兜底口径） */
    public static final long TIMEOUT_MILLIS = 31_000L;
    /** 输入框停笔监测时长（秒）：停笔满该时长后倒计时继续（打字期间倒计时暂停） */
    public static final int MONITOR_SECONDS = 5;
    /** 30 秒耗尽后的缓冲提交窗口（秒） */
    public static final int BUFFER_SECONDS = 5;
    /** 有效时长上限（秒）：30 秒答题 + 5 秒缓冲 = 35 秒 */
    public static final int TOTAL_ACTIVE_SECONDS = (int) (ROUND_MILLIS / 1000L) + BUFFER_SECONDS;

    /* ========== 容错 ========== */
    /** 每回合初始重试次数 */
    public static final int INIT_RETRY = 2;
    /** 累计错误上限（达到即判负） */
    public static final int MAX_ERRORS = 3;
    /** 有效诗句最小汉字长度（去标点后） */
    public static final int MIN_HANZI_LENGTH = 4;

    /* ========== 达标条数 ========== */
    public static final int TARGET_EASY = 10;
    public static final int TARGET_MEDIUM = 20;

    /* ========== Redis ========== */
    /** 对局状态键前缀：game:{gameId} */
    public static final String REDIS_KEY_PREFIX = "game:";
    /** 游客诗句集合键后缀：game:{gameId}:sentences */
    public static final String REDIS_SENTENCE_SUFFIX = ":sentences";
    /** 对局状态 TTL：30 分钟 */
    public static final long TTL_MINUTES = 30L;

    /* ========== AI 模块编码 ========== */
    public static final String MODULE_GEN_ANCIENT = "feihua_gen_ancient";
    public static final String MODULE_GEN_NORMAL = "feihua_gen_normal";
    public static final String MODULE_GEN_SUPER = "feihua_gen_super";
    public static final String MODULE_VERIFY = "feihua_verify";
    public static final String MODULE_TOPIC = "feihua_topic";
    public static final String MODULE_CHAIN_GENERATE = "chain_generate";

    /** AI 调用 model 入参（实际会被模块绑定的模型覆盖） */
    public static final String MODEL_FALLBACK = "zhipu";

    /* ========== 失败原因（errorReason） ========== */
    public static final String REASON_EMPTY = "空输入";
    public static final String REASON_TOO_SHORT = "诗句过短（无效格式）";
    public static final String REASON_NOT_STARTED = "本回合尚未开始";
    public static final String REASON_TIMEOUT = "答题超时";
    public static final String REASON_DUPLICATE = "诗句重复";
    public static final String REASON_POSITION = "令字位次不符";
    public static final String REASON_NOT_CONTAIN = "诗句不含令字";
    public static final String REASON_NOT_ALL_KEYWORDS = "关键字不全";
    public static final String REASON_CHAIN_FIRST = "首字与上一句末字不符";
    public static final String REASON_NOT_REAL = "诗句真实性校验不通过";
    public static final String REASON_TOPIC = "诗句与主题不符";

    /* ========== 飞花令文案 ========== */
    public static final String MSG_CORRECT = "佳句！接续成功，请继续接令。";
    public static final String MSG_ERROR_RETRY_1 = "诗句不合规，还有两次重试机会。";
    public static final String MSG_ERROR_RETRY_2 = "仍不符合规则，仅剩最后一次机会。";
    public static final String MSG_DUPLICATE = "诗句已出现，请另寻佳句。";
    public static final String MSG_EMPTY = "输入为空，请重新输入。";
    public static final String MSG_NOT_STARTED = "请点击「开始挑战」后作答。";
    public static final String MSG_LOSE_ERRORS = "本轮答题失误次数已用尽，再接再厉！";
    public static final String MSG_LOSE_TIMEOUT = "答题超时，挑战结束，继续加油积累诗词底蕴！";
    public static final String MSG_GIVEUP = "已结束本轮飞花令挑战，期待下次再战！";
    public static final String MSG_WIN_EASY = "恭喜完成初级飞花令挑战！";
    public static final String MSG_WIN_MEDIUM = "毅力十足，成功完成高阶接龙挑战！";
    public static final String MSG_WIN_HARD = "博学多才，本轮 AI 已无诗句可接，你成功战胜 AI！";

    /* ========== 诗词接龙文案 ========== */
    public static final String MSG_CHAIN_CORRECT = "接住了！以「%s」起，AI 继续接令。";
    public static final String MSG_CHAIN_FIRST = "首字须与上一句末字「%s」相同，请重新接龙。";
    public static final String MSG_CHAIN_WIN = "妙极！AI 已无句可接，这场接龙你赢了！";
    public static final String MSG_CHAIN_LOSE = "接龙中断，再接再厉！";
    public static final String MSG_CHAIN_TIMEOUT = "接龙超时，挑战结束，继续加油积累诗词底蕴！";
    public static final String MSG_CHAIN_GIVEUP = "已结束本轮诗词接龙挑战，期待下次再战！";
    public static final String MSG_CHAIN_START = "AI 起句：「%s」，请以末字「%s」接起。";
    /** 选择范围限定时的越界提示（%s 为范围名称，如「唐代」） */
    public static final String MSG_CHAIN_OUT_OF_SCOPE = "该句不在所选范围「%s」内，请提交范围内的真实诗句。";

    /**
     * 超时判定：前端上报的「已消耗有效答题秒数」优先（打字期间倒计时暂停，该时长不计入）；
     * 未上报时回退到服务端墙钟口径（30 秒 + 1 秒宽限）。
     *
     * @param activeSeconds    前端上报的已消耗有效秒数，可为 null
     * @param wallElapsedMillis 服务端墙钟耗时（毫秒）
     * @return true 表示已超时
     */
    public static boolean isTimeout(Integer activeSeconds, long wallElapsedMillis) {
        if (activeSeconds != null) {
            return activeSeconds >= TOTAL_ACTIVE_SECONDS;
        }
        return wallElapsedMillis > TIMEOUT_MILLIS;
    }

    /**
     * 飞花令达标条数。
     *
     * @param level 难度：easy/medium/hard
     * @return easy=10、medium=20、hard=null
     */
    public static Integer winTargetCount(String level) {
        if (LEVEL_EASY.equals(level)) {
            return TARGET_EASY;
        }
        if (LEVEL_MEDIUM.equals(level)) {
            return TARGET_MEDIUM;
        }
        return null;
    }

    /**
     * 按飞花令模式返回候选生成模块编码。
     */
    public static String genModuleCode(String mode) {
        if (MODE_ANCIENT.equals(mode)) {
            return MODULE_GEN_ANCIENT;
        }
        if (MODE_SUPER.equals(mode)) {
            return MODULE_GEN_SUPER;
        }
        return MODULE_GEN_NORMAL;
    }

    /**
     * 飞花令模式是否为超级飞花令。
     */
    public static boolean isSuper(String mode) {
        return MODE_SUPER.equals(mode);
    }
}
