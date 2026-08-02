package com.smartexpense.service.abnormal;

import com.smartexpense.entity.AbnormalRecord;
import com.smartexpense.mapper.AbnormalRecordMapper;
import com.smartexpense.redis.RedisLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 异常预警扫描服务：定时扫描昨日数据 + 支持手动触发扫描。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbnormalScanService {

    /** 扫描单飞锁 key 前缀：同一天同时只会执行一个扫描任务 */
    private static final String SCAN_LOCK_PREFIX = "lock:abnormal:scan:";

    private final AbnormalRuleEngine ruleEngine;
    private final AbnormalRecordMapper recordMapper;
    private final RedisLock redisLock;

    /** 每天 9:00 自动扫描昨日数据 */
    @Scheduled(cron = "0 0 9 * * ?")
    public void dailyScan() {
        int inserted = scan(LocalDate.now().minusDays(1));
        log.info("每日异常预警扫描完成，新增 {} 条记录", inserted);
    }

    /** 扫描指定日期（当天 00:00 ~ 次日 00:00）的数据，返回新增记录数。
     *  单飞保护：同一天的扫描任务同时只会执行一个（定时任务与手动触发并发时跳过） */
    public int scan(LocalDate date) {
        String token = UUID.randomUUID().toString();
        String lockKey = SCAN_LOCK_PREFIX + date;
        if (!redisLock.tryLock(lockKey, token, 30)) {
            log.info("同日扫描任务已在执行，跳过本次, date: {}", date);
            return 0;
        }
        try {
            return doScan(date);
        } finally {
            redisLock.unlock(lockKey, token);
        }
    }

    private int doScan(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<AbnormalRecord> hits = ruleEngine.scan(start, end);

        int inserted = 0;
        for (AbnormalRecord r : hits) {
            r.setHandled(0);
            r.setCreateTime(LocalDateTime.now());
            try {
                recordMapper.insert(r);
                inserted++;
            } catch (DuplicateKeyException e) {
                // 并发/重复扫描撞唯一键 (rule_code, biz_key)：同规则同业务只记录一次
                log.debug("预警已存在，跳过, ruleCode: {}, bizKey: {}", r.getRuleCode(), r.getBizKey());
            }
        }
        return inserted;
    }
}
