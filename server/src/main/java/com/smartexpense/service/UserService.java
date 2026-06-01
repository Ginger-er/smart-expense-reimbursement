package com.smartexpense.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartexpense.entity.SysUser;
import com.smartexpense.vo.UserVO;

public interface UserService {

    SysUser create(SysUser user);

    /** 用户自助注册（角色固定为员工） */
    SysUser register(SysUser user);

    SysUser update(SysUser user);

    void delete(Long id);

    SysUser getById(Long id);

    Page<UserVO> list(Integer pageNum, Integer pageSize, String keyword, Long deptId, Integer status);

    SysUser login(String username, String password);

    /** 修改当前用户密码 */
    void updatePassword(Long userId, String oldPassword, String newPassword);

    /** 更新当前用户个人信息（姓名、手机号） */
    void updateProfile(Long userId, String realName, String phone);
}
