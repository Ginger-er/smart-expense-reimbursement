package com.smartexpense.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 异常预警规则专用查询（只读聚合查询，结果列名用驼峰别名）。
 */
@Mapper
public interface AbnormalQueryMapper {

    /** A001 重复发票：同一发票号出现在多个不同报销单中 */
    @Select("SELECT i.invoice_no AS invoiceNo, i.user_id AS userId, " +
            "GROUP_CONCAT(DISTINCT i.reimbursement_id ORDER BY i.reimbursement_id) AS reimbursementIds " +
            "FROM invoice i " +
            "WHERE i.invoice_no IS NOT NULL AND i.invoice_no != '' AND i.reimbursement_id IS NOT NULL " +
            "AND i.create_time >= #{start} AND i.create_time < #{end} " +
            "GROUP BY i.invoice_no, i.user_id " +
            "HAVING COUNT(DISTINCT i.reimbursement_id) > 1")
    List<Map<String, Object>> duplicateInvoiceNos(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    /** A002 日期异常：发票开票日期不在关联出差申请的行程范围内 */
    @Select("SELECT i.id AS invoiceId, i.user_id AS userId, i.invoice_no AS invoiceNo, " +
            "i.invoice_date AS invoiceDate, t.trip_no AS tripNo, t.start_date AS startDate, t.end_date AS endDate " +
            "FROM invoice i JOIN trip t ON i.trip_id = t.id " +
            "WHERE i.invoice_date IS NOT NULL " +
            "AND i.create_time >= #{start} AND i.create_time < #{end} " +
            "AND (i.invoice_date < t.start_date OR i.invoice_date > t.end_date)")
    List<Map<String, Object>> invoiceDateOutOfTripRange(@Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);

    /** A003 金额突增：本月已通过/已打款的报销总额超过上月 150% */
    @Select("SELECT cur.user_id AS userId, cur.month_total AS curTotal, prev.month_total AS prevTotal " +
            "FROM ( " +
            "  SELECT user_id, SUM(total_amount) AS month_total FROM reimbursement " +
            "  WHERE status IN (3,5) AND create_time >= #{monthStart} GROUP BY user_id " +
            ") cur JOIN ( " +
            "  SELECT user_id, SUM(total_amount) AS month_total FROM reimbursement " +
            "  WHERE status IN (3,5) AND create_time >= #{prevMonthStart} AND create_time < #{monthStart} GROUP BY user_id " +
            ") prev ON cur.user_id = prev.user_id " +
            "WHERE prev.month_total > 0 AND cur.month_total > prev.month_total * 1.5")
    List<Map<String, Object>> amountSurgeUsers(@Param("monthStart") LocalDateTime monthStart,
                                               @Param("prevMonthStart") LocalDateTime prevMonthStart);
}
