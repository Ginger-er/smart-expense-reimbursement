package com.smartexpense.controller;

import com.smartexpense.common.Result;
import com.smartexpense.service.DashboardService;
import com.smartexpense.vo.DashboardStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "工作台", description = "工作台统计数据")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "工作台统计数据")
    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        return Result.success(dashboardService.stats());
    }
}
