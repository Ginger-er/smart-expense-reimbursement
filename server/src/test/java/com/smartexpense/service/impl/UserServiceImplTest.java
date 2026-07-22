package com.smartexpense.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.smartexpense.entity.SysUser;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private SysUserMapper userMapper;

    @InjectMocks
    private UserServiceImpl service;

    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
    }

    private SysUser user(long id, int role, int status) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername("u" + id);
        u.setPassword(BCrypt.hashpw("123456", BCrypt.gensalt()));
        u.setRole(role);
        u.setStatus(status);
        u.setRealName("用户" + id);
        return u;
    }

    @Test
    void login_success_shouldReturnUserWithoutPassword() {
        when(userMapper.selectOne(any())).thenReturn(user(2L, 1, 1));
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("token-xxx");

            SysUser result = service.login("u2", "123456");

            assertEquals(2L, result.getId());
            assertNull(result.getPassword()); // 密码不回传
            stp.verify(() -> StpUtil.login(2L));
        }
    }

    @Test
    void login_wrongPassword_shouldThrow() {
        when(userMapper.selectOne(any())).thenReturn(user(2L, 1, 1));

        assertThrows(BusinessException.class, () -> service.login("u2", "wrong"));
    }

    @Test
    void login_disabledUser_shouldThrow() {
        when(userMapper.selectOne(any())).thenReturn(user(2L, 1, 0));

        assertThrows(BusinessException.class, () -> service.login("u2", "123456"));
    }

    @Test
    void register_duplicateUsername_shouldThrow() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        SysUser u = new SysUser();
        u.setUsername("dup");
        u.setPassword("123456");
        u.setRealName("重复用户");
        assertThrows(BusinessException.class, () -> service.register(u));
    }

    @Test
    void register_success_shouldSetEmployeeRoleAndClearPassword() {
        when(userMapper.selectCount(any())).thenReturn(0L);

        SysUser u = new SysUser();
        u.setUsername("newbie");
        u.setPassword("123456");
        u.setRealName("新员工");
        SysUser result = service.register(u);

        assertEquals(1, result.getRole()); // 自助注册固定员工
        assertEquals(1, result.getStatus());
        assertNull(result.getPassword()); // 密码哈希不返回前端
    }

    @Test
    void delete_shouldDisableAndKickout() {
        SysUser u = user(1L, 1, 1);
        when(userMapper.selectById(1L)).thenReturn(u);
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            service.delete(1L);

            stp.verify(() -> StpUtil.kickout(1L)); // 立即踢下线
        }
        assertEquals(0, u.getStatus()); // 软删除（禁用）
        verify(userMapper).updateById(any(SysUser.class));
    }

    @Test
    void delete_kickoutException_shouldNotFail() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, 1, 1));
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.kickout(1L)).thenThrow(new RuntimeException("redis down"));

            service.delete(1L); // 踢下线失败不应影响禁用主流程
        }
    }

    @Test
    void updatePassword_wrongOldPassword_shouldThrow() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, 1, 1));

        assertThrows(BusinessException.class, () -> service.updatePassword(1L, "wrong-old", "newpass1"));
        verify(userMapper, never()).updateById(any());
    }

    @Test
    void updatePassword_correctOldPassword_shouldUpdate() {
        SysUser u = user(1L, 1, 1);
        when(userMapper.selectById(1L)).thenReturn(u);

        service.updatePassword(1L, "123456", "newpass1");

        verify(userMapper).updateById(any(SysUser.class));
        assertEquals(1, u.getPwdModified());
        org.junit.jupiter.api.Assertions.assertTrue(BCrypt.checkpw("newpass1", u.getPassword()));
    }
}
