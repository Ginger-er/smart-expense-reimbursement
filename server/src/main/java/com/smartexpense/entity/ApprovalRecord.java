package com.smartexpense.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("approval_record")
public class ApprovalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tripId;

    private Long reimbursementId;

    private Long approverId;

    /** 操作: 1通过 2驳回 3转办 */
    private Integer action;

    private String comment;

    private String nodeName;

    @TableField("create_time")
    private LocalDateTime createTime;
}
