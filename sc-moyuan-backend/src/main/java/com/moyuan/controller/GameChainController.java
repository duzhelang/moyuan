package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.dto.request.ChainStartRequest;
import com.moyuan.dto.request.GameGiveUpRequest;
import com.moyuan.dto.request.GamePlayRequest;
import com.moyuan.dto.request.GameRoundBeginRequest;
import com.moyuan.dto.response.ChainStartResponse;
import com.moyuan.dto.response.GamePlayResponse;
import com.moyuan.service.GameChainService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 诗词接龙接口。
 * 游客与新页面共用同一套接口，后端按登录态分层（游客仅 Redis，登录用户落库）。
 */
@Slf4j
@Tag(name = "诗词接龙接口")
@RestController
@RequestMapping("/api/game/chain")
@RequiredArgsConstructor
public class GameChainController {

    private final GameChainService gameChainService;

    @Operation(summary = "创建诗词接龙对局，返回 AI 起句")
    @PostMapping("/start")
    public R<ChainStartResponse> start(@RequestBody ChainStartRequest req) {
        return R.success(gameChainService.start(req, currentUserIdOrNull()));
    }

    @Operation(summary = "开始/进入下一回合，后端记录 roundStartTime")
    @PostMapping("/round/begin")
    public R<GamePlayResponse> begin(@RequestBody GameRoundBeginRequest req) {
        return R.success(gameChainService.beginRound(req.getGameId(), currentUserIdOrNull()));
    }

    @Operation(summary = "提交本轮接句")
    @PostMapping("/play")
    public R<GamePlayResponse> play(@RequestBody GamePlayRequest req) {
        return R.success(gameChainService.play(req, currentUserIdOrNull()));
    }

    @Operation(summary = "主动放弃本局")
    @PostMapping("/giveup")
    public R<Void> giveUp(@RequestBody GameGiveUpRequest req) {
        gameChainService.giveUp(req.getGameId(), currentUserIdOrNull());
        return R.success();
    }

    @Operation(summary = "恢复当前对局状态")
    @GetMapping("/state")
    public R<GamePlayResponse> state(@RequestParam String gameId) {
        return R.success(gameChainService.state(gameId, currentUserIdOrNull()));
    }

    /** 登录态获取：未登录（游客）返回 null。 */
    private Long currentUserIdOrNull() {
        try {
            return SecurityUtil.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
