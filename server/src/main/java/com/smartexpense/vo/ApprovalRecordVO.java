package com.smartexpense.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApprovalRecordVO {
    private Long id;
    private Long reimbursementId;
    private Long tripId;
    private Long approverId;
    private String approverName;
    /** 操作: 1通过 2驳回 3转办 */
    private Integer action;
    private String comment;
    private String nodeName;
    private LocalDateTime createTime;
}
