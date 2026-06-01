package com.smartexpense.common;

import com.smartexpense.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private SysUser user;
}
