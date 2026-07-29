package com.smartexpense.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartexpense.annotation.OperLog;
import com.smartexpense.common.PageResult;
import com.smartexpense.common.Result;
import com.smartexpense.entity.AbnormalRecord;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.AbnormalRecordMapper;
import com.smartexpense.service.abnormal.AbnormalScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用异常预警：财务/管理员查看预警列表、标记处理；管理员可手动触发扫描。
 */
@Tag(name = "异常预警", description = "费用异常预警扫描与处理")
@RestController
@RequestMapping("/abnormal")
@RequiredArgsConstructor
public class AbnormalController {

    private final AbnormalRecordMapper recordMapper;
    private final AbnormalScanService scanService;

    @Operation(summary = "预警列表（分页，可按处理状态筛选）")
    @SaCheckRole(value = {"finance", "admin"}, mode = SaMode.OR)
    @GetMapping("/list")
    public PageResult<AbnormalRecord> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer handled) {
        LambdaQueryWrapper<AbnormalRecord> wrapper = new LambdaQueryWrapper<>();
        if (handled != null) {
            wrapper.eq(AbnormalRecord::getHandled, handled);
        }
        wrapper.orderByDesc(AbnormalRecord::getCreateTime);
        Page<AbnormalRecord> page = recordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.success(page);
    }

    @Operation(summary = "标记预警已处理")
    @OperLog("处理预警")
    @SaCheckRole(value = {"finance", "admin"}, mode = SaMode.OR)
    @PostMapping("/handle/{id}")
    public Result<Void> handle(@PathVariable Long id) {
        AbnormalRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("预警记录不存在");
        }
        record.setHandled(1);
        record.setHandleTime(LocalDateTime.now());
        recordMapper.updateById(record);
        return Result.success();
    }

    @Operation(summary = "手动触发扫描昨日数据（管理员，返回新增条数）")
    @OperLog("手动扫描预警")
    @SaCheckRole("admin")
    @PostMapping("/scan")
    public Result<Integer> scan() {
        int inserted = scanService.scan(LocalDate.now().minusDays(1));
        return Result.success("扫描完成，新增 " + inserted + " 条预警", inserted);
    }
}
