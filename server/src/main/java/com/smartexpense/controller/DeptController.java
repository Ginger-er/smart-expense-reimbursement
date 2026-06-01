package com.smartexpense.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.smartexpense.annotation.OperLog;
import com.smartexpense.common.Result;
import com.smartexpense.entity.SysDept;
import com.smartexpense.service.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "部门管理", description = "部门增删改查")
@RestController
@RequestMapping("/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "部门列表")
    @GetMapping("/list")
    public Result<List<SysDept>> list() {
        return Result.success(deptService.list());
    }

    @Operation(summary = "部门树")
    @GetMapping("/tree")
    public Result<List<SysDept>> tree() {
        return Result.success(deptService.tree());
    }

    @Operation(summary = "创建部门")
    @SaCheckRole("admin")
    @OperLog("创建部门")
    @PostMapping("/create")
    public Result<SysDept> create(@RequestBody SysDept dept) {
        return Result.success(deptService.create(dept));
    }

    @Operation(summary = "更新部门")
    @SaCheckRole("admin")
    @OperLog("更新部门")
    @PutMapping("/update")
    public Result<SysDept> update(@RequestBody SysDept dept) {
        return Result.success(deptService.update(dept));
    }

    @Operation(summary = "删除部门")
    @SaCheckRole("admin")
    @OperLog("删除部门")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.success();
    }
}
