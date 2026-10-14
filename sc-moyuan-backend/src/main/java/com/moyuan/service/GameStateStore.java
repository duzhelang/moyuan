package com.moyuan.service;

import com.moyuan.dto.game.GameRuntimeState;
import com.moyuan.dto.response.SentenceRef;

import java.util.List;

/**
 * 对局运行时状态与计时（Redis），以及落库归档。
 */
public interface GameStateStore {

    /**
     * 创建对局并写入 Redis（登录用户同时写入 game_record）。
     *
     * @param gameType      玩法类型 feihua/chain
     * @param userId        归属用户（游客为 null）
     * @param mode          飞花令模式（接龙为 null）
     * @param level         飞花令难度（接龙为 null）
     * @param superType     超级飞花令子类型（接龙一律 null）
     * @param keywords      令字/关键字列表（接龙为 null）
     * @param startSentence 接龙 AI 起句（飞花令为 null）
     * @return 已持久化的对局状态
     */
    GameRuntimeState create(String gameType, Long userId, String mode, String level,
                            String superType, List<String> keywords, SentenceRef startSentence);

    /**
     * 按 gameId 加载对局状态，并做归属校验（防跨用户读取）。
     */
    GameRuntimeState load(String gameId, Long currentUserId);

    /**
     * 写入并续期（整体覆盖 + 刷新 TTL）。
     */
    void persist(GameRuntimeState state);

    /**
     * 仅续期（不写值）。
     */
    void refreshTtl(GameRuntimeState state);

    /**
     * 本回合已用毫秒数（服务端时间）。
     */
    long calcElapsed(GameRuntimeState state);

    /**
     * 本回合剩余秒数：max(0, ceil((30 - elapsed) / 1s))。
     */
    int calcRemainSeconds(GameRuntimeState state);

    /**
     * 结束对局归档：登录用户更新 game_record，游客仅改状态。
     */
    void archive(GameRuntimeState state, String result);

    /**
     * 判重：登录用户查 game_sentence 表，游客查 Redis 集合。
     */
    boolean isDuplicate(GameRuntimeState state, String sentence);

    /**
     * 记录一条有效诗句：登录用户写 game_sentence，游客写 Redis 集合。
     */
    void recordSentence(GameRuntimeState state, int roundNo, String sentence,
                        String title, String author, String source);

    /**
     * 读取历史诗句（登录用户查表按回合排序，游客读 Redis 集合）。
     */
    List<String> loadHistory(GameRuntimeState state);
}
