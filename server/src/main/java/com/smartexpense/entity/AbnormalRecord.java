package com.smartexpense.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 费用异常预警记录：由规则引擎扫描生成，财务/管理员在预警列表中处理。
 */
@Data
@TableName("abnormal_record")
public class AbnormalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则编码：A001重复发票 A002日期异常 A003金额突增 */
    private String ruleCode;

    private String ruleName;

    /** 业务去重键：同规则同业务只记录一次（如 发票号|用户、发票ID、用户|月份） */
    private String bizKey;

    private Long reimbursementId;

    private Long invoiceId;

    private Long userId;

    private String message;

    /** 是否已处理：0未处理 1已处理 */
    private Integer handled;

    private LocalDateTime handleTime;

    private LocalDateTime createTime;
}
