package com.smartexpense.redis;

import com.smartexpense.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLockTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private RedisLock redisLock;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void tryLock_redisOk_shouldReturnTrue() {
        when(valueOps.setIfAbsent(eq("k"), eq("v"), any(Duration.class))).thenReturn(true);

        assertTrue(redisLock.tryLock("k", "v", 10));
    }

    @Test
    void tryLock_valueHeldByOther_shouldReturnFalse() {
        when(valueOps.setIfAbsent(eq("k"), eq("v"), any(Duration.class))).thenReturn(false);

        assertFalse(redisLock.tryLock("k", "v", 10));
    }

    @Test
    void tryLock_redisDown_shouldFailOpen() {
        when(valueOps.setIfAbsent(eq("k"), eq("v"), any(Duration.class)))
                .thenThrow(new RuntimeException("connection refused"));

        // Redis 挂了降级为无锁执行，业务由 DB 状态机兜底
        assertTrue(redisLock.tryLock("k", "v", 10));
    }

    @Test
    void unlock_shouldExecuteLuaScript() {
        redisLock.unlock("k", "v");

        verify(redisTemplate).execute(any(DefaultRedisScript.class), anyList(), eq("v"));
    }

    @Test
    void unlock_redisDown_shouldSwallowException() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString()))
                .thenThrow(new RuntimeException("connection refused"));

        redisLock.unlock("k", "v"); // 不应抛出异常
    }

    @Test
    void executeWithLock_lockAcquired_shouldRunActionAndUnlock() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        AtomicBoolean ran = new AtomicBoolean(false);

        String result = redisLock.executeWithLock("k", 10, () -> {
            ran.set(true);
            return "ok";
        });

        assertTrue(ran.get());
        assertTrue("ok".equals(result));
        verify(redisTemplate).execute(any(DefaultRedisScript.class), anyList(), anyString()); // finally 里释放了锁
    }

    @Test
    void executeWithLock_lockBusy_shouldThrowBusinessException() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        AtomicBoolean ran = new AtomicBoolean(false);

        assertThrows(BusinessException.class,
                () -> redisLock.executeWithLock("k", 10, () -> {
                    ran.set(true);
                    return "ok";
                }));
        assertFalse(ran.get()); // 拿不到锁时 action 不执行
    }

    @Test
    void executeWithLock_redisDown_shouldStillRunAction() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RuntimeException("connection refused"));
        AtomicBoolean ran = new AtomicBoolean(false);

        redisLock.executeWithLock("k", 10, () -> {
            ran.set(true);
            return null;
        });

        assertTrue(ran.get());
    }

    @Test
    void executeWithLockAfterCommit_lockAcquired_shouldRunActionAndUnlock() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        AtomicBoolean ran = new AtomicBoolean(false);

        String result = redisLock.executeWithLockAfterCommit("k", 10, () -> {
            ran.set(true);
            return "ok";
        });

        assertTrue(ran.get());
        assertTrue("ok".equals(result));
        // 无事务上下文（单元测试）时立即释放
        verify(redisTemplate).execute(any(DefaultRedisScript.class), anyList(), anyString());
    }

    @Test
    void executeWithLockAfterCommit_lockBusy_shouldThrowBusinessException() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        AtomicBoolean ran = new AtomicBoolean(false);

        assertThrows(BusinessException.class,
                () -> redisLock.executeWithLockAfterCommit("k", 10, () -> {
                    ran.set(true);
                    return "ok";
                }));
        assertFalse(ran.get());
    }

    @Test
    void executeWithLockAfterCommit_withActiveTx_shouldDeferUnlockToAfterCompletion() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        // 模拟事务上下文：注册事务同步后，锁应推迟到事务结束才释放
        TransactionSynchronizationManager.initSynchronization();
        try {
            redisLock.executeWithLockAfterCommit("k", 10, () -> "ok");

            // 事务未结束：锁尚未释放
            verify(redisTemplate, never()).execute(any(DefaultRedisScript.class), anyList(), anyString());

            // 模拟 Spring 在事务提交后触发的回调
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        // 提交回调后才释放锁
        verify(redisTemplate).execute(any(DefaultRedisScript.class), anyList(), anyString());
    }
}
