package com.smartexpense.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartexpense.vo.DashboardStatsVO;
import com.smartexpense.vo.ReimbursementVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsCacheTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    private StatsCache statsCache;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 与 Spring Boot 自动配置一致：注册 jsr310 模块，LocalDateTime 才能正常序列化
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        statsCache = new StatsCache(redisTemplate, objectMapper);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private DashboardStatsVO buildStats() {
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setPendingApproval(3);
        vo.setMonthTotalAmount(new BigDecimal("12800.50"));
        vo.setReimbursementCount(6);
        vo.setInvoiceCount(9);
        vo.setMyDraftCount(1);
        vo.setMyRejectedCount(2);
        vo.setUserCount(4);
        vo.setDeptCount(2);
        ReimbursementVO r = new ReimbursementVO();
        r.setId(1L);
        r.setOrderNo("BX20260801000001");
        r.setAmount(new BigDecimal("1000.00"));
        r.setCreateTime(LocalDateTime.of(2026, 8, 1, 10, 30));
        vo.setRecentReimbursements(List.of(r));
        return vo;
    }

    @Test
    void getOrLoad_cacheHit_shouldReturnDeserializedValue() throws Exception {
        String json = objectMapper.writeValueAsString(buildStats());
        when(valueOps.get("stats:dashboard:1:2:all")).thenReturn(json);
        AtomicBoolean loaderCalled = new AtomicBoolean(false);

        DashboardStatsVO result = statsCache.getOrLoad("stats:dashboard:1:2:all",
                DashboardStatsVO.class, () -> {
                    loaderCalled.set(true);
                    return buildStats();
                });

        assertEquals(false, loaderCalled.get()); // 命中缓存不回源
        assertEquals(3, result.getPendingApproval());
        assertEquals(0, new BigDecimal("12800.50").compareTo(result.getMonthTotalAmount()));
        // LocalDateTime 反序列化正确
        assertEquals(LocalDateTime.of(2026, 8, 1, 10, 30), result.getRecentReimbursements().get(0).getCreateTime());
    }

    @Test
    void getOrLoad_cacheMiss_shouldLoadAndCache() {
        when(valueOps.get("k")).thenReturn(null);
        DashboardStatsVO vo = buildStats();

        DashboardStatsVO result = statsCache.getOrLoad("k", DashboardStatsVO.class, () -> vo);

        assertNotNull(result);
        verify(valueOps).set(eq("k"), anyString(), eq(Duration.ofSeconds(300)));
    }

    @Test
    void getOrLoad_redisDownOnGet_shouldFallthroughToLoader() {
        when(valueOps.get("k")).thenThrow(new RuntimeException("connection refused"));
        AtomicBoolean loaderCalled = new AtomicBoolean(false);

        statsCache.getOrLoad("k", DashboardStatsVO.class, () -> {
            loaderCalled.set(true);
            return buildStats();
        });

        assertEquals(true, loaderCalled.get());
    }

    @Test
    void getOrLoad_corruptJson_shouldFallthroughToLoader() {
        when(valueOps.get("k")).thenReturn("{not valid json");
        AtomicBoolean loaderCalled = new AtomicBoolean(false);

        statsCache.getOrLoad("k", DashboardStatsVO.class, () -> {
            loaderCalled.set(true);
            return buildStats();
        });

        assertEquals(true, loaderCalled.get());
    }

    @Test
    void getOrLoad_redisDownOnPut_shouldStillReturnValue() {
        when(valueOps.get("k")).thenReturn(null);
        doThrow(new RuntimeException("connection refused"))
                .when(valueOps).set(eq("k"), anyString(), any(Duration.class));

        DashboardStatsVO result = statsCache.getOrLoad("k", DashboardStatsVO.class, this::buildStats);

        assertNotNull(result); // 缓存写失败不影响返回
    }

    @Test
    void getOrLoad_loaderReturnsNull_shouldNotCache() {
        when(valueOps.get("k")).thenReturn(null);

        statsCache.getOrLoad("k", DashboardStatsVO.class, () -> null);

        verify(valueOps, never()).set(eq("k"), anyString(), any(Duration.class));
    }

    @Test
    void keyFormat_shouldContainRoleScopeAndDate() {
        assertEquals("stats:dashboard:2:all:3", StatsCache.dashboardKey(2, null, 3L));
        assertEquals("stats:report:1:2:all:all:all",
                StatsCache.reportKey(1, 2L, null, null, null));
        assertEquals("stats:report:3:all:all:2026-08-01:2026-08-31",
                StatsCache.reportKey(3, null, null, "2026-08-01", "2026-08-31"));
    }
}
