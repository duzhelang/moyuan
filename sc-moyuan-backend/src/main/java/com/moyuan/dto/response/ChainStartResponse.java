package com.moyuan.dto.response;

/**
 * 诗词接龙开局响应（含 AI 起句）。
 * 注：boolean 字段以 isXxx 形式对外暴露，保证 JSON 字段名与前端约定一致。
 */
public class ChainStartResponse {

    private String gameId;
    private int chainCount;
    private SentenceRef startSentence;
    private boolean isGuest;

    /** 选择范围朝代 ID；为空表示不限朝代（中国古典诗词） */
    private Long dynastyId;

    /** 选择范围朝代名称；为空表示不限朝代 */
    private String dynastyName;

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
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

    public int getChainCount() {
        return chainCount;
    }

    public void setChainCount(int chainCount) {
        this.chainCount = chainCount;
    }

    public SentenceRef getStartSentence() {
        return startSentence;
    }

    public void setStartSentence(SentenceRef startSentence) {
        this.startSentence = startSentence;
    }

    public boolean getIsGuest() {
        return isGuest;
    }

    public void setIsGuest(boolean isGuest) {
        this.isGuest = isGuest;
    }
}
