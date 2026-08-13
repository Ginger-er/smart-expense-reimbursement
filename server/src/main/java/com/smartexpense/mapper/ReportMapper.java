package com.smartexpense.mapper;

import com.smartexpense.vo.DeptRankVO;
import com.smartexpense.vo.ExpenseTypeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ReportMapper {

    @Select("<script>SELECT COALESCE(SUM(r.total_amount), 0) FROM reimbursement r LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.status NOT IN (0,4) " +
            "<if test='userId != null'>AND r.user_id = #{userId}</if>" +
            "<if test='deptId != null'>AND u.dept_id = #{deptId}</if>" +
            "<if test='startDate != null and startDate != \"\"'>AND r.create_time &gt;= #{startDate}</if>" +
            "<if test='endDate != null and endDate != \"\"'>AND r.create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if></script>")
    BigDecimal sumTotalAmount(@Param("userId") Long userId, @Param("deptId") Long deptId,
                              @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT COUNT(*) FROM reimbursement r LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.status NOT IN (0,4) " +
            "<if test='userId != null'>AND r.user_id = #{userId}</if>" +
            "<if test='deptId != null'>AND u.dept_id = #{deptId}</if>" +
            "<if test='startDate != null and startDate != \"\"'>AND r.create_time &gt;= #{startDate}</if>" +
            "<if test='endDate != null and endDate != \"\"'>AND r.create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if></script>")
    int countTotal(@Param("userId") Long userId, @Param("deptId") Long deptId,
                   @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT COUNT(*) FROM reimbursement r LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.status IN (3,5) " +
            "<if test='userId != null'>AND r.user_id = #{userId}</if>" +
            "<if test='deptId != null'>AND u.dept_id = #{deptId}</if>" +
            "<if test='startDate != null and startDate != \"\"'>AND r.create_time &gt;= #{startDate}</if>" +
            "<if test='endDate != null and endDate != \"\"'>AND r.create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if></script>")
    int countApproved(@Param("userId") Long userId, @Param("deptId") Long deptId,
                      @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT COUNT(*) FROM reimbursement r LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.status = 4 " +
            "<if test='userId != null'>AND r.user_id = #{userId}</if>" +
            "<if test='deptId != null'>AND u.dept_id = #{deptId}</if>" +
            "<if test='startDate != null and startDate != \"\"'>AND r.create_time &gt;= #{startDate}</if>" +
            "<if test='endDate != null and endDate != \"\"'>AND r.create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if></script>")
    int countRejected(@Param("userId") Long userId, @Param("deptId") Long deptId,
                      @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT d.dept_name AS name, COALESCE(SUM(r.total_amount), 0) AS amount " +
            "FROM reimbursement r " +
            "JOIN sys_user u ON r.user_id = u.id " +
            "JOIN sys_dept d ON u.dept_id = d.id " +
            "WHERE r.status NOT IN (0,4) " +
            "<if test='userId != null'>AND r.user_id = #{userId}</if>" +
            "<if test='deptId != null'>AND u.dept_id = #{deptId}</if>" +
            "<if test='startDate != null and startDate != \"\"'>AND r.create_time &gt;= #{startDate}</if>" +
            "<if test='endDate != null and endDate != \"\"'>AND r.create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>" +
            "GROUP BY d.id, d.dept_name ORDER BY amount DESC</script>")
    List<DeptRankVO> deptRanking(@Param("userId") Long userId, @Param("deptId") Long deptId,
                                 @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>SELECT CASE i.type WHEN 1 THEN '交通费' WHEN 2 THEN '住宿费' WHEN 3 THEN '餐饮费' ELSE '其他' END AS name, " +
            "COALESCE(SUM(i.amount), 0) AS amount " +
            "FROM invoice i " +
            "JOIN reimbursement r ON i.reimbursement_id = r.id " +
            "LEFT JOIN sys_user u ON i.user_id = u.id " +
            "WHERE r.status NOT IN (0,4) " + // 与 sumTotalAmount 口径一致：排除草稿报销单
            "<if test='userId != null'>AND i.user_id = #{userId}</if>" +
            "<if test='deptId != null'>AND u.dept_id = #{deptId}</if>" +
            "<if test='startDate != null and startDate != \"\"'>AND i.create_time &gt;= #{startDate}</if>" +
            "<if test='endDate != null and endDate != \"\"'>AND i.create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>" +
            "GROUP BY i.type ORDER BY amount DESC</script>")
    List<ExpenseTypeVO> expenseTypes(@Param("userId") Long userId, @Param("deptId") Long deptId,
                                     @Param("startDate") String startDate, @Param("endDate") String endDate);
}
