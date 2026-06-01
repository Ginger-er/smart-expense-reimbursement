package com.smartexpense.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("trip")
public class Trip {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String tripNo;

    @NotBlank(message = "目的地不能为空")
    private String destination;

    @NotBlank(message = "出差事由不能为空")
    private String purpose;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    private BigDecimal budgetAmount;

    /** 状态: 0草稿 1已提交 2审批中 3已通过 4已驳回 */
    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
