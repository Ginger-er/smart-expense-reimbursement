package com.smartexpense.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartexpense.annotation.OperLog;
import com.smartexpense.common.LoginVO;
import com.smartexpense.common.PageResult;
import com.smartexpense.common.Result;
import com.smartexpense.entity.SysUser;
import com.smartexpense.service.UserService;
import com.smartexpense.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户管理", description = "用户登录、增删改查")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户登录")
    @OperLog("用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        SysUser user = userService.login(username, password);
        String token = StpUtil.getTokenValue();
        return Result.success("登录成功", new LoginVO(token, user));
    }

    @Operation(summary = "用户自助注册（默认员工角色）")
    @OperLog("用户注册")
    @PostMapping("/register")
    public Result<SysUser> register(@RequestBody SysUser user) {
        return Result.success("注册成功", userService.register(user));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<SysUser> info() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(userService.getById(userId));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }

    @Operation(summary = "修改当前用户密码")
    @OperLog("修改密码")
    @PostMapping("/password")
    public Result<Void> updatePassword(@RequestBody Map<String, String> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.updatePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return Result.success();
    }

    @Operation(summary = "更新当前用户个人信息")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody SysUser user) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.updateProfile(userId, user.getRealName(), user.getPhone());
        return Result.success();
    }

    @Operation(summary = "创建用户")
    @SaCheckRole("admin")
    @OperLog("创建用户")
    @PostMapping("/create")
    public Result<SysUser> create(@RequestBody SysUser user) {
        return Result.success(userService.create(user));
    }

    @Operation(summary = "更新用户")
    @SaCheckRole("admin")
    @OperLog("更新用户")
    @PutMapping("/update")
    public Result<SysUser> update(@RequestBody SysUser user) {
        return Result.success(userService.update(user));
    }

    @Operation(summary = "删除/禁用用户")
    @SaCheckRole("admin")
    @OperLog("删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @Operation(summary = "用户详情")
    @SaCheckRole("admin")
    @GetMapping("/{id}")
    public Result<SysUser> detail(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @Operation(summary = "用户列表")
    @SaCheckRole("admin")
    @GetMapping("/list")
    public PageResult<UserVO> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long deptId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        Page<UserVO> page = userService.list(pageNum, pageSize, keyword, deptId, status);
        return PageResult.success(page);
    }
}
