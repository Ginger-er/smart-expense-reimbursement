package com.smartexpense.service.abnormal;

import com.smartexpense.entity.AbnormalRecord;
import com.smartexpense.mapper.AbnormalQueryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 三条预警规则 + 规则引擎的单元测试（策略模式核心逻辑）。
 */
@ExtendWith(MockitoExtension.class)
class AbnormalRuleTest {

    @Mock
    private AbnormalQueryMapper queryMapper;

    private final LocalDateTime start = LocalDateTime.of(2026, 8, 22, 0, 0);
    private final LocalDateTime end = LocalDateTime.of(2026, 8, 23, 0, 0);

    @Test
    void duplicateInvoiceRule_shouldBuildRecord() {
        when(queryMapper.duplicateInvoiceNos(start, end)).thenReturn(List.of(
                Map.of("invoiceNo", "INV-001", "userId", 2L, "reimbursementIds", "10,11")));

        DuplicateInvoiceRule rule = new DuplicateInvoiceRule(queryMapper);
        List<AbnormalRecord> records = rule.check(start, end);

        assertEquals(1, records.size());
        AbnormalRecord r = records.get(0);
        assertEquals("A001", r.getRuleCode());
        assertEquals("重复发票", r.getRuleName());
        assertEquals("INV-001|2", r.getBizKey());
        assertTrue(r.getMessage().contains("INV-001"));
        assertTrue(r.getMessage().contains("重复报销"));
    }

    @Test
    void invoiceDateOutOfTripRangeRule_shouldBuildRecord() {
        when(queryMapper.invoiceDateOutOfTripRange(start, end)).thenReturn(List.of(
                Map.of("invoiceId", 5L, "userId", 2L, "invoiceNo", "INV-009",
                        "invoiceDate", "2026-08-20", "tripNo", "CC20260801",
                        "startDate", "2026-08-01", "endDate", "2026-08-15")));

        InvoiceDateOutOfTripRangeRule rule = new InvoiceDateOutOfTripRangeRule(queryMapper);
        List<AbnormalRecord> records = rule.check(start, end);

        assertEquals(1, records.size());
        AbnormalRecord r = records.get(0);
        assertEquals("A002", r.getRuleCode());
        assertEquals("inv-5", r.getBizKey());
        assertEquals(5L, r.getInvoiceId());
        assertTrue(r.getMessage().contains("INV-009"));
        assertTrue(r.getMessage().contains("CC20260801"));
    }

    @Test
    void amountSurgeRule_shouldBuildRecord() {
        when(queryMapper.amountSurgeUsers(any(), any())).thenReturn(List.of(
                Map.of("userId", 2L, "curTotal", 3000.00, "prevTotal", 1000.00)));

        AmountSurgeRule rule = new AmountSurgeRule(queryMapper);
        List<AbnormalRecord> records = rule.check(start, end);

        assertEquals(1, records.size());
        AbnormalRecord r = records.get(0);
        assertEquals("A003", r.getRuleCode());
        assertEquals("2|2026-08", r.getBizKey());
        assertTrue(r.getMessage().contains("1.5"));
    }

    @Test
    void engine_shouldAggregateAllRulesAndSkipFailedRule() {
        AbnormalRule okRule = new AbnormalRule() {
            @Override public String code() { return "X001"; }
            @Override public String name() { return "正常规则"; }
            @Override public List<AbnormalRecord> check(LocalDateTime s, LocalDateTime e) {
                AbnormalRecord r = new AbnormalRecord();
                r.setRuleCode(code());
                r.setBizKey("k1");
                return List.of(r);
            }
        };
        AbnormalRule brokenRule = new AbnormalRule() {
            @Override public String code() { return "X002"; }
            @Override public String name() { return "故障规则"; }
            @Override public List<AbnormalRecord> check(LocalDateTime s, LocalDateTime e) {
                throw new RuntimeException("sql error");
            }
        };

        AbnormalRuleEngine engine = new AbnormalRuleEngine(List.of(okRule, brokenRule));

        List<AbnormalRecord> hits = engine.scan(start, end);

        // 正常规则命中 1 条，故障规则被跳过不影响整体
        assertEquals(1, hits.size());
        assertEquals("X001", hits.get(0).getRuleCode());
    }
}
