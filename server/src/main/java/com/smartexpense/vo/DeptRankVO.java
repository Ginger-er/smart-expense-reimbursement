package com.smartexpense.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeptRankVO {
    /** 部门名称 */
    private String name;
    /** 报销总额 */
    private BigDecimal amount;
}
