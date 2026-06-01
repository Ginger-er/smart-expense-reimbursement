package com.smartexpense.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartexpense.entity.SysUser;
import com.smartexpense.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("<script>" +
            "SELECT u.id, u.username, u.real_name, u.dept_id, d.dept_name, " +
            "u.role, u.phone, u.status, u.pwd_modified, u.create_time " +
            "FROM sys_user u " +
            "LEFT JOIN sys_dept d ON u.dept_id = d.id " +
            "<where>" +
            "  <if test=\"keyword != null and keyword != ''\">" +
            "    AND (u.username LIKE CONCAT('%', #{keyword}, '%') OR u.real_name LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if>" +
            "  <if test='deptId != null'>AND u.dept_id = #{deptId}</if>" +
            "  <if test='status != null'>AND u.status = #{status}</if>" +
            "</where> " +
            "ORDER BY u.create_time DESC" +
            "</script>")
    Page<UserVO> selectPageVO(Page<UserVO> page, @Param("keyword") String keyword,
                              @Param("deptId") Long deptId, @Param("status") Integer status);
}
