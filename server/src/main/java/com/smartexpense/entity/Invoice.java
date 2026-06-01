package com.smartexpense.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("invoice")
public class Invoice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long tripId;

    private Long reimbursementId;

    private String invoiceNo;

    private String invoiceCode;

    private BigDecimal amount;

    private BigDecimal taxAmount;

    private LocalDate invoiceDate;

    /** 类型: 1交通 2住宿 3餐饮 4其他 */
    private Integer type;

    private String sellerName;

    private String buyerName;

    private String fileUrl;

    private String ocrJson;

    /** OCR状态: 0待识别 1成功 2失败 3人工修正 */
    private Integer ocrStatus;

    /** 校验状态: 0未校验 1通过 2失败 */
    private Integer verifyStatus;

    @TableField("create_time")
    private LocalDateTime createTime;
}
