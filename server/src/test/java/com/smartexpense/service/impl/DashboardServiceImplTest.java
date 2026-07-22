package com.smartexpense.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.smartexpense.entity.SysUser;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.DashboardMapper;
import com.smartexpense.mapper.SysUserMapper;
import com.smartexpense.redis.StatsCache;
import com.smartexpense.vo.DashboardStatsVO;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private DashboardMapper dashboardMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private StatsCache statsCache;

    @InjectMocks
    private DashboardServiceImpl service;

    private SysUser user(long id, int role, Long deptId) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setRole(role);
        u.setDeptId(deptId);
        return u;
    }

    @Test
    void stats_manager_shouldUseScopedCacheKey() {
        lenient().when(statsCache.getOrLoad(anyString(), any(), any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(2)).get());
        when(dashboardMapper.countPendingReimbursement(any(), any())).thenReturn(2);
        when(dashboardMapper.countPendingTrip(any(), any())).thenReturn(1);
        when(dashboardMapper.sumMonthTotal(any(), any(), anyString())).thenReturn(new BigDecimal("5000"));
        when(dashboardMapper.countReimbursement(any(), any())).thenReturn(6);
        when(dashboardMapper.countInvoice(any(), any())).thenReturn(9);
        when(dashboardMapper.countMyDraft(anyLong())).thenReturn(1);
        when(dashboardMapper.countMyRejected(anyLong())).thenReturn(2);
        when(dashboardMapper.countUser()).thenReturn(4);
        when(dashboardMapper.countDept()).thenReturn(2);
        when(dashboardMapper.recentReimbursements(any(), any())).thenReturn(List.of());

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(userMapper.selectById(3L)).thenReturn(user(3L, 2, 2L));

            DashboardStatsVO result = service.stats();

            assertEquals(3, result.getPendingApproval()); // 2 报销 + 1 出差
            assertEquals(0, new BigDecimal("5000").compareTo(result.getMonthTotalAmount()));
        }
        // 缓存 key 含角色与部门范围
        verify(statsCache).getOrLoad(eq("stats:dashboard:2:all:2"), any(), any());
    }

    @Test
    void stats_managerWithoutDept_shouldThrow() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(userMapper.selectById(3L)).thenReturn(user(3L, 2, null)); // 部门缺失

            assertThrows(BusinessException.class, () -> service.stats());
        }
    }

    @Test
    void stats_employee_shouldUseUserScopedKey() {
        lenient().when(statsCache.getOrLoad(anyString(), any(), any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(2)).get());
        when(dashboardMapper.countPendingReimbursement(any(), any())).thenReturn(0);
        when(dashboardMapper.countPendingTrip(any(), any())).thenReturn(0);
        when(dashboardMapper.sumMonthTotal(any(), any(), anyString())).thenReturn(BigDecimal.ZERO);
        when(dashboardMapper.countReimbursement(any(), any())).thenReturn(0);
        when(dashboardMapper.countInvoice(any(), any())).thenReturn(0);
        when(dashboardMapper.countMyDraft(anyLong())).thenReturn(0);
        when(dashboardMapper.countMyRejected(anyLong())).thenReturn(0);
        when(dashboardMapper.countUser()).thenReturn(4);
        when(dashboardMapper.countDept()).thenReturn(2);
        when(dashboardMapper.recentReimbursements(any(), any())).thenReturn(List.of());

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));

            service.stats();

            verify(statsCache).getOrLoad(eq("stats:dashboard:1:2:all"), any(), any());
        }
    }
}
