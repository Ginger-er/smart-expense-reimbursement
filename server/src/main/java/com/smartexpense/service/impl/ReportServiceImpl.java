package com.smartexpense.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.smartexpense.entity.SysUser;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.ReportMapper;
import com.smartexpense.mapper.SysUserMapper;
import com.smartexpense.service.ReimbursementService;
import com.smartexpense.service.ReportService;
import com.smartexpense.vo.ReimbursementExportVO;
import com.smartexpense.vo.ReportStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final SysUserMapper userMapper;
    private final ReimbursementService reimbursementService;

    @Override
    public ReportStatsVO stats(String startDate, String endDate) {
        SysUser current = userMapper.selectById(StpUtil.getLoginIdAsLong());
        // 数据范围：员工看自己、领导看本部门、财务/管理员看全部
        Long userId = current.getRole() == 1 ? current.getId() : null;
        Long deptId = current.getRole() == 2 ? current.getDeptId() : null;
        // 领导部门信息缺失时不允许查询，否则部门过滤条件不生效会看到全公司数据
        if (current.getRole() == 2 && deptId == null) {
            throw new BusinessException("您的部门信息缺失，请联系管理员");
        }

        ReportStatsVO vo = new ReportStatsVO();
        vo.setTotalAmount(reportMapper.sumTotalAmount(userId, deptId, startDate, endDate));
        int totalCount = reportMapper.countTotal(userId, deptId, startDate, endDate);
        vo.setTotalCount(totalCount);

        int approved = reportMapper.countApproved(userId, deptId, startDate, endDate);
        int rejected = reportMapper.countRejected(userId, deptId, startDate, endDate);
        int decided = approved + rejected;
        vo.setApprovalRate(decided > 0
                ? BigDecimal.valueOf(approved * 100.0 / decided).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        vo.setAvgAmount(totalCount > 0
                ? vo.getTotalAmount().divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        vo.setDeptRanking(reportMapper.deptRanking(userId, deptId, startDate, endDate));
        vo.setExpenseTypes(reportMapper.expenseTypes(userId, deptId, startDate, endDate));
        return vo;
    }

    @Override
    public List<ReimbursementExportVO> exportList(String startDate, String endDate) {
        // 复用报销导出的数据范围过滤，排除草稿（与报表明细口径一致）
        return reimbursementService.exportList(null, null, startDate, endDate).stream()
                .filter(e -> e.getStatus() != null && e.getStatus() != 0)
                .collect(Collectors.toList());
    }
}
