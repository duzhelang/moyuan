package com.moyuan.dto.response;

import java.util.List;

/**
 * 飞花令开局响应。
 * 注：boolean 字段以 isXxx 形式对外暴露（getIsXxx/setIsXxx），保证 JSON 字段名与前端约定一致。
 */
public class GameStartResponse {

    private String gameId;
    private String mode;
    private String level;
    private List<String> keywords;
    private Integer winTargetCount;
    private SentenceRef startSentence;
    private boolean isGuest;

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
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

    public Integer getWinTargetCount() {
        return winTargetCount;
    }

    public void setWinTargetCount(Integer winTargetCount) {
        this.winTargetCount = winTargetCount;
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
