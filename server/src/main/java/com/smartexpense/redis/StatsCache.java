package com.smartexpense.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 统计类数据的 Redis 缓存（工作台/报表）。
 *
 * <p>设计取舍（面试常问）：
 * <ul>
 *   <li>只做 TTL 缓存（5 分钟），不做主动失效——统计数据是分析型查询，允许短时陈旧</li>
 *   <li>key 含角色/用户/部门/日期范围，保证同一查询条件命中同一缓存，数据范围不串</li>
 *   <li>Redis 读写异常全部降级为直查数据库，缓存故障不影响业务</li>
 *   <li>序列化复用 Spring 托管的 ObjectMapper（jsr310 已配置），与 REST 接口的 JSON 格式一致</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsCache {

    /** 缓存 5 分钟：统计数据允许近实时 */
    public static final long TTL_SECONDS = 300;

    private static final String DASH_PREFIX = "stats:dashboard:";
    private static final String REPORT_PREFIX = "stats:report:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 读缓存，未命中或缓存故障时回源数据库并回填缓存 */
    public <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, type);
            }
        } catch (Exception e) {
            log.warn("缓存读取/反序列化失败，回源数据库, key: {}", key, e);
        }
        T value = loader.get();
        if (value != null) {
            try {
                redisTemplate.opsForValue().set(key,
                        objectMapper.writeValueAsString(value), Duration.ofSeconds(TTL_SECONDS));
            } catch (Exception e) {
                log.warn("缓存写入失败，忽略, key: {}", key, e);
            }
        }
        return value;
    }

    public static String dashboardKey(int role, Long userId, Long deptId) {
        return DASH_PREFIX + role + ":" + safe(userId) + ":" + safe(deptId);
    }

    public static String reportKey(int role, Long userId, Long deptId, String startDate, String endDate) {
        return REPORT_PREFIX + role + ":" + safe(userId) + ":" + safe(deptId)
                + ":" + safe(startDate) + ":" + safe(endDate);
    }

    private static String safe(Object o) {
        return o == null ? "all" : o.toString();
    }
}
