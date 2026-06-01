package com.smartexpense.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    private Long deptId;

    /** 角色: 1员工 2领导 3财务 4管理员 */
    private Integer role;

    private String phone;

    /** 状态: 1正常 0停用 */
    private Integer status;

    /** 密码是否已改: 0初始密码 1已修改 */
    @TableField("pwd_modified")
    private Integer pwdModified;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
