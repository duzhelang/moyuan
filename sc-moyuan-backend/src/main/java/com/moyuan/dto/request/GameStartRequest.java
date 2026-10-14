package com.moyuan.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 飞花令开局请求。
 */
@Data
public class GameStartRequest {

    /** 模式：ancient/normal/super */
    private String mode;

    /** 难度：easy/medium/hard */
    private String level;

    /** 超级飞花令子类型：char/topic（仅 super 需要） */
    private String superType;

    /** 令字/关键字列表（super-char 为多关键字，super-topic 为题述） */
    private List<String> keywords;

    /** 输入方式：text 文本 / voice 语音（仅标识，校验逻辑相同） */
    private String inputType = "text";
}
