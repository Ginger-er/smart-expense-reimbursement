package com.smartexpense.service.abnormal;

import com.smartexpense.entity.AbnormalRecord;
import com.smartexpense.mapper.AbnormalRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbnormalScanServiceTest {

    @Mock
    private AbnormalRuleEngine ruleEngine;
    @Mock
    private AbnormalRecordMapper recordMapper;

    @InjectMocks
    private AbnormalScanService service;

    private AbnormalRecord record(String code, String bizKey) {
        AbnormalRecord r = new AbnormalRecord();
        r.setRuleCode(code);
        r.setRuleName("规则");
        r.setBizKey(bizKey);
        r.setMessage("预警消息");
        return r;
    }

    @Test
    void scan_shouldInsertNewRecords() {
        when(ruleEngine.scan(any(), any())).thenReturn(List.of(record("A001", "k1")));
        when(recordMapper.selectCount(any())).thenReturn(0L); // 不存在，插入

        int inserted = service.scan(LocalDate.of(2026, 8, 22));

        assertEquals(1, inserted);
        verify(recordMapper).insert(any(AbnormalRecord.class));
    }

    @Test
    void scan_existingRecord_shouldSkipDedup() {
        when(ruleEngine.scan(any(), any())).thenReturn(List.of(record("A001", "k1")));
        when(recordMapper.selectCount(any())).thenReturn(1L); // 已存在，跳过

        int inserted = service.scan(LocalDate.of(2026, 8, 22));

        assertEquals(0, inserted);
        verify(recordMapper, never()).insert(any(AbnormalRecord.class));
    }

    @Test
    void scan_shouldSetHandledAndCreateTime() {
        when(ruleEngine.scan(any(), any())).thenReturn(List.of(record("A002", "inv-9")));
        when(recordMapper.selectCount(any())).thenReturn(0L);

        service.scan(LocalDate.of(2026, 8, 22));

        verify(recordMapper).insert(org.mockito.ArgumentMatchers.argThat(r ->
                r.getHandled() != null && r.getHandled() == 0
                        && r.getCreateTime() != null));
    }
}
