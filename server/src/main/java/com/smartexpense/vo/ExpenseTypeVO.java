package com.smartexpense.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpenseTypeVO {
    /** 费用类型名称 */
    private String name;
    /** 金额合计 */
    private BigDecimal amount;
}
