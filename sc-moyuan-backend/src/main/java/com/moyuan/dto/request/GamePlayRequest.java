package com.moyuan.dto.request;

import lombok.Data;

/**
 * 提交本轮作答请求。
 * mode/level/keywords/roundNo/roundStartTime 等均由后端按 gameId 读取，不接受前端传入。
 */
@Data
public class GamePlayRequest {

    /** 对局唯一 ID */
    private String gameId;

    /** 用户本轮提交诗句（文本或语音转写结果） */
    private String userSentence;

    /** 输入方式：text 文本 / voice 语音 */
    private String inputType = "text";

    /**
     * 前端上报的「已消耗有效答题秒数」：打字期间倒计时暂停，仅统计实际走表时长，
     * 含 30 秒答题 + 5 秒缓冲共 35 秒上限。
     * 非空时后端以此判定超时；为空时回退服务端墙钟口径。
     */
    private Integer activeSeconds;
}
