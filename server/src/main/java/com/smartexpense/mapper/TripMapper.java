package com.smartexpense.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartexpense.entity.Trip;
import com.smartexpense.vo.TripDetailVO;
import com.smartexpense.vo.TripVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TripMapper extends BaseMapper<Trip> {

    @Select("<script>" +
            "SELECT t.id, t.user_id AS user_id, t.trip_no AS trip_no, u.real_name AS applicant_name, d.dept_name AS dept_name, " +
            "t.destination, t.purpose AS reason, t.start_date, t.end_date, " +
            "t.budget_amount AS budget, t.status, t.create_time " +
            "FROM trip t " +
            "LEFT JOIN sys_user u ON t.user_id = u.id " +
            "LEFT JOIN sys_dept d ON u.dept_id = d.id " +
            "<where>" +
            "  <if test='status != null'>AND t.status = #{status}</if>" +
            "  AND ( (t.status = 0 AND t.user_id = #{currentUserId}) " +
            "        OR (t.status != 0 " +
            "            <if test='role == 1'>AND t.user_id = #{currentUserId}</if>" +
            "            <if test='role == 2'>AND u.dept_id = #{deptId}</if>)) " +
            "  <if test='keyword != null and keyword != \"\"'>AND (t.trip_no LIKE CONCAT('%', #{keyword}, '%') OR t.destination LIKE CONCAT('%', #{keyword}, '%') OR u.real_name LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "  <if test='startDate != null and startDate != \"\"'>AND t.create_time &gt;= #{startDate}</if>" +
            "  <if test='endDate != null and endDate != \"\"'>AND t.create_time &lt; DATE_ADD(#{endDate}, INTERVAL 1 DAY)</if>" +
            "</where> " +
            "ORDER BY t.create_time DESC" +
            "</script>")
    Page<TripVO> selectPageVO(Page<TripVO> page, @Param("status") Integer status,
                              @Param("role") Integer role,
                              @Param("currentUserId") Long currentUserId,
                              @Param("deptId") Long deptId,
                              @Param("keyword") String keyword,
                              @Param("startDate") String startDate,
                              @Param("endDate") String endDate);

    @Select("SELECT t.id, t.user_id, t.trip_no, t.destination, t.purpose, t.start_date, t.end_date, t.budget_amount, t.status, t.create_time, t.update_time, " +
            "u.real_name AS applicant_name, d.dept_name AS dept_name, u.dept_id AS dept_id " +
            "FROM trip t " +
            "LEFT JOIN sys_user u ON t.user_id = u.id " +
            "LEFT JOIN sys_dept d ON u.dept_id = d.id " +
            "WHERE t.id = #{id}")
    TripDetailVO selectDetailById(@Param("id") Long id);
}
