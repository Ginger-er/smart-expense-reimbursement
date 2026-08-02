package com.smartexpense.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.smartexpense.entity.SysUser;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.ReportMapper;
import com.smartexpense.redis.StatsCache;
import com.smartexpense.mapper.SysUserMapper;
import com.smartexpense.service.ReimbursementService;
import com.smartexpense.service.ReportService;
import com.smartexpense.vo.ReimbursementExportVO;
import com.smartexpense.vo.ReportStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final SysUserMapper userMapper;
    private final ReimbursementService reimbursementService;
    private final StatsCache statsCache;

    @Override
    public ReportStatsVO stats(String startDate, String endDate) {
        validateDateParam(startDate);
        validateDateParam(endDate);
        SysUser current = userMapper.selectById(StpUtil.getLoginIdAsLong());
        // 数据范围：员工看自己、领导看本部门、财务/管理员看全部
        Long userId = current.getRole() == 1 ? current.getId() : null;
        Long deptId = current.getRole() == 2 ? current.getDeptId() : null;
        // 领导部门信息缺失时不允许查询，否则部门过滤条件不生效会看到全公司数据
        if (current.getRole() == 2 && deptId == null) {
            throw new BusinessException("您的部门信息缺失，请联系管理员");
        }

        // 统计缓存：7 条聚合 SQL（含 GROUP BY + JOIN）结果缓存 5 分钟，
        // key 含角色/用户/部门/日期范围，Redis 故障自动回源数据库
        String cacheKey = StatsCache.reportKey(current.getRole(), userId, deptId, startDate, endDate);
        return statsCache.getOrLoad(cacheKey, ReportStatsVO.class, () -> {
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
        });
    }

    @Override
    public List<ReimbursementExportVO> exportList(String startDate, String endDate) {
        // 复用报销导出的数据范围过滤，排除草稿（与报表明细口径一致）
        return reimbursementService.exportList(null, null, startDate, endDate).stream()
                .filter(e -> e.getStatus() != null && e.getStatus() != 0)
                .collect(Collectors.toList());
    }

    /** 日期参数必须为 yyyy-MM-dd：非法值既不参与 SQL 也不进缓存 key，避免任意字符串撑爆 Redis */
    private void validateDateParam(String date) {
        if (date != null && !date.isEmpty()) {
            try {
                LocalDate.parse(date);
            } catch (Exception e) {
                throw new BusinessException("日期格式错误，应为 yyyy-MM-dd");
            }
        }
    }
}
