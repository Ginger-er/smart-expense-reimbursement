package com.smartexpense.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TripDetailVO {
    private Long id;
    private Long userId;
    private String tripNo;
    private String destination;
    private String purpose;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budgetAmount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // 申请人（join user/dept）
    private String applicantName;
    private String deptName;
    private Long deptId;
    // 审批记录
    private List<ApprovalRecordVO> approvalRecords;
}
