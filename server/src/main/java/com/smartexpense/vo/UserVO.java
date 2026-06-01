package com.smartexpense.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private Long deptId;
    private String deptName;
    private Integer role;
    private String phone;
    private Integer status;
    private Integer pwdModified;
    private LocalDateTime createTime;
}
