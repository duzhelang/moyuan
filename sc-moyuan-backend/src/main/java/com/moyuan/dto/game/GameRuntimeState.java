package com.moyuan.dto.game;

import com.moyuan.dto.response.SentenceRef;
import lombok.Data;

import java.util.List;

/**
 * 对局运行时状态（Redis value 对象）。
 * 键：game:{gameId}，TTL 30 分钟；每次交互后续期。
 */
@Data
public class GameRuntimeState {

    /** 对局唯一 ID（补充字段，便于响应回传与后续读写） */
    private String gameId;

    /** 玩法类型：feihua / chain */
    private String gameType;

    /** 归属用户（游客为 null） */
    private Long userId;

    /** 是否游客 */
    private boolean isGuest;

    /** 飞花令模式：ancient/normal/super */
    private String mode;

    /** 飞花令难度：easy/medium/hard */
    private String level;

    /** 超级飞花令子类型：char/topic */
    private String superType;

    /** 令字/关键字列表，或主题描述（单元素） */
    private List<String> keywords;

    /** 接龙选择范围朝代 ID；为空表示不限朝代（中国古典诗词） */
    private Long dynastyId;

    /** 接龙选择范围朝代名称；为空表示不限朝代 */
    private String dynastyName;

    /** 接龙：当前待接句（上一句） */
    private SentenceRef lastSentence;

    /** 当前回合 */
    private int roundNo;

    /** 本回合开始时间戳（毫秒），计时唯一权威来源 */
    private long roundStartTime;

    /** 剩余重试次数（初始 2） */
    private int retryLeft;

    /** 有效条数 / 接龙链长 */
    private int validCount;

    /** 本回合累计错误次数 */
    private int roundErrors;

    /** 对局状态：playing / over */
    private String status;

    /** 对局结果：win/lose/giveup（补充字段，用于对局结束后恢复时回传） */
    private String gameResult;
}
