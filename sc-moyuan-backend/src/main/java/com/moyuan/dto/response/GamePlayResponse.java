package com.moyuan.dto.response;

import java.util.List;

/**
 * 对局作答/回合响应（飞花令与接龙共用）。
 * 注：boolean 字段以 isXxx 形式对外暴露（getIsXxx/setIsXxx），保证 JSON 字段名与前端约定一致。
 */
public class GamePlayResponse {

    private String gameId;

    /** 本轮作答是否合法 */
    private boolean isValid;

    /** 飞花令模式（接龙为 null） */
    private String mode;

    /** 飞花令难度（接龙为 null） */
    private String level;

    /** 令字/关键字（接龙为 null） */
    private List<String> keywords;

    /** 接龙选择范围朝代 ID；为空表示不限朝代（中国古典诗词） */
    private Long dynastyId;

    /** 接龙选择范围朝代名称；为空表示不限朝代 */
    private String dynastyName;

    /** 当前回合 */
    private int roundNo;

    /** 裁判提示文案 */
    private String checkMsg;

    /** 失败原因（成功为空） */
    private String errorReason;

    /** LLM 生成、后端过滤后的候选诗句（飞花令） */
    private List<SentenceRef> nextReferenceList;

    /** AI 接句（接龙，作为下一轮待接句）；AI 接不上时为空 */
    private SentenceRef nextSentence;

    /** 后端按 gameId 查库/查缓存返回的历史诗句 */
    private List<String> historySentences;

    /** 当前有效条数（接龙即链长） */
    private int historyCount;

    /** 接龙链长（接龙专用，与 historyCount 同源；飞花令为 0） */
    private int chainCount;

    /** 目标胜利条数；hard 为 null */
    private Integer winTargetCount;

    /** 是否结束 */
    private boolean isGameOver;

    /** 对局结果："" | win | lose | giveup */
    private String gameResult = "";

    /** 剩余倒计时秒数（答错时为未重置的实时剩余值） */
    private int remainSeconds;

    /** 本回合剩余重试次数（初始 2） */
    private int retryLeft;

    /** 是否游客 */
    private boolean isGuest;

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(boolean isValid) {
        this.isValid = isValid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public Long getDynastyId() {
        return dynastyId;
    }

    public void setDynastyId(Long dynastyId) {
        this.dynastyId = dynastyId;
    }

    public String getDynastyName() {
        return dynastyName;
    }

    public void setDynastyName(String dynastyName) {
        this.dynastyName = dynastyName;
    }

    public int getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(int roundNo) {
        this.roundNo = roundNo;
    }

    public String getCheckMsg() {
        return checkMsg;
    }

    public void setCheckMsg(String checkMsg) {
        this.checkMsg = checkMsg;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }

    public List<SentenceRef> getNextReferenceList() {
        return nextReferenceList;
    }

    public void setNextReferenceList(List<SentenceRef> nextReferenceList) {
        this.nextReferenceList = nextReferenceList;
    }

    public SentenceRef getNextSentence() {
        return nextSentence;
    }

    public void setNextSentence(SentenceRef nextSentence) {
        this.nextSentence = nextSentence;
    }

    public List<String> getHistorySentences() {
        return historySentences;
    }

    public void setHistorySentences(List<String> historySentences) {
        this.historySentences = historySentences;
    }

    public int getHistoryCount() {
        return historyCount;
    }

    public void setHistoryCount(int historyCount) {
        this.historyCount = historyCount;
    }

    public int getChainCount() {
        return chainCount;
    }

    public void setChainCount(int chainCount) {
        this.chainCount = chainCount;
    }

    public Integer getWinTargetCount() {
        return winTargetCount;
    }

    public void setWinTargetCount(Integer winTargetCount) {
        this.winTargetCount = winTargetCount;
    }

    public boolean getIsGameOver() {
        return isGameOver;
    }

    public void setIsGameOver(boolean isGameOver) {
        this.isGameOver = isGameOver;
    }

    public String getGameResult() {
        return gameResult;
    }

    public void setGameResult(String gameResult) {
        this.gameResult = gameResult;
    }

    public int getRemainSeconds() {
        return remainSeconds;
    }

    public void setRemainSeconds(int remainSeconds) {
        this.remainSeconds = remainSeconds;
    }

    public int getRetryLeft() {
        return retryLeft;
    }

    public void setRetryLeft(int retryLeft) {
        this.retryLeft = retryLeft;
    }

    public boolean getIsGuest() {
        return isGuest;
    }

    public void setIsGuest(boolean isGuest) {
        this.isGuest = isGuest;
    }
}
