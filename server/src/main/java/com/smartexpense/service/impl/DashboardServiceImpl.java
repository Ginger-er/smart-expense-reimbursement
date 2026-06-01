package com.smartexpense.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.smartexpense.entity.SysUser;
import com.smartexpense.mapper.DashboardMapper;
import com.smartexpense.mapper.SysUserMapper;
import com.smartexpense.service.DashboardService;
import com.smartexpense.vo.DashboardStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;
    private final SysUserMapper userMapper;

    @Override
    public DashboardStatsVO stats() {
        SysUser current = userMapper.selectById(StpUtil.getLoginIdAsLong());
        // 数据范围：员工看自己、领导看本部门、财务/管理员看全部
        Long userId = current.getRole() == 1 ? current.getId() : null;
        Long deptId = current.getRole() == 2 ? current.getDeptId() : null;

        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setPendingApproval(dashboardMapper.countPendingReimbursement(userId, deptId)
                + dashboardMapper.countPendingTrip(userId, deptId));
        String monthStart = LocalDate.now().withDayOfMonth(1) + " 00:00:00";
        vo.setMonthTotalAmount(dashboardMapper.sumMonthTotal(userId, deptId, monthStart));
        vo.setReimbursementCount(dashboardMapper.countReimbursement(userId, deptId));
        vo.setInvoiceCount(dashboardMapper.countInvoice(userId, deptId));
        vo.setMyDraftCount(dashboardMapper.countMyDraft(current.getId()));
        vo.setMyRejectedCount(dashboardMapper.countMyRejected(current.getId()));
        vo.setUserCount(dashboardMapper.countUser());
        vo.setDeptCount(dashboardMapper.countDept());
        vo.setRecentReimbursements(dashboardMapper.recentReimbursements(userId, deptId));
        return vo;
    }
}
