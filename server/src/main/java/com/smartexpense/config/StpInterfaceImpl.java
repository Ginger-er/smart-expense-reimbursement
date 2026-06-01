package com.smartexpense.config;

import cn.dev33.satoken.stp.StpInterface;
import com.smartexpense.entity.SysUser;
import com.smartexpense.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限接口实现：根据登录用户返回角色码，供 @SaCheckRole 使用。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysUserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 本项目用角色码鉴权，不使用细粒度权限
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roles = new ArrayList<>();
        try {
            Long userId = Long.valueOf(loginId.toString());
            SysUser user = userMapper.selectById(userId);
            if (user != null && user.getRole() != null) {
                roles.add(mapRole(user.getRole()));
            }
        } catch (Exception ignored) {
            // 角色查询失败时返回空，接口鉴权会判定为无权限
        }
        return roles;
    }

    private String mapRole(Integer role) {
        switch (role) {
            case 2: return "manager";
            case 3: return "finance";
            case 4: return "admin";
            default: return "employee";
        }
    }
}
