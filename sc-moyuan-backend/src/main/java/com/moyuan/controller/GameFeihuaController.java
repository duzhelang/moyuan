package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.dto.request.GameGiveUpRequest;
import com.moyuan.dto.request.GamePlayRequest;
import com.moyuan.dto.request.GameRoundBeginRequest;
import com.moyuan.dto.request.GameStartRequest;
import com.moyuan.dto.response.GamePlayResponse;
import com.moyuan.dto.response.GameStartResponse;
import com.moyuan.service.GameFeihuaService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 飞花令接口。
 * 游客与新页面共用同一套接口，后端按登录态分层（游客仅 Redis，登录用户落库）。
 */
@Slf4j
@Tag(name = "飞花令接口")
@RestController
@RequestMapping("/api/game/feihua")
@RequiredArgsConstructor
public class GameFeihuaController {

    private final GameFeihuaService gameFeihuaService;

    @Operation(summary = "创建飞花令对局")
    @PostMapping("/start")
    public R<GameStartResponse> start(@RequestBody GameStartRequest req) {
        return R.success(gameFeihuaService.start(req, currentUserIdOrNull()));
    }

    @Operation(summary = "开始/进入下一回合，后端记录 roundStartTime")
    @PostMapping("/round/begin")
    public R<GamePlayResponse> begin(@RequestBody GameRoundBeginRequest req) {
        return R.success(gameFeihuaService.beginRound(req.getGameId(), currentUserIdOrNull()));
    }

    @Operation(summary = "提交本轮作答")
    @PostMapping("/play")
    public R<GamePlayResponse> play(@RequestBody GamePlayRequest req) {
        return R.success(gameFeihuaService.play(req, currentUserIdOrNull()));
    }

    @Operation(summary = "主动放弃本局")
    @PostMapping("/giveup")
    public R<Void> giveUp(@RequestBody GameGiveUpRequest req) {
        gameFeihuaService.giveUp(req.getGameId(), currentUserIdOrNull());
        return R.success();
    }

    @Operation(summary = "恢复当前对局状态")
    @GetMapping("/state")
    public R<GamePlayResponse> state(@RequestParam String gameId) {
        return R.success(gameFeihuaService.state(gameId, currentUserIdOrNull()));
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
