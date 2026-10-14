package com.moyuan.dto.request;

import lombok.Data;

/**
 * 回合开始请求（记录 roundStartTime）。
 */
@Data
public class GameRoundBeginRequest {

    /** 对局唯一 ID */
    private String gameId;
}
