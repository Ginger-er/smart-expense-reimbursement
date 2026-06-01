package com.smartexpense.vo;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 报销单导出 VO（EasyExcel 列定义）
 */
@Data
public class ReimbursementExportVO {

    @ExcelProperty("报销单号")
    @ColumnWidth(20)
    private String orderNo;

    @ExcelProperty("申请人")
    @ColumnWidth(12)
    private String applicantName;

    @ExcelProperty("部门")
    @ColumnWidth(12)
    private String deptName;

    @ExcelProperty("金额(元)")
    @ColumnWidth(14)
    private BigDecimal amount;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String statusText;

    @ExcelProperty("发票数")
    @ColumnWidth(10)
    private Integer invoiceCount;

    @ExcelProperty("报销说明")
    @ColumnWidth(30)
    private String remark;

    @ExcelProperty("提交时间")
    @ColumnWidth(20)
    private String createTime;

    /** 状态码（仅服务端过滤用，不导出到 Excel） */
    @ExcelIgnore
    private Integer status;
}
