package com.smartexpense.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.smartexpense.entity.SysUser;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.ReportMapper;
import com.smartexpense.mapper.SysUserMapper;
import com.smartexpense.redis.StatsCache;
import com.smartexpense.service.ReimbursementService;
import com.smartexpense.vo.ReportStatsVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportMapper reportMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private ReimbursementService reimbursementService;
    @Mock
    private StatsCache statsCache;

    @InjectMocks
    private ReportServiceImpl service;

    private SysUser user(long id, int role, Long deptId) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setRole(role);
        u.setDeptId(deptId);
        return u;
    }

    private void stubCachePassThrough() {
        // 透传：缓存 miss 时直接执行 loader
        lenient().when(statsCache.getOrLoad(anyString(), any(), any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(2)).get());
    }

    @Test
    void stats_employee_shouldUseScopedCacheKey() {
        stubCachePassThrough();
        when(reportMapper.sumTotalAmount(2L, null, "2026-08-01", "2026-08-31"))
                .thenReturn(new BigDecimal("1000"));
        when(reportMapper.countTotal(2L, null, "2026-08-01", "2026-08-31")).thenReturn(2);
        when(reportMapper.countApproved(2L, null, "2026-08-01", "2026-08-31")).thenReturn(1);
        when(reportMapper.countRejected(2L, null, "2026-08-01", "2026-08-31")).thenReturn(1);
        when(reportMapper.deptRanking(any(), any(), any(), any())).thenReturn(List.of());
        when(reportMapper.expenseTypes(any(), any(), any(), any())).thenReturn(List.of());

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));

            ReportStatsVO result = service.stats("2026-08-01", "2026-08-31");

            assertEquals(0, new BigDecimal("1000").compareTo(result.getTotalAmount()));
            assertEquals(2, result.getTotalCount());
            // 审批通过率 = 1/2 = 50.0%
            assertEquals(0, new BigDecimal("50.0").compareTo(result.getApprovalRate()));
        }
        // 缓存 key 含角色/用户/日期范围
        verify(statsCache).getOrLoad(eq("stats:report:1:2:all:2026-08-01:2026-08-31"), any(), any());
    }

    @Test
    void stats_managerWithoutDept_shouldThrow() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(userMapper.selectById(3L)).thenReturn(user(3L, 2, null)); // 部门缺失

            assertThrows(BusinessException.class, () -> service.stats(null, null));
        }
    }

    @Test
    void stats_noDecided_shouldReturnZeroRate() {
        stubCachePassThrough();
        when(reportMapper.sumTotalAmount(any(), any(), any(), any())).thenReturn(new BigDecimal("0"));
        when(reportMapper.countTotal(any(), any(), any(), any())).thenReturn(0);
        when(reportMapper.countApproved(any(), any(), any(), any())).thenReturn(0);
        when(reportMapper.countRejected(any(), any(), any(), any())).thenReturn(0);
        when(reportMapper.deptRanking(any(), any(), any(), any())).thenReturn(List.of());
        when(reportMapper.expenseTypes(any(), any(), any(), any())).thenReturn(List.of());

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(4L);
            when(userMapper.selectById(4L)).thenReturn(user(4L, 4, null));

            ReportStatsVO result = service.stats(null, null);

            assertEquals(0, new BigDecimal("0.0").compareTo(result.getApprovalRate())); // 除零保护
            assertEquals(0, new BigDecimal("0").compareTo(result.getAvgAmount()));
        }
    }
}
