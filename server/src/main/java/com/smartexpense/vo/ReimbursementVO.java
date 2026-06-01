package com.smartexpense.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReimbursementVO {
    private Long id;
    private Long userId;
    private String orderNo;
    private String applicantName;
    private String deptName;
    private BigDecimal amount;
    private Integer invoiceCount;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
