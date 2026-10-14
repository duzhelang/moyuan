package com.moyuan.service.impl;

import com.moyuan.common.ResultCode;
import com.moyuan.dto.game.GameConstants;
import com.moyuan.dto.game.GameRuntimeState;
import com.moyuan.dto.request.GamePlayRequest;
import com.moyuan.dto.request.GameStartRequest;
import com.moyuan.dto.response.GamePlayResponse;
import com.moyuan.dto.response.GameStartResponse;
import com.moyuan.dto.response.SentenceRef;
import com.moyuan.exception.BusinessException;
import com.moyuan.service.AiService;
import com.moyuan.service.GameFeihuaService;
import com.moyuan.service.GameStateStore;
import com.moyuan.service.GameVerifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 飞花令对局编排实现。
 * 计时权威、判重、位次/含字规则、真实性三级链调度、计数与胜负、候选生成与二次过滤均在后端。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameFeihuaServiceImpl implements GameFeihuaService {

    private final GameStateStore stateStore;
    private final GameVerifyService verifyService;
    private final AiService aiService;

    @Override
    public GameStartResponse start(GameStartRequest req, Long userId) {
        validateStart(req);
        String mode = req.getMode().trim();
        String level = req.getLevel().trim();
        String superType = GameConstants.isSuper(mode) ? req.getSuperType().trim() : null;
        List<String> keywords = normalizeKeywords(req.getKeywords());

        GameRuntimeState state = stateStore.create(GameConstants.TYPE_FEIHUA, userId, mode, level,
                superType, keywords, null);

        GameStartResponse resp = new GameStartResponse();
        resp.setGameId(state.getGameId());
        resp.setMode(mode);
        resp.setLevel(level);
        resp.setKeywords(keywords);
        resp.setWinTargetCount(GameConstants.winTargetCount(level));
        resp.setStartSentence(null);
        resp.setIsGuest(userId == null);
        log.info("飞花令开局：gameId={}，mode={}，level={}，isGuest={}",
                state.getGameId(), mode, level, userId == null);
        return resp;
    }

    @Override
    public GamePlayResponse beginRound(String gameId, Long userId) {
        GameRuntimeState state = stateStore.load(gameId, userId);
        GamePlayResponse resp = baseResponse(state);
        if (GameConstants.STATUS_OVER.equals(state.getStatus())) {
            return fillOver(state, resp);
        }
        // 记录本回合 roundStartTime（服务端时间戳，计时唯一权威）
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

        // 1) 前置：对局未结束
        if (GameConstants.STATUS_OVER.equals(state.getStatus())) {
            return fillOver(state, resp);
        }
        // 1) 前置：尚未开始计时
        if (state.getRoundStartTime() <= 0) {
            resp.setIsValid(false);
            resp.setErrorReason(GameConstants.REASON_NOT_STARTED);
            resp.setCheckMsg(GameConstants.MSG_NOT_STARTED);
            return resp;
        }
        // 1) 前置：超时判定（前端上报有效时长优先；未上报时回退服务端墙钟 30 秒 + 1 秒宽限）
        if (GameConstants.isTimeout(req.getActiveSeconds(), stateStore.calcElapsed(state))) {
            return endGame(state, resp, GameConstants.RESULT_LOSE,
                    GameConstants.MSG_LOSE_TIMEOUT, GameConstants.REASON_TIMEOUT);
        }
        // 1) 前置：空输入（不计错误、不消耗重试、不重置计时）
        String raw = req.getUserSentence();
        if (!StringUtils.hasText(raw)) {
            resp.setIsValid(false);
            resp.setErrorReason(GameConstants.REASON_EMPTY);
            resp.setCheckMsg(GameConstants.MSG_EMPTY);
            resp.setRemainSeconds(stateStore.calcRemainSeconds(state));
            resp.setRetryLeft(state.getRetryLeft());
            return resp;
        }
        String normalized = verifyService.normalize(raw);
        // 1) 前置：有效诗句长度口径（去标点后 ≥ 4 字）
        if (normalized.length() < GameConstants.MIN_HANZI_LENGTH) {
            return reject(state, resp, GameConstants.REASON_TOO_SHORT);
        }

        // 2) 本地规则：判重 → 位次/含字/多关键字
        if (stateStore.isDuplicate(state, normalized)) {
            return reject(state, resp, GameConstants.REASON_DUPLICATE);
        }
        String ruleReason = ruleFailReason(state, normalized);
        if (ruleReason != null) {
            return reject(state, resp, ruleReason);
        }

        // 3) 真实性三级链
        if (!verifyService.verifyReal(normalized)) {
            return reject(state, resp, GameConstants.REASON_NOT_REAL);
        }

        // 4) super-topic 主题语义匹配
        if (isSuperTopic(state) && !verifyService.verifyTopic(normalized, topicOf(state))) {
            return reject(state, resp, GameConstants.REASON_TOPIC);
        }

        // 5) 全部通过：有效计数 +1 并入库
        state.setValidCount(state.getValidCount() + 1);
        stateStore.recordSentence(state, state.getRoundNo(), normalized, null, null, GameConstants.SOURCE_USER);

        Integer target = GameConstants.winTargetCount(state.getLevel());
        List<SentenceRef> candidates = Collections.emptyList();
        boolean win = false;
        String winMsg = "";

        if (target != null && state.getValidCount() >= target) {
            // easy/medium 达标即胜，不再生成候选
            win = true;
            winMsg = GameConstants.LEVEL_EASY.equals(state.getLevel())
                    ? GameConstants.MSG_WIN_EASY : GameConstants.MSG_WIN_MEDIUM;
        } else {
            // 生成候选并二次过滤（未过滤结果绝不下发）
            candidates = generateAndFilterCandidates(state, state.getRoundNo() + 1);
            if (candidates.isEmpty() && GameConstants.LEVEL_HARD.equals(state.getLevel())) {
                // hard 模式 AI 枯竭 → 用户胜
                win = true;
                winMsg = GameConstants.MSG_WIN_HARD;
            }
        }

        if (win) {
            stateStore.archive(state, GameConstants.RESULT_WIN);
            resp.setIsValid(true);
            resp.setCheckMsg(winMsg);
            resp.setErrorReason("");
            resp.setNextReferenceList(candidates);
            resp.setIsGameOver(true);
            resp.setGameResult(GameConstants.RESULT_WIN);
            resp.setRoundNo(state.getRoundNo());
            resp.setRetryLeft(state.getRetryLeft());
            resp.setRemainSeconds(0);
            resp.setWinTargetCount(target);
            fillHistory(state, resp);
            return resp;
        }

        // 6) 进入下一回合：roundNo+1、retryLeft=2、写入新一轮 roundStartTime（响应返回前）
        state.setRoundNo(state.getRoundNo() + 1);
        state.setRetryLeft(GameConstants.INIT_RETRY);
        state.setRoundErrors(0);
        state.setRoundStartTime(System.currentTimeMillis());
        stateStore.persist(state);

        resp.setIsValid(true);
        resp.setCheckMsg(GameConstants.MSG_CORRECT);
        resp.setErrorReason("");
        resp.setNextReferenceList(candidates);
        resp.setRoundNo(state.getRoundNo());
        resp.setRetryLeft(state.getRetryLeft());
        resp.setRemainSeconds((int) (GameConstants.ROUND_MILLIS / 1000L));
        resp.setIsGameOver(false);
        resp.setWinTargetCount(target);
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
        log.info("飞花令主动放弃：gameId={}", gameId);
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
        // 恢复时若已超时 → 直接判负
        if (stateStore.calcElapsed(state) > GameConstants.TIMEOUT_MILLIS) {
            return endGame(state, resp, GameConstants.RESULT_LOSE,
                    GameConstants.MSG_LOSE_TIMEOUT, GameConstants.REASON_TIMEOUT);
        }
        resp.setIsValid(true);
        resp.setCheckMsg("");
        resp.setRemainSeconds(stateStore.calcRemainSeconds(state));
        resp.setRetryLeft(state.getRetryLeft());
        resp.setIsGameOver(false);
        return resp;
    }

    /* ==================== 内部实现 ==================== */

    private void validateStart(GameStartRequest req) {
        if (req == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "缺少开局参数");
        }
        String mode = req.getMode();
        if (!GameConstants.MODE_ANCIENT.equals(mode) && !GameConstants.MODE_NORMAL.equals(mode)
                && !GameConstants.MODE_SUPER.equals(mode)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "飞花令模式不合法");
        }
        String level = req.getLevel();
        if (!GameConstants.LEVEL_EASY.equals(level) && !GameConstants.LEVEL_MEDIUM.equals(level)
                && !GameConstants.LEVEL_HARD.equals(level)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "难度不合法");
        }
        List<String> keywords = req.getKeywords();
        if (keywords == null || keywords.isEmpty()
                || keywords.stream().noneMatch(k -> k != null && !k.trim().isEmpty())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "令字/关键字不能为空");
        }
        if (GameConstants.MODE_SUPER.equals(mode)) {
            String superType = req.getSuperType();
            if (!GameConstants.SUPER_CHAR.equals(superType) && !GameConstants.SUPER_TOPIC.equals(superType)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "超级飞花令子类型不合法");
            }
            if (GameConstants.SUPER_CHAR.equals(superType)) {
                long distinct = keywords.stream()
                        .map(k -> k == null ? "" : verifyService.normalize(k))
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .count();
                if (distinct < 2) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "多关键字模式至少需 2 个汉字");
                }
            }
        }
    }

    private List<String> normalizeKeywords(List<String> keywords) {
        List<String> result = new ArrayList<>();
        if (keywords != null) {
            for (String kw : keywords) {
                if (kw != null && !kw.trim().isEmpty()) {
                    result.add(kw.trim());
                }
            }
        }
        return result;
    }

    /** 本地规则校验：返回失败原因，通过返回 null。 */
    private String ruleFailReason(GameRuntimeState state, String normalized) {
        String mode = state.getMode();
        List<String> keywords = state.getKeywords();
        if (GameConstants.MODE_ANCIENT.equals(mode)) {
            char target = firstChar(keywords);
            int pos = verifyService.indexOfHanzi(normalized, target);
            if (pos < 0) {
                return GameConstants.REASON_NOT_CONTAIN;
            }
            // 位次由后端 roundNo 决定，第 N 回合令字须在第 N 个汉字位置
            if (pos != state.getRoundNo()) {
                return GameConstants.REASON_POSITION;
            }
            return null;
        }
        if (GameConstants.MODE_NORMAL.equals(mode)) {
            return verifyService.indexOfHanzi(normalized, firstChar(keywords)) > 0
                    ? null : GameConstants.REASON_NOT_CONTAIN;
        }
        if (GameConstants.MODE_SUPER.equals(mode) && GameConstants.SUPER_CHAR.equals(state.getSuperType())) {
            for (String kw : keywords) {
                if (verifyService.indexOfHanzi(normalized, firstCharOf(kw)) <= 0) {
                    return GameConstants.REASON_NOT_ALL_KEYWORDS;
                }
            }
            return null;
        }
        // super-topic 无字符串规则，交由主题 LLM 判断
        return null;
    }

    /** 生成候选并按规则 + 判重 + 真实性二次过滤；position 为下一回合（ancient 位次）序号。 */
    private List<SentenceRef> generateAndFilterCandidates(GameRuntimeState state, int position) {
        List<String> history = stateStore.loadHistory(state);
        String userPrompt = buildGenPrompt(state, position, history);
        String moduleCode = GameConstants.genModuleCode(state.getMode());

        String raw;
        try {
            raw = aiService.chat(userPrompt, GameConstants.MODEL_FALLBACK, moduleCode);
        } catch (Exception e) {
            log.warn("候选诗句生成失败：gameId={}，error={}", state.getGameId(), e.getMessage());
            return Collections.emptyList();
        }

        List<SentenceRef> parsed = verifyService.parseCandidates(raw);
        List<SentenceRef> keep = new ArrayList<>();
        for (SentenceRef candidate : parsed) {
            String s = verifyService.normalize(candidate.getSentence());
            if (s.isEmpty()) {
                continue;
            }
            if (!matchCandidate(state, s, position)) {
                continue;
            }
            if (stateStore.isDuplicate(state, s)) {
                continue;
            }
            if (!verifyService.verifyReal(s)) {
                continue;
            }
            candidate.setSentence(s);
            keep.add(candidate);
            if (keep.size() >= 5) {
                break;
            }
        }
        return keep;
    }

    private boolean matchCandidate(GameRuntimeState state, String s, int position) {
        String mode = state.getMode();
        List<String> keywords = state.getKeywords();
        if (GameConstants.MODE_ANCIENT.equals(mode)) {
            return verifyService.indexOfHanzi(s, firstChar(keywords)) == position;
        }
        if (GameConstants.MODE_NORMAL.equals(mode)) {
            return verifyService.indexOfHanzi(s, firstChar(keywords)) > 0;
        }
        if (GameConstants.MODE_SUPER.equals(mode) && GameConstants.SUPER_CHAR.equals(state.getSuperType())) {
            for (String kw : keywords) {
                if (verifyService.indexOfHanzi(s, firstCharOf(kw)) <= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private String buildGenPrompt(GameRuntimeState state, int position, List<String> history) {
        String historyStr = String.join("，", history);
        String mode = state.getMode();
        if (GameConstants.MODE_ANCIENT.equals(mode)) {
            return "令字：" + state.getKeywords().get(0) + "\n当前回合序号roundNo：" + position
                    + "\n已使用诗句列表：" + historyStr + "\n生成符合古制飞花令的候选诗句，输出纯JSON数组。";
        }
        if (GameConstants.MODE_NORMAL.equals(mode)) {
            return "令字：" + state.getKeywords().get(0) + "\n已使用诗句列表：" + historyStr
                    + "\n生成符合诗词大会标准版飞花令的候选诗句，输出纯JSON数组。";
        }
        return "super子类型：" + state.getSuperType() + "\n关键字/主题：" + String.join("，", state.getKeywords())
                + "\n已使用诗句列表：" + historyStr + "\n生成超级飞花令候选诗句，输出纯JSON数组。";
    }

    /** 失败处理：消耗重试，累计 3 次判负；答错不重置计时。 */
    private GamePlayResponse reject(GameRuntimeState state, GamePlayResponse resp, String reason) {
        state.setRoundErrors(state.getRoundErrors() + 1);
        state.setRetryLeft(Math.max(0, state.getRetryLeft() - 1));
        resp.setIsValid(false);
        resp.setErrorReason(reason);
        resp.setNextReferenceList(null);
        resp.setRemainSeconds(stateStore.calcRemainSeconds(state));

        if (state.getRoundErrors() >= GameConstants.MAX_ERRORS) {
            stateStore.archive(state, GameConstants.RESULT_LOSE);
            resp.setCheckMsg(GameConstants.MSG_LOSE_ERRORS);
            resp.setIsGameOver(true);
            resp.setGameResult(GameConstants.RESULT_LOSE);
            resp.setRetryLeft(0);
            resp.setRoundNo(state.getRoundNo());
            return resp;
        }
        stateStore.persist(state);
        resp.setCheckMsg(GameConstants.REASON_DUPLICATE.equals(reason)
                ? GameConstants.MSG_DUPLICATE
                : (state.getRoundErrors() == 1 ? GameConstants.MSG_ERROR_RETRY_1
                : GameConstants.MSG_ERROR_RETRY_2));
        resp.setIsGameOver(false);
        resp.setGameResult("");
        resp.setRetryLeft(state.getRetryLeft());
        resp.setRoundNo(state.getRoundNo());
        return resp;
    }

    /** 结束对局（超时等）。 */
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
        resp.setNextReferenceList(null);
        fillHistory(state, resp);
        return resp;
    }

    private String overMsg(GameRuntimeState state) {
        String result = state.getGameResult();
        if (GameConstants.RESULT_WIN.equals(result)) {
            if (GameConstants.LEVEL_EASY.equals(state.getLevel())) {
                return GameConstants.MSG_WIN_EASY;
            }
            if (GameConstants.LEVEL_MEDIUM.equals(state.getLevel())) {
                return GameConstants.MSG_WIN_MEDIUM;
            }
            return GameConstants.MSG_WIN_HARD;
        }
        if (GameConstants.RESULT_GIVEUP.equals(result)) {
            return GameConstants.MSG_GIVEUP;
        }
        return GameConstants.MSG_LOSE_ERRORS;
    }

    private void fillHistory(GameRuntimeState state, GamePlayResponse resp) {
        resp.setHistorySentences(stateStore.loadHistory(state));
        resp.setHistoryCount(state.getValidCount());
    }

    private boolean isSuperTopic(GameRuntimeState state) {
        return GameConstants.MODE_SUPER.equals(state.getMode())
                && GameConstants.SUPER_TOPIC.equals(state.getSuperType());
    }

    private String topicOf(GameRuntimeState state) {
        List<String> keywords = state.getKeywords();
        return (keywords == null || keywords.isEmpty()) ? "" : keywords.get(0);
    }

    private char firstChar(List<String> keywords) {
        return (keywords == null || keywords.isEmpty()) ? 0 : firstCharOf(keywords.get(0));
    }

    private char firstCharOf(String keyword) {
        return verifyService.firstHanzi(keyword);
    }
}
