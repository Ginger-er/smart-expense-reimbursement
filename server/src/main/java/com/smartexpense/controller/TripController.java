package com.smartexpense.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartexpense.annotation.OperLog;
import com.smartexpense.common.PageResult;
import com.smartexpense.common.Result;
import com.smartexpense.entity.Trip;
import com.smartexpense.service.TripService;
import com.smartexpense.vo.TripDetailVO;
import com.smartexpense.vo.TripVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "出差申请管理", description = "出差申请的增删改查")
@RestController
@RequestMapping("/trip")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @Operation(summary = "创建出差申请")
    @OperLog("创建出差申请")
    @PostMapping("/create")
    public Result<Trip> create(@Valid @RequestBody Trip trip) {
        return Result.success(tripService.create(trip));
    }

    @Operation(summary = "更新出差申请")
    @OperLog("更新出差申请")
    @PutMapping("/update")
    public Result<Trip> update(@Valid @RequestBody Trip trip) {
        return Result.success(tripService.update(trip));
    }

    @Operation(summary = "删除出差申请")
    @OperLog("删除出差申请")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tripService.delete(id);
        return Result.success();
    }

    @Operation(summary = "出差申请详情（含审批记录 + 申请人）")
    @GetMapping("/detail/{id}")
    public Result<TripDetailVO> getDetail(@PathVariable Long id) {
        return Result.success(tripService.getDetail(id));
    }

    @Operation(summary = "出差申请列表")
    @GetMapping("/list")
    public PageResult<TripVO> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        Page<TripVO> page = tripService.list(pageNum, pageSize, status, keyword, startDate, endDate);
        return PageResult.success(page);
    }

    @Operation(summary = "提交出差申请")
    @OperLog("提交出差申请")
    @PostMapping("/submit/{id}")
    public Result<Trip> submit(@PathVariable Long id) {
        return Result.success("提交成功", tripService.submit(id));
    }

    @Operation(summary = "审批出差申请")
    @SaCheckRole(value = {"manager", "finance", "admin"}, mode = SaMode.OR)
    @OperLog("审批出差申请")
    @PostMapping("/approve/{id}")
    public Result<Trip> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer action = (Integer) body.get("action");
        String comment = (String) body.get("comment");
        return Result.success("审批完成", tripService.approve(id, action, comment));
    }
}
