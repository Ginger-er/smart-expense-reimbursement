package com.smartexpense.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.alibaba.excel.EasyExcel;
import com.smartexpense.common.Result;
import com.smartexpense.service.ReportService;
import com.smartexpense.vo.ReimbursementExportVO;
import com.smartexpense.vo.ReportStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "数据报表", description = "报销统计分析")
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "报表统计")
    @SaCheckRole(value = {"manager", "finance", "admin"}, mode = SaMode.OR)
    @GetMapping("/stats")
    public Result<ReportStatsVO> stats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(reportService.stats(startDate, endDate));
    }

    @Operation(summary = "导出报表（Excel）")
    @SaCheckRole(value = {"manager", "finance", "admin"}, mode = SaMode.OR)
    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) throws IOException {
        List<ReimbursementExportVO> list = reportService.exportList(startDate, endDate);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("报销明细报表", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), ReimbursementExportVO.class)
                .sheet("报销明细")
                .doWrite(list);
    }
}
