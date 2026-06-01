package com.smartexpense.controller;

import com.alibaba.excel.EasyExcel;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartexpense.annotation.OperLog;
import com.smartexpense.common.PageResult;
import com.smartexpense.common.Result;
import com.smartexpense.entity.Reimbursement;
import com.smartexpense.service.ReimbursementService;
import com.smartexpense.vo.ReimbursementDetailVO;
import com.smartexpense.vo.ReimbursementExportVO;
import com.smartexpense.vo.ReimbursementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Tag(name = "报销管理", description = "报销单创建、提交、审批、查询")
@RestController
@RequestMapping("/reimbursement")
@RequiredArgsConstructor
public class ReimbursementController {

    private final ReimbursementService reimbursementService;

    @Operation(summary = "创建报销单")
    @OperLog("创建报销单")
    @PostMapping("/create")
    public Result<Reimbursement> create(@Valid @RequestBody Reimbursement reimbursement) {
        return Result.success(reimbursementService.create(reimbursement));
    }

    @Operation(summary = "提交报销单")
    @OperLog("提交报销单")
    @PostMapping("/submit/{id}")
    public Result<Reimbursement> submit(@PathVariable Long id) {
        return Result.success("提交成功", reimbursementService.submit(id));
    }

    @Operation(summary = "审批报销单")
    @SaCheckRole(value = {"manager", "finance", "admin"}, mode = SaMode.OR)
    @OperLog("审批报销单")
    @PostMapping("/approve/{id}")
    public Result<Reimbursement> approve(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Integer action = (Integer) body.get("action");
        String comment = (String) body.get("comment");
        return Result.success("审批完成", reimbursementService.approve(id, action, comment));
    }

    @Operation(summary = "打款")
    @SaCheckRole(value = {"finance", "admin"}, mode = SaMode.OR)
    @OperLog("报销打款")
    @PostMapping("/pay/{id}")
    public Result<Reimbursement> pay(@PathVariable Long id) {
        return Result.success("打款完成", reimbursementService.pay(id));
    }

    @Operation(summary = "报销单列表")
    @GetMapping("/list")
    public PageResult<ReimbursementVO> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        Page<ReimbursementVO> page = reimbursementService.list(pageNum, pageSize, status, keyword, startDate, endDate);
        return PageResult.success(page);
    }

    @Operation(summary = "报销单详情")
    @GetMapping("/detail/{id}")
    public Result<ReimbursementDetailVO> detail(@PathVariable Long id) {
        return Result.success(reimbursementService.detail(id));
    }

    @Operation(summary = "删除报销单")
    @OperLog("删除报销单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reimbursementService.delete(id);
        return Result.success();
    }

    @Operation(summary = "导出报销单（Excel）")
    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) throws IOException {
        List<ReimbursementExportVO> list = reimbursementService.exportList(status, keyword, startDate, endDate);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("报销单导出", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), ReimbursementExportVO.class)
                .sheet("报销单")
                .doWrite(list);
    }
}
