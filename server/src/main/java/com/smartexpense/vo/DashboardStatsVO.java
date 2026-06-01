package com.smartexpense.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardStatsVO {
    /** 待审批数（报销+出差，范围内） */
    private Integer pendingApproval;
    /** 本月报销总额（范围内） */
    private BigDecimal monthTotalAmount;
    /** 报销单总数（范围内） */
    private Integer reimbursementCount;
    /** 发票总数（范围内） */
    private Integer invoiceCount;
    /** 我的草稿数 */
    private Integer myDraftCount;
    /** 我被驳回数 */
    private Integer myRejectedCount;
    /** 用户总数（管理员） */
    private Integer userCount;
    /** 部门总数（管理员） */
    private Integer deptCount;
    /** 最近报销单（范围内前5条） */
    private List<ReimbursementVO> recentReimbursements;
}
