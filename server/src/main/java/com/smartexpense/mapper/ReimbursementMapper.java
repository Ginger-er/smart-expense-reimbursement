package com.smartexpense.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartexpense.entity.Reimbursement;
import com.smartexpense.vo.ReimbursementDetailVO;
import com.smartexpense.vo.ReimbursementVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface ReimbursementMapper extends BaseMapper<Reimbursement> {

    @Select("<script>" +
            "SELECT r.id, r.user_id AS user_id, r.reimburse_no AS order_no, u.real_name AS applicant_name, " +
            "d.dept_name AS dept_name, " +
            "r.total_amount AS amount, r.invoice_count, r.status, r.remark, r.create_time " +
            "FROM reimbursement r " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "LEFT JOIN sys_dept d ON u.dept_id = d.id " +
            "<where>" +
            "  <if test='status != null'>AND r.status = #{status}</if>" +
            "  AND ( (r.status = 0 AND r.user_id = #{currentUserId}) " +
            "        OR (r.status != 0 " +
            "            <if test='role == 1'>AND r.user_id = #{currentUserId}</if>" +
            "            <if test='role == 2'>AND u.dept_id = #{deptId}</if>)) " +
            "  <if test='keyword != null and keyword != \"\"'>AND (r.reimburse_no LIKE CONCAT('%', #{keyword}, '%') OR u.real_name LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "  <if test='startDate != null and startDate != \"\"'>AND r.create_time &gt;= #{startDate}</if>" +
            "  <if test='endDate != null and endDate != \"\"'>AND r.create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>" +
            "</where> " +
            "ORDER BY r.create_time DESC" +
            "</script>")
    Page<ReimbursementVO> selectPageVO(Page<ReimbursementVO> page, @Param("status") Integer status,
                                       @Param("role") Integer role,
                                       @Param("currentUserId") Long currentUserId,
                                       @Param("deptId") Long deptId,
                                       @Param("keyword") String keyword,
                                       @Param("startDate") String startDate,
                                       @Param("endDate") String endDate);

    @Select("SELECT r.id, r.user_id, r.reimburse_no, r.total_amount, r.invoice_count, r.status, r.reject_reason, r.remark, r.create_time, r.pay_time, r.pay_user_name, " +
            "u.real_name AS applicant_name, d.dept_name AS dept_name, u.dept_id AS dept_id " +
            "FROM reimbursement r " +
            "LEFT JOIN sys_user u ON r.user_id = u.id " +
            "LEFT JOIN sys_dept d ON u.dept_id = d.id " +
            "WHERE r.id = #{id}")
    ReimbursementDetailVO selectDetailById(@Param("id") Long id);

    /** 打款：仅当状态仍为「已通过」时更新，返回受影响行数（乐观锁，防止并发重复打款） */
    @Update("UPDATE reimbursement SET status = 5, pay_time = #{payTime}, pay_user_id = #{payUserId}, " +
            "pay_user_name = #{payUserName}, update_time = #{updateTime} " +
            "WHERE id = #{id} AND status = 3")
    int payIfApproved(@Param("id") Long id,
                      @Param("payTime") LocalDateTime payTime,
                      @Param("payUserId") Long payUserId,
                      @Param("payUserName") String payUserName,
                      @Param("updateTime") LocalDateTime updateTime);
}
