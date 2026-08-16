package com.smartexpense.mapper;

import com.smartexpense.vo.ReimbursementVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface DashboardMapper {

    @Select("<script>SELECT COUNT(*) FROM reimbursement r LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.status IN (1,2) " +
            "<if test='userId != null'>AND r.user_id = #{userId}</if>" +
            "<if test='deptId != null'>AND u.dept_id = #{deptId}</if></script>")
    int countPendingReimbursement(@Param("userId") Long userId, @Param("deptId") Long deptId);

    @Select("<script>SELECT COUNT(*) FROM trip t LEFT JOIN sys_user u ON t.user_id = u.id " +
            "WHERE t.status IN (1,2) " +
            "<if test='userId != null'>AND t.user_id = #{userId}</if>" +
            "<if test='deptId != null'>AND u.dept_id = #{deptId}</if></script>")
    int countPendingTrip(@Param("userId") Long userId, @Param("deptId") Long deptId);

    @Select("<script>SELECT COALESCE(SUM(r.total_amount), 0) FROM reimbursement r LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.status NOT IN (0,4) AND r.create_time >= #{monthStart} AND r.create_time &lt; #{monthEnd} " +
            "<if test='userId != null'>AND r.user_id = #{userId}</if>" +
            "<if test='deptId != null'>AND u.dept_id = #{deptId}</if></script>")
    BigDecimal sumMonthTotal(@Param("userId") Long userId, @Param("deptId") Long deptId,
                             @Param("monthStart") String monthStart, @Param("monthEnd") String monthEnd);

    @Select("<script>SELECT COUNT(*) FROM reimbursement r LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.status != 0 " +
            "<if test='userId != null'>AND r.user_id = #{userId}</if>" +
            "<if test='deptId != null'>AND u.dept_id = #{deptId}</if></script>")
    int countReimbursement(@Param("userId") Long userId, @Param("deptId") Long deptId);

    @Select("<script>SELECT COUNT(*) FROM invoice i LEFT JOIN sys_user u ON i.user_id = u.id " +
            "WHERE 1=1 " +
            "<if test='userId != null'>AND i.user_id = #{userId}</if>" +
            "<if test='deptId != null'>AND u.dept_id = #{deptId}</if></script>")
    int countInvoice(@Param("userId") Long userId, @Param("deptId") Long deptId);

    @Select("SELECT COUNT(*) FROM reimbursement WHERE user_id = #{userId} AND status = 0")
    int countMyDraft(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM reimbursement WHERE user_id = #{userId} AND status = 4")
    int countMyRejected(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM sys_user WHERE status = 1")
    int countUser();

    @Select("SELECT COUNT(*) FROM sys_dept WHERE status = 1")
    int countDept();

    @Select("<script>SELECT r.id, r.user_id AS user_id, r.reimburse_no AS order_no, u.real_name AS applicant_name, " +
            "d.dept_name AS dept_name, " +
            "r.total_amount AS amount, r.invoice_count, r.status, r.remark, r.create_time " +
            "FROM reimbursement r " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "LEFT JOIN sys_dept d ON u.dept_id = d.id " +
            "WHERE 1=1 " +
            "<if test='userId != null'>AND r.user_id = #{userId}</if>" +
            "<if test='deptId != null'>AND u.dept_id = #{deptId}</if> " +
            "ORDER BY r.create_time DESC LIMIT 5</script>")
    List<ReimbursementVO> recentReimbursements(@Param("userId") Long userId, @Param("deptId") Long deptId);
}
