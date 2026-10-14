package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.common.ResultCode;
import com.moyuan.dto.game.GameConstants;
import com.moyuan.dto.game.GameRuntimeState;
import com.moyuan.dto.request.ChainStartRequest;
import com.moyuan.dto.request.GamePlayRequest;
import com.moyuan.dto.response.ChainStartResponse;
import com.moyuan.dto.response.GamePlayResponse;
import com.moyuan.dto.response.SentenceRef;
import com.moyuan.entity.Poem;
import com.moyuan.entity.Poet;
import com.moyuan.entity.Dynasty;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.DynastyMapper;
import com.moyuan.mapper.PoetMapper;
import com.moyuan.service.AiService;
import com.moyuan.service.GameChainService;
import com.moyuan.service.GameStateStore;
import com.moyuan.service.GameVerifyService;
import com.moyuan.service.PoemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 诗词接龙对局编排实现。
 * 规则：首字衔接上一句末字；AI 起句取自本地库；AI 接句生成后二次过滤，过滤为空判用户胜。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameChainServiceImpl implements GameChainService {

    private final GameStateStore stateStore;
    private final GameVerifyService verifyService;
    private final AiService aiService;
    private final PoemService poemService;
    private final PoetMapper poetMapper;
    private final DynastyMapper dynastyMapper;

    @Override
    public ChainStartResponse start(ChainStartRequest req, Long userId) {
        // 选择范围：朝代为空表示「中国古典诗词（不限朝代）」
        Long dynastyId = req == null ? null : req.getDynastyId();
        String dynastyName = resolveDynastyName(dynastyId);

        SentenceRef startRef = pickStartSentence(dynastyId);
        GameRuntimeState state = stateStore.create(GameConstants.TYPE_CHAIN, userId, null, null,
                null, null, startRef);
        // 记录选择范围，供 AI 生成与校验沿用
        state.setDynastyId(dynastyId);
        state.setDynastyName(dynastyName);
        stateStore.persist(state);
        // AI 起句计入有效诗句（source=ai，回合 0），保证判重与历史展示完整
        stateStore.recordSentence(state, 0, startRef.getSentence(),
                startRef.getTitle(), startRef.getAuthor(), GameConstants.SOURCE_AI);

        ChainStartResponse resp = new ChainStartResponse();
        resp.setGameId(state.getGameId());
        resp.setChainCount(0);
        resp.setStartSentence(startRef);
        resp.setIsGuest(userId == null);
        resp.setDynastyId(dynastyId);
        resp.setDynastyName(dynastyName);
        log.info("接龙开局：gameId={}，起句={}，范围={}，isGuest={}", state.getGameId(), startRef.getSentence(),
                dynastyName == null ? "中国古典诗词" : dynastyName, userId == null);
        return resp;
    }

    @Override
    public GamePlayResponse beginRound(String gameId, Long userId) {
        GameRuntimeState state = stateStore.load(gameId, userId);
        GamePlayResponse resp = baseResponse(state);
        if (GameConstants.STATUS_OVER.equals(state.getStatus())) {
            return fillOver(state, resp);
        }
        state.setStatus(GameConstants.STATUS_PLAYING);
        state.setRoundStartTime(System.currentTimeMillis());
        state.setRetryLeft(GameConstants.INIT_RETRY);
        state.setRoundErrors(0);
        stateStore.persist(state);

        resp.setIsValid(true);
        resp.setCheckMsg("");
        resp.setErrorReason("");
        resp.setRoundNo(state.getRoundNo());
        resp.setRetryLeft(state.getRetryLeft());
        resp.setRemainSeconds((int) (GameConstants.ROUND_MILLIS / 1000L));
        resp.setIsGameOver(false);
        return resp;
    }

    @Override
    public GamePlayResponse play(GamePlayRequest req, Long userId) {
        if (req == null || !StringUtils.hasText(req.getGameId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "缺少对局ID");
        }
        GameRuntimeState state = stateStore.load(req.getGameId(), userId);
        GamePlayResponse resp = baseResponse(state);

        if (GameConstants.STATUS_OVER.equals(state.getStatus())) {
            return fillOver(state, resp);
        }
        if (state.getRoundStartTime() <= 0) {
            resp.setIsValid(false);
            resp.setErrorReason(GameConstants.REASON_NOT_STARTED);
            resp.setCheckMsg(GameConstants.MSG_NOT_STARTED);
            return resp;
        }
        // 超时判定（前端上报有效时长优先；未上报时回退服务端墙钟 30 秒 + 1 秒宽限）
        if (GameConstants.isTimeout(req.getActiveSeconds(), stateStore.calcElapsed(state))) {
            return endGame(state, resp, GameConstants.RESULT_LOSE,
                    GameConstants.MSG_CHAIN_TIMEOUT, GameConstants.REASON_TIMEOUT);
        }

        String raw = req.getUserSentence();
        if (!StringUtils.hasText(raw)) {
            // 空输入：不计错误、不消耗重试、不重置计时
            resp.setIsValid(false);
            resp.setErrorReason(GameConstants.REASON_EMPTY);
            resp.setCheckMsg(GameConstants.MSG_EMPTY);
            resp.setRemainSeconds(stateStore.calcRemainSeconds(state));
            resp.setRetryLeft(state.getRetryLeft());
            return resp;
        }
        String normalized = verifyService.normalize(raw);
        if (normalized.length() < GameConstants.MIN_HANZI_LENGTH) {
            return reject(state, resp, GameConstants.REASON_TOO_SHORT, null);
        }

        char lastChar = lastCharOf(state);
        // 本地规则：首字须等于上一句末字
        if (lastChar == 0 || verifyService.firstHanzi(normalized) != lastChar) {
            return reject(state, resp, GameConstants.REASON_CHAIN_FIRST,
                    GameConstants.MSG_CHAIN_FIRST.replace("%s", String.valueOf(lastChar)));
        }
        // 判重
        if (stateStore.isDuplicate(state, normalized)) {
            return reject(state, resp, GameConstants.REASON_DUPLICATE, null);
        }
        // 真实性三级链（限定范围内时同时校验朝代归属）
        if (!verifyService.verifyReal(normalized, state.getDynastyId(), state.getDynastyName())) {
            String msg = state.getDynastyName() == null ? null
                    : GameConstants.MSG_CHAIN_OUT_OF_SCOPE.replace("%s", state.getDynastyName());
            return reject(state, resp, GameConstants.REASON_NOT_REAL, msg);
        }

        // 通过：链长 +1，用户句入库
        state.setValidCount(state.getValidCount() + 1);
        stateStore.recordSentence(state, state.getRoundNo(), normalized, null, null, GameConstants.SOURCE_USER);

        // AI 接句生成 + 二次过滤
        SentenceRef next = generateNextSentence(state, normalized);
        if (next == null) {
            // AI 接不上 → 用户胜
            stateStore.archive(state, GameConstants.RESULT_WIN);
            resp.setIsValid(true);
            resp.setCheckMsg(GameConstants.MSG_CHAIN_WIN);
            resp.setErrorReason("");
            resp.setNextSentence(null);
            resp.setIsGameOver(true);
            resp.setGameResult(GameConstants.RESULT_WIN);
            resp.setRemainSeconds(0);
            resp.setRetryLeft(state.getRetryLeft());
            resp.setRoundNo(state.getRoundNo());
            fillHistory(state, resp);
            return resp;
        }

        // AI 接句成为新的待接句；进入下一回合
        stateStore.recordSentence(state, state.getRoundNo(), next.getSentence(),
                next.getTitle(), next.getAuthor(), GameConstants.SOURCE_AI);
        state.setLastSentence(next);
        state.setRoundNo(state.getRoundNo() + 1);
        state.setRetryLeft(GameConstants.INIT_RETRY);
        state.setRoundErrors(0);
        state.setRoundStartTime(System.currentTimeMillis());
        stateStore.persist(state);

        resp.setIsValid(true);
        resp.setCheckMsg(String.format(GameConstants.MSG_CHAIN_CORRECT, String.valueOf(lastChar)));
        resp.setErrorReason("");
        resp.setNextSentence(next);
        resp.setRoundNo(state.getRoundNo());
        resp.setRetryLeft(state.getRetryLeft());
        resp.setRemainSeconds((int) (GameConstants.ROUND_MILLIS / 1000L));
        resp.setIsGameOver(false);
        fillHistory(state, resp);
        return resp;
    }

    @Override
    public void giveUp(String gameId, Long userId) {
        GameRuntimeState state = stateStore.load(gameId, userId);
        if (GameConstants.STATUS_OVER.equals(state.getStatus())) {
            return;
        }
        stateStore.archive(state, GameConstants.RESULT_GIVEUP);
        log.info("接龙主动放弃：gameId={}", gameId);
    }

    @Override
    public GamePlayResponse state(String gameId, Long userId) {
        GameRuntimeState state = stateStore.load(gameId, userId);
        GamePlayResponse resp = baseResponse(state);
        if (GameConstants.STATUS_OVER.equals(state.getStatus())) {
            return fillOver(state, resp);
        }
        if (state.getRoundStartTime() <= 0) {
            resp.setIsValid(true);
            resp.setCheckMsg("");
            resp.setRemainSeconds((int) (GameConstants.ROUND_MILLIS / 1000L));
            return resp;
        }
        if (stateStore.calcElapsed(state) > GameConstants.TIMEOUT_MILLIS) {
            return endGame(state, resp, GameConstants.RESULT_LOSE,
                    GameConstants.MSG_CHAIN_TIMEOUT, GameConstants.REASON_TIMEOUT);
        }
        resp.setIsValid(true);
        resp.setCheckMsg("");
        resp.setRemainSeconds(stateStore.calcRemainSeconds(state));
        resp.setRetryLeft(state.getRetryLeft());
        resp.setIsGameOver(false);
        return resp;
    }

    /* ==================== 内部实现 ==================== */

    /**
     * 从本地诗词库随机取一句作为 AI 起句，不调用 LLM。
     * dynastyId 非空时仅在该朝代范围内取句。
     */
    private SentenceRef pickStartSentence(Long dynastyId) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, 1)
                .eq(Poem::getPoemType, "classical")
                .eq(dynastyId != null, Poem::getDynastyId, dynastyId)
                .isNotNull(Poem::getContent)
                .last("ORDER BY RAND() LIMIT 1");
        Poem poem = poemService.getOne(wrapper);
        if (poem == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    dynastyId == null ? "诗词库暂无可用起句，请稍后再试" : "所选范围暂无可用起句，请更换范围后再试");
        }
        String sentence = pickLine(poem.getContent());
        if (!StringUtils.hasText(sentence)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    dynastyId == null ? "诗词库暂无可用起句，请稍后再试" : "所选范围暂无可用起句，请更换范围后再试");
        }
        String author = null;
        if (poem.getPoetId() != null) {
            Poet poet = poetMapper.selectById(poem.getPoetId());
            if (poet != null) {
                author = poet.getName();
            }
        }
        return new SentenceRef(sentence, poem.getTitle(), author,
                "AI 起句，末字「" + verifyService.lastHanzi(sentence) + "」");
    }

    /** 解析选择范围朝代名称；dynastyId 为空或无效返回 null（表示不限朝代）。 */
    private String resolveDynastyName(Long dynastyId) {
        if (dynastyId == null) {
            return null;
        }
        Dynasty dynasty = dynastyMapper.selectById(dynastyId);
        if (dynasty == null || !StringUtils.hasText(dynasty.getName())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "所选范围不合法");
        }
        return dynasty.getName();
    }

    /** 从诗文中按标点切分出一句长度合规的古典诗句。 */
    private String pickLine(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        String[] parts = content.split("[，。！？；,.!?;\\n\\r\\s]+");
        List<String> lines = new ArrayList<>();
        for (String part : parts) {
            String n = verifyService.normalize(part);
            if (n.length() >= GameConstants.MIN_HANZI_LENGTH && n.length() <= 20) {
                lines.add(n);
            }
        }
        if (lines.isEmpty()) {
            return null;
        }
        return lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
    }

    /** 调用 chain_generate 生成 AI 接句，并按首字衔接 + 判重 + 真实性二次过滤，返回首个合格句。 */
    private SentenceRef generateNextSentence(GameRuntimeState state, String userSentence) {
        char lastChar = verifyService.lastHanzi(userSentence);
        List<String> history = stateStore.loadHistory(state);
        String scopeLine = state.getDynastyName() == null
                ? "\n范围限定：仅限中国古典诗词原句。"
                : "\n范围限定：仅限「" + state.getDynastyName() + "」朝代的诗词原句（不得使用其他朝代）。";
        String userPrompt = "上一句：" + userSentence + "（末字：" + lastChar + "）"
                + "\n已使用诗句列表：" + String.join("，", history)
                + scopeLine
                + "\n请接出首字为「" + lastChar + "」的古典诗词原句，输出纯JSON数组。";

        String raw;
        try {
            raw = aiService.chat(userPrompt, GameConstants.MODEL_FALLBACK, GameConstants.MODULE_CHAIN_GENERATE);
        } catch (Exception e) {
            log.warn("AI 接句生成失败：gameId={}，error={}", state.getGameId(), e.getMessage());
            return null;
        }

        List<SentenceRef> parsed = verifyService.parseCandidates(raw);
        for (SentenceRef candidate : parsed) {
            String s = verifyService.normalize(candidate.getSentence());
            if (s.isEmpty()) {
                continue;
            }
            if (verifyService.firstHanzi(s) != lastChar) {
                continue;
            }
            if (stateStore.isDuplicate(state, s)) {
                continue;
            }
            if (!verifyService.verifyReal(s, state.getDynastyId(), state.getDynastyName())) {
                continue;
            }
            candidate.setSentence(s);
            candidate.setMatchDesc("AI 接句，首字「" + lastChar + "」");
            return candidate;
        }
        return null;
    }

    private char lastCharOf(GameRuntimeState state) {
        SentenceRef last = state.getLastSentence();
        return (last == null || last.getSentence() == null) ? 0 : verifyService.lastHanzi(last.getSentence());
    }

    /** 失败处理：消耗重试，累计 3 次判负；答错不重置计时。 */
    private GamePlayResponse reject(GameRuntimeState state, GamePlayResponse resp, String reason, String customMsg) {
        state.setRoundErrors(state.getRoundErrors() + 1);
        state.setRetryLeft(Math.max(0, state.getRetryLeft() - 1));
        resp.setIsValid(false);
        resp.setErrorReason(reason);
        resp.setNextSentence(null);
        resp.setRemainSeconds(stateStore.calcRemainSeconds(state));

        if (state.getRoundErrors() >= GameConstants.MAX_ERRORS) {
            stateStore.archive(state, GameConstants.RESULT_LOSE);
            resp.setCheckMsg(GameConstants.MSG_CHAIN_LOSE);
            resp.setIsGameOver(true);
            resp.setGameResult(GameConstants.RESULT_LOSE);
            resp.setRetryLeft(0);
            resp.setRoundNo(state.getRoundNo());
            return resp;
        }
        stateStore.persist(state);
        String msg;
        if (GameConstants.REASON_DUPLICATE.equals(reason)) {
            msg = GameConstants.MSG_DUPLICATE;
        } else if (customMsg != null) {
            msg = customMsg;
        } else if (state.getRoundErrors() == 1) {
            msg = GameConstants.MSG_ERROR_RETRY_1;
        } else {
            msg = GameConstants.MSG_ERROR_RETRY_2;
        }
        resp.setCheckMsg(msg);
        resp.setIsGameOver(false);
        resp.setGameResult("");
        resp.setRetryLeft(state.getRetryLeft());
        resp.setRoundNo(state.getRoundNo());
        return resp;
    }

    private GamePlayResponse endGame(GameRuntimeState state, GamePlayResponse resp,
                                     String result, String msg, String reason) {
        stateStore.archive(state, result);
        resp.setIsValid(false);
        resp.setErrorReason(reason);
        resp.setCheckMsg(msg);
        resp.setIsGameOver(true);
        resp.setGameResult(result);
        resp.setRemainSeconds(0);
        resp.setRetryLeft(state.getRetryLeft());
        resp.setRoundNo(state.getRoundNo());
        fillHistory(state, resp);
        return resp;
    }

    private GamePlayResponse baseResponse(GameRuntimeState state) {
        GamePlayResponse resp = new GamePlayResponse();
        resp.setGameId(state.getGameId());
        resp.setMode(state.getMode());
        resp.setLevel(state.getLevel());
        resp.setKeywords(state.getKeywords());
        resp.setDynastyId(state.getDynastyId());
        resp.setDynastyName(state.getDynastyName());
        resp.setIsGuest(state.isGuest());
        resp.setRoundNo(state.getRoundNo());
        resp.setRetryLeft(state.getRetryLeft());
        resp.setWinTargetCount(GameConstants.winTargetCount(state.getLevel()));
        resp.setRemainSeconds(stateStore.calcRemainSeconds(state));
        resp.setIsValid(false);
        resp.setIsGameOver(false);
        resp.setGameResult("");
        resp.setNextReferenceList(null);
        resp.setNextSentence(null);
        fillHistory(state, resp);
        return resp;
    }

    private GamePlayResponse fillOver(GameRuntimeState state, GamePlayResponse resp) {
        resp.setIsGameOver(true);
        resp.setIsValid(false);
        resp.setGameResult(state.getGameResult() == null ? "" : state.getGameResult());
        resp.setCheckMsg(overMsg(state));
        resp.setRemainSeconds(0);
        resp.setRetryLeft(state.getRetryLeft());
        resp.setRoundNo(state.getRoundNo());
        resp.setNextSentence(null);
        fillHistory(state, resp);
        return resp;
    }

    private String overMsg(GameRuntimeState state) {
        String result = state.getGameResult();
        if (GameConstants.RESULT_WIN.equals(result)) {
            return GameConstants.MSG_CHAIN_WIN;
        }
        if (GameConstants.RESULT_GIVEUP.equals(result)) {
            return GameConstants.MSG_CHAIN_GIVEUP;
        }
        return GameConstants.MSG_CHAIN_LOSE;
    }

    private void fillHistory(GameRuntimeState state, GamePlayResponse resp) {
        resp.setHistorySentences(stateStore.loadHistory(state));
        resp.setHistoryCount(state.getValidCount());
        // 接龙链长与有效条数同源，供前端展示
        resp.setChainCount(state.getValidCount());
    }
}
