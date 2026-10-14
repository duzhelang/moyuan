package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 游戏对局主表实体。
 * 飞花令与诗词接龙共用，通过 game_type 区分。
 */
@Data
@TableName("game_record")
public class GameRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对局唯一ID */
    private String gameId;

    /** 玩法类型：feihua-飞花令，chain-诗词接龙 */
    private String gameType;

    /** 所属用户ID（游客为null） */
    private Long userId;

    /** 是否游客对局：0-否，1-是 */
    private Integer isGuest;

    /** 飞花令模式：ancient/normal/super；接龙为null */
    private String mode;

    /** 飞花令难度：easy/medium/hard；接龙为null */
    private String level;

    /** 超级飞花令子类型：char/topic */
    private String superType;

    /** 令字/关键字（逗号分隔）或主题描述 */
    private String keywords;

    /** 有效诗句条数（接龙即链长） */
    private Integer validCount;

    /** 总回合数 */
    private Integer roundNo;

    /** 对局结果：win/lose/giveup */
    private String gameResult;

    /** 当前回合开始时间 */
    private LocalDateTime roundStartTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 结束时间 */
    private LocalDateTime endTime;
}
