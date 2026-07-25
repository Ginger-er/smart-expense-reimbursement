package com.smartexpense.service.abnormal;

import com.smartexpense.entity.AbnormalRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 规则引擎：持有全部规则策略（Spring 自动注入所有 AbnormalRule 实现），
 * 逐个执行并汇总命中结果；单条规则执行失败不影响整体扫描。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AbnormalRuleEngine {

    private final List<AbnormalRule> rules;

    public List<AbnormalRecord> scan(LocalDateTime start, LocalDateTime end) {
        List<AbnormalRecord> hits = new ArrayList<>();
        for (AbnormalRule rule : rules) {
            try {
                List<AbnormalRecord> found = rule.check(start, end);
                log.info("预警规则 {} ({}) 命中 {} 条", rule.code(), rule.name(), found.size());
                hits.addAll(found);
            } catch (Exception e) {
                // 单条规则失败不影响其他规则与主流程
                log.warn("预警规则 {} 执行失败，跳过", rule.code(), e);
            }
        }
        return hits;
    }
}
