package com.smartexpense.service.abnormal;

import com.smartexpense.entity.AbnormalRecord;
import com.smartexpense.mapper.AbnormalRecordMapper;
import com.smartexpense.redis.RedisLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbnormalScanServiceTest {

    @Mock
    private AbnormalRuleEngine ruleEngine;
    @Mock
    private AbnormalRecordMapper recordMapper;
    @Mock
    private RedisLock redisLock;

    @InjectMocks
    private AbnormalScanService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisLock.tryLock(anyString(), anyString(), anyLong())).thenReturn(true);
        // 防御性预检默认不存在（有唯一索引的库上靠 DuplicateKeyException 兜底）
        lenient().when(recordMapper.selectCount(any())).thenReturn(0L);
    }

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

        int inserted = service.scan(LocalDate.of(2026, 8, 22));

        assertEquals(1, inserted);
        verify(recordMapper).insert(any(AbnormalRecord.class));
    }

    @Test
    void scan_duplicateKey_shouldSkipDedup() {
        when(ruleEngine.scan(any(), any())).thenReturn(List.of(record("A001", "k1")));
        when(recordMapper.insert(any(AbnormalRecord.class)))
                .thenThrow(new DuplicateKeyException("uk_rule_biz 冲突")); // 已存在，跳过

        int inserted = service.scan(LocalDate.of(2026, 8, 22));

        assertEquals(0, inserted);
    }

    @Test
    void scan_existingByPreCheck_shouldSkipWithoutInsert() {
        when(ruleEngine.scan(any(), any())).thenReturn(List.of(record("A001", "k1")));
        when(recordMapper.selectCount(any())).thenReturn(1L); // 预检发现已存在（老库无唯一索引场景）

        int inserted = service.scan(LocalDate.of(2026, 8, 22));

        assertEquals(0, inserted);
        verify(recordMapper, never()).insert(any(AbnormalRecord.class));
    }

    @Test
    void scan_lockBusy_shouldReturnMinusOneWithoutScanning() {
        when(redisLock.tryLock(anyString(), anyString(), anyLong())).thenReturn(false); // 已有扫描在跑

        int inserted = service.scan(LocalDate.of(2026, 8, 22));

        assertEquals(-1, inserted);
        verify(ruleEngine, never()).scan(any(), any());
    }

    @Test
    void scan_shouldSetHandledAndCreateTime() {
        when(ruleEngine.scan(any(), any())).thenReturn(List.of(record("A002", "inv-9")));

        service.scan(LocalDate.of(2026, 8, 22));

        verify(recordMapper).insert(org.mockito.ArgumentMatchers.argThat(r ->
                r.getHandled() != null && r.getHandled() == 0
                        && r.getCreateTime() != null));
    }
}
