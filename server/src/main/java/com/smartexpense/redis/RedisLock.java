package com.smartexpense.redis;

import com.smartexpense.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 基于 Redis 的简单分布式锁。
 *
 * <p>设计要点（面试常问）：
 * <ul>
 *   <li>加锁用 SET key value NX EX ttl —— 原子操作，防死锁（进程挂了锁自动过期）</li>
 *   <li>value 用 UUID 标识持有者，释放时用 Lua 脚本校验 value 一致才删除，防误删他人锁</li>
 *   <li>Redis 故障时 fail-open（视为加锁成功继续执行），由数据库状态机/乐观锁兜底，业务永不因锁阻塞</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLock {

    private final StringRedisTemplate redisTemplate;

    /** 释放锁的 Lua 脚本：仅当锁的持有者是自己时才删除，保证原子性 */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    /**
     * 尝试加锁。
     *
     * @return true 获取成功；Redis 不可用时也返回 true（降级为无锁执行，由 DB 兜底）
     */
    public boolean tryLock(String key, String value, long ttlSeconds) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(key, value, Duration.ofSeconds(ttlSeconds)));
        } catch (Exception e) {
            log.warn("Redis 加锁失败，降级为无锁执行, key: {}", key, e);
            return true;
        }
    }

    /** 释放锁（Lua 原子释放，仅释放自己持有的锁） */
    public void unlock(String key, String value) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, List.of(key), value);
        } catch (Exception e) {
            log.warn("Redis 解锁失败, key: {}", key, e);
        }
    }

    /** 锁内执行：拿不到锁抛业务异常（提示前端"操作进行中，请勿重复操作"） */
    public <T> T executeWithLock(String key, long ttlSeconds, Supplier<T> action) {
        String value = UUID.randomUUID().toString();
        if (tryLock(key, value, ttlSeconds)) {
            try {
                return action.get();
            } finally {
                unlock(key, value);
            }
        }
        throw new BusinessException("操作进行中，请勿重复操作");
    }

    /** 锁内执行（无返回值版本） */
    public void runWithLock(String key, long ttlSeconds, Runnable action) {
        executeWithLock(key, ttlSeconds, () -> {
            action.run();
            return null;
        });
    }
}
