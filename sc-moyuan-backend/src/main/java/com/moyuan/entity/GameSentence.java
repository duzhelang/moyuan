package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对局有效诗句明细表实体。
 * 登录用户实时写入，判重按 game_id 查此表；游客仅存 Redis。
 */
@Data
@TableName("game_sentence")
public class GameSentence {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对局ID */
    private String gameId;

    /** 提交用户ID */
    private Long userId;

    /** 所属回合 */
    private Integer roundNo;

    /** 诗句原文 */
    private String sentence;

    /** 诗题 */
    private String title;

    /** 作者 */
    private String author;

    /** 来源：user-用户提交，ai-AI候选或接句 */
    private String source;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
