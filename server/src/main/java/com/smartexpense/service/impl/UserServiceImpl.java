package com.smartexpense.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartexpense.entity.SysUser;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.SysUserMapper;
import com.smartexpense.service.UserService;
import com.smartexpense.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    private final SysUserMapper userMapper;

    @Override
    @Transactional
    public SysUser create(SysUser user) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, user.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 密码为空时使用默认初始密码，员工登录后自行修改，管理员无需掌握员工密码
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword("123456");
        }
        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        user.setStatus(1);
        user.setPwdModified(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        save(user);

        log.info("用户创建成功, id: {}, username: {}", user.getId(), user.getUsername());
        user.setPassword(null); // 密码哈希不返回给前端
        return user;
    }

    @Override
    @Transactional
    public SysUser register(SysUser user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().length() < 4) {
            throw new BusinessException("密码至少4位");
        }
        if (user.getRealName() == null || user.getRealName().trim().isEmpty()) {
            throw new BusinessException("姓名不能为空");
        }
        // 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, user.getUsername().trim());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        user.setUsername(user.getUsername().trim());
        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        user.setRole(1); // 自助注册固定为员工
        user.setStatus(1);
        user.setPwdModified(1); // 用户自己设置的密码
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        save(user);

        log.info("用户自助注册成功, id: {}, username: {}", user.getId(), user.getUsername());
        user.setPassword(null); // 密码哈希不返回给前端
        return user;
    }

    @Override
    @Transactional
    public SysUser update(SysUser user) {
        SysUser existing = getById(user.getId());
        // 密码不为空时才更新
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        } else {
            user.setPassword(existing.getPassword());
        }
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        log.info("用户更新成功, id: {}", user.getId());
        return user;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysUser existing = getById(id);
        existing.setStatus(0); // 软删除
        updateById(existing);
        // 立即踢下线：否则已登录用户的 token 最长还有 24h 有效期，禁用不生效
        try {
            StpUtil.kickout(id);
        } catch (Exception e) {
            log.warn("踢下线失败（用户可能未登录）, id: {}", id, e);
        }
        log.info("用户禁用成功, id: {}", id);
    }

    @Override
    public SysUser getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    public Page<UserVO> list(Integer pageNum, Integer pageSize, String keyword, Long deptId, Integer status) {
        // 删除即禁用(status=0)，默认列表只查启用账号；需查已删除账号时显式传 status=0
        if (status == null) {
            status = 1;
        }
        return userMapper.selectPageVO(new Page<>(pageNum, pageSize), keyword, deptId, status);
    }

    @Override
    public SysUser login(String username, String password) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // Sa-Token 登录
        StpUtil.login(user.getId());
        String tokenValue = StpUtil.getTokenValue();

        log.info("用户登录成功, id: {}, username: {}", user.getId(), username);
        // 清空密码再返回
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = getById(userId);
        if (oldPassword == null || oldPassword.isEmpty()) {
            throw new BusinessException("请输入旧密码");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new BusinessException("请输入新密码");
        }
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        user.setPwdModified(1);
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        log.info("用户修改密码成功, id: {}", userId);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, String realName, String phone) {
        if (realName == null || realName.trim().isEmpty()) {
            throw new BusinessException("姓名不能为空");
        }
        SysUser user = getById(userId);
        user.setRealName(realName.trim());
        user.setPhone(phone);
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        log.info("用户更新个人信息成功, id: {}", userId);
    }
}
