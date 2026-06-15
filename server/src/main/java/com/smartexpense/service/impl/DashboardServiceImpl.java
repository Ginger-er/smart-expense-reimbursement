package com.smartexpense.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.smartexpense.entity.SysUser;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.DashboardMapper;
import com.smartexpense.redis.StatsCache;
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
    private final StatsCache statsCache;

    @Override
    public DashboardStatsVO stats() {
        SysUser current = userMapper.selectById(StpUtil.getLoginIdAsLong());
        // 数据范围：员工看自己、领导看本部门、财务/管理员看全部
        Long userId = current.getRole() == 1 ? current.getId() : null;
        Long deptId = current.getRole() == 2 ? current.getDeptId() : null;
        // 领导部门信息缺失时不允许查询，否则部门过滤条件不生效会看到全公司数据
        if (current.getRole() == 2 && deptId == null) {
            throw new BusinessException("您的部门信息缺失，请联系管理员");
        }

        // 统计缓存：key 含角色/用户/部门，保证数据范围不串；5 分钟 TTL，Redis 故障自动回源
        String cacheKey = StatsCache.dashboardKey(current.getRole(), userId, deptId);
        return statsCache.getOrLoad(cacheKey, DashboardStatsVO.class, () -> {
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
        });
    }
}
