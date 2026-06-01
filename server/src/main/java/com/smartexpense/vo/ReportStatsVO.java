package com.smartexpense.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReportStatsVO {
    /** 报销总额（非草稿） */
    private BigDecimal totalAmount;
    /** 报销单数 */
    private Integer totalCount;
    /** 审批通过率（%） */
    private BigDecimal approvalRate;
    /** 平均单笔金额 */
    private BigDecimal avgAmount;
    /** 部门报销排行 */
    private List<DeptRankVO> deptRanking;
    /** 费用类型分布 */
    private List<ExpenseTypeVO> expenseTypes;
}
