package com.smartexpense.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TripVO {
    private Long id;
    private Long userId;
    private String tripNo;
    private String applicantName;
    private String deptName;
    private String destination;
    private String reason;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private Integer status;
    private LocalDateTime createTime;
}
