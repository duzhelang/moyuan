package com.moyuan.service;

import com.moyuan.dto.request.ChainStartRequest;
import com.moyuan.dto.request.GamePlayRequest;
import com.moyuan.dto.response.ChainStartResponse;
import com.moyuan.dto.response.GamePlayResponse;

/**
 * 诗词接龙对局编排：AI 起句、用户接句、AI 接句生成与过滤、状态恢复。
 */
public interface GameChainService {

    ChainStartResponse start(ChainStartRequest req, Long userId);

    GamePlayResponse beginRound(String gameId, Long userId);

    GamePlayResponse play(GamePlayRequest req, Long userId);

    void giveUp(String gameId, Long userId);

    GamePlayResponse state(String gameId, Long userId);
}
