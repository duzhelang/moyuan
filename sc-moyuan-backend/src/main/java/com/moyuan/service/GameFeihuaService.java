package com.moyuan.service;

import com.moyuan.dto.request.GamePlayRequest;
import com.moyuan.dto.request.GameStartRequest;
import com.moyuan.dto.response.GamePlayResponse;
import com.moyuan.dto.response.GameStartResponse;

/**
 * 飞花令对局编排：开局、回合开始、作答、放弃、状态恢复。
 */
public interface GameFeihuaService {

    GameStartResponse start(GameStartRequest req, Long userId);

    GamePlayResponse beginRound(String gameId, Long userId);

    GamePlayResponse play(GamePlayRequest req, Long userId);

    void giveUp(String gameId, Long userId);

    GamePlayResponse state(String gameId, Long userId);
}
