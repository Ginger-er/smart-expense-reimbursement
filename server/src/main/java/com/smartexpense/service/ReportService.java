package com.smartexpense.service;

import com.smartexpense.vo.ReimbursementExportVO;
import com.smartexpense.vo.ReportStatsVO;

import java.util.List;

public interface ReportService {

    /** 报表统计 */
    ReportStatsVO stats(String startDate, String endDate);

    /** 导出报表明细（Excel，排除草稿） */
    List<ReimbursementExportVO> exportList(String startDate, String endDate);
}
