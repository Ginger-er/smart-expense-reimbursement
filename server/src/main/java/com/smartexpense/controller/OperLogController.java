package com.smartexpense.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartexpense.common.PageResult;
import com.smartexpense.entity.SysOperLog;
import com.smartexpense.mapper.SysOperLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "操作日志", description = "操作日志查询")
@RestController
@RequestMapping("/operlog")
@RequiredArgsConstructor
public class OperLogController {

    private final SysOperLogMapper operLogMapper;

    @Operation(summary = "操作日志列表")
    @SaCheckRole("admin")
    @GetMapping("/list")
    public PageResult<SysOperLog> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<SysOperLog> page = operLogMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysOperLog>().orderByDesc(SysOperLog::getCreateTime));
        return PageResult.success(page);
    }
}
