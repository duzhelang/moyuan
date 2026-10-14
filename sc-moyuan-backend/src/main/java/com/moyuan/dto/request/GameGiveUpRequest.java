package com.moyuan.dto.request;

import lombok.Data;

/**
 * 主动放弃本局请求。
 */
@Data
public class GameGiveUpRequest {

    /** 对局唯一 ID */
    private String gameId;
}
