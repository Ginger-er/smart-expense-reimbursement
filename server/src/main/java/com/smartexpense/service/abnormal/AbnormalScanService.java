package com.smartexpense.service.abnormal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartexpense.entity.AbnormalRecord;
import com.smartexpense.mapper.AbnormalRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 异常预警扫描服务：定时扫描昨日数据 + 支持手动触发扫描。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbnormalScanService {

    private final AbnormalRuleEngine ruleEngine;
    private final AbnormalRecordMapper recordMapper;

    /** 每天 9:00 自动扫描昨日数据 */
    @Scheduled(cron = "0 0 9 * * ?")
    public void dailyScan() {
        int inserted = scan(LocalDate.now().minusDays(1));
        log.info("每日异常预警扫描完成，新增 {} 条记录", inserted);
    }

    /** 扫描指定日期（当天 00:00 ~ 次日 00:00）的数据，返回新增记录数 */
    public int scan(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<AbnormalRecord> hits = ruleEngine.scan(start, end);

        int inserted = 0;
        for (AbnormalRecord r : hits) {
            if (alreadyExists(r.getRuleCode(), r.getBizKey())) {
                continue; // 同规则同业务只记录一次，避免每天重复堆积
            }
            r.setHandled(0);
            r.setCreateTime(LocalDateTime.now());
            recordMapper.insert(r);
            inserted++;
        }
        return inserted;
    }

    private boolean alreadyExists(String ruleCode, String bizKey) {
        return recordMapper.selectCount(new LambdaQueryWrapper<AbnormalRecord>()
                .eq(AbnormalRecord::getRuleCode, ruleCode)
                .eq(AbnormalRecord::getBizKey, bizKey)) > 0;
    }
}
