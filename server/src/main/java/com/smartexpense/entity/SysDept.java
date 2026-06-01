package com.smartexpense.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_dept")
public class SysDept {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String deptName;

    private Long leaderId;

    private Integer sortOrder;

    /** 状态: 1正常 0停用 */
    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;
}
