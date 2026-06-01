package com.smartexpense.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("reimbursement")
public class Reimbursement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String reimburseNo;

    private BigDecimal totalAmount;

    private Integer invoiceCount;

    /** 状态: 0草稿 1待审批 2审批中 3已通过 4已驳回 5已打款 */
    private Integer status;

    private String rejectReason;

    /** 报销说明 */
    @Size(max = 500, message = "报销说明不能超过500字")
    private String remark;

    private LocalDateTime payTime;

    /** 打款人ID */
    private Long payUserId;

    /** 打款人姓名 */
    private String payUserName;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
