package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.moyuan.common.ResultCode;
import com.moyuan.dto.game.GameConstants;
import com.moyuan.dto.game.GameRuntimeState;
import com.moyuan.dto.response.SentenceRef;
import com.moyuan.entity.GameRecord;
import com.moyuan.entity.GameSentence;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.GameRecordMapper;
import com.moyuan.mapper.GameSentenceMapper;
import com.moyuan.service.GameStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 对局状态存储实现：Redis 保存运行时状态与计时权威，MySQL 保存登录用户对局归档与有效诗句。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameStateStoreImpl implements GameStateStore {

    private final RedisTemplate<String, Object> redisTemplate;
    private final GameRecordMapper gameRecordMapper;
    private final GameSentenceMapper gameSentenceMapper;

    private static final DateTimeFormatter ID_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration TTL = Duration.ofMinutes(GameConstants.TTL_MINUTES);

    @Override
    public GameRuntimeState create(String gameType, Long userId, String mode, String level,
                                   String superType, List<String> keywords, SentenceRef startSentence) {
        String prefix = GameConstants.TYPE_CHAIN.equals(gameType) ? "ch_" : "fh_";
        String gameId = prefix + LocalDate.now().format(ID_DATE) + "_" + randomHex(8);

        GameRuntimeState state = new GameRuntimeState();
        state.setGameId(gameId);
        state.setGameType(gameType);
        state.setUserId(userId);
        state.setGuest(userId == null);
        state.setMode(mode);
        state.setLevel(level);
        state.setSuperType(superType);
        state.setKeywords(keywords);
        state.setLastSentence(startSentence);
        state.setRoundNo(1);
        state.setRoundStartTime(0L);
        state.setRetryLeft(GameConstants.INIT_RETRY);
        state.setValidCount(0);
        state.setRoundErrors(0);
        state.setStatus(GameConstants.STATUS_PLAYING);
        state.setGameResult("");
        persist(state);

        // 登录用户：开局即写入对局主表；游客仅保留 Redis 会话
        if (userId != null) {
            try {
                GameRecord record = new GameRecord();
                record.setGameId(gameId);
                record.setGameType(gameType);
                record.setUserId(userId);
                record.setIsGuest(0);
                record.setMode(mode);
                record.setLevel(level);
                record.setSuperType(superType);
                record.setKeywords(keywordsText(keywords));
                record.setValidCount(0);
                record.setRoundNo(1);
                gameRecordMapper.insert(record);
            } catch (Exception e) {
                log.warn("对局主表落库失败：gameId={}，error={}", gameId, e.getMessage());
            }
        }
        return state;
    }

    @Override
    public GameRuntimeState load(String gameId, Long currentUserId) {
        if (gameId == null || gameId.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "缺少对局ID");
        }
        Object obj = redisTemplate.opsForValue().get(stateKey(gameId));
        if (!(obj instanceof GameRuntimeState state)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "对局不存在或已过期，请重新开局");
        }
        // 归属校验：非游客对局仅允许归属用户操作，防止跨身份读取
        if (state.getUserId() != null && !state.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return state;
    }

    @Override
    public void persist(GameRuntimeState state) {
        redisTemplate.opsForValue().set(stateKey(state.getGameId()), state, TTL);
        // 游客诗句集合可能尚未创建，expire 对不存在键返回 false，无副作用
        redisTemplate.expire(sentenceKey(state.getGameId()), TTL);
    }

    @Override
    public void refreshTtl(GameRuntimeState state) {
        redisTemplate.expire(stateKey(state.getGameId()), TTL);
        redisTemplate.expire(sentenceKey(state.getGameId()), TTL);
    }

    @Override
    public long calcElapsed(GameRuntimeState state) {
        if (state.getRoundStartTime() <= 0) {
            return 0L;
        }
        return System.currentTimeMillis() - state.getRoundStartTime();
    }

    @Override
    public int calcRemainSeconds(GameRuntimeState state) {
        if (state.getRoundStartTime() <= 0) {
            return (int) (GameConstants.ROUND_MILLIS / 1000L);
        }
        long elapsed = calcElapsed(state);
        return (int) Math.max(0L, Math.ceil((GameConstants.ROUND_MILLIS - elapsed) / 1000.0));
    }

    @Override
    public void archive(GameRuntimeState state, String result) {
        state.setStatus(GameConstants.STATUS_OVER);
        state.setGameResult(result);
        persist(state);

        if (state.getUserId() == null) {
            // 游客对局不落库，Redis 会话按 TTL 自然过期
            return;
        }
        try {
            LambdaUpdateWrapper<GameRecord> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(GameRecord::getGameId, state.getGameId())
                    .set(GameRecord::getGameResult, result)
                    .set(GameRecord::getValidCount, state.getValidCount())
                    .set(GameRecord::getRoundNo, state.getRoundNo())
                    .set(GameRecord::getEndTime, LocalDateTime.now());
            if (state.getRoundStartTime() > 0) {
                wrapper.set(GameRecord::getRoundStartTime, toDateTime(state.getRoundStartTime()));
            }
            gameRecordMapper.update(null, wrapper);
        } catch (Exception e) {
            log.warn("对局归档更新失败：gameId={}，result={}，error={}", state.getGameId(), result, e.getMessage());
        }
    }

    @Override
    public boolean isDuplicate(GameRuntimeState state, String sentence) {
        if (state.getUserId() != null) {
            LambdaQueryWrapper<GameSentence> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GameSentence::getGameId, state.getGameId())
                    .eq(GameSentence::getSentence, sentence);
            return gameSentenceMapper.selectCount(wrapper) > 0;
        }
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(sentenceKey(state.getGameId()), sentence));
    }

    @Override
    public void recordSentence(GameRuntimeState state, int roundNo, String sentence,
                               String title, String author, String source) {
        if (state.getUserId() != null) {
            GameSentence entity = new GameSentence();
            entity.setGameId(state.getGameId());
            entity.setUserId(state.getUserId());
            entity.setRoundNo(roundNo);
            entity.setSentence(sentence);
            entity.setTitle(title);
            entity.setAuthor(author);
            entity.setSource(source);
            try {
                gameSentenceMapper.insert(entity);
            } catch (Exception e) {
                // 并发重复由 uk_game_sentence 兜底，插入冲突仅告警
                log.warn("有效诗句落库失败（可能重复）：gameId={}，sentence={}，error={}",
                        state.getGameId(), sentence, e.getMessage());
            }
        } else {
            String key = sentenceKey(state.getGameId());
            redisTemplate.opsForSet().add(key, sentence);
            redisTemplate.expire(key, TTL);
        }
    }

    @Override
    public List<String> loadHistory(GameRuntimeState state) {
        if (state.getUserId() != null) {
            LambdaQueryWrapper<GameSentence> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GameSentence::getGameId, state.getGameId())
                    .orderByAsc(GameSentence::getRoundNo)
                    .orderByAsc(GameSentence::getId);
            List<GameSentence> list = gameSentenceMapper.selectList(wrapper);
            return list.stream().map(GameSentence::getSentence).collect(Collectors.toList());
        }
        Set<Object> members = redisTemplate.opsForSet().members(sentenceKey(state.getGameId()));
        if (members == null || members.isEmpty()) {
            return new ArrayList<>();
        }
        return members.stream().map(String::valueOf).collect(Collectors.toList());
    }

    private String stateKey(String gameId) {
        return GameConstants.REDIS_KEY_PREFIX + gameId;
    }

    private String sentenceKey(String gameId) {
        return GameConstants.REDIS_KEY_PREFIX + gameId + GameConstants.REDIS_SENTENCE_SUFFIX;
    }

    private String keywordsText(List<String> keywords) {
        return keywords == null ? null : String.join(",", keywords);
    }

    private LocalDateTime toDateTime(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }

    private String randomHex(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(Integer.toHexString(RANDOM.nextInt(16)));
        }
        return sb.toString();
    }
}
