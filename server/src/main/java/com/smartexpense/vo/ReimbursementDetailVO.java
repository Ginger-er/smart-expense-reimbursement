package com.smartexpense.vo;

import com.smartexpense.entity.Invoice;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReimbursementDetailVO {
    // 基础
    private Long id;
    private Long userId;
    private String reimburseNo;
    private BigDecimal totalAmount;
    private Integer invoiceCount;
    private Integer status;
    private String rejectReason;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private String payUserName;
    // 申请人（join user/dept）
    private String applicantName;
    private String deptName;
    private Long deptId;
    // 发票 + 审批记录
    private List<Invoice> invoices;
    private List<ApprovalRecordVO> approvalRecords;
}
