package com.smartexpense.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartexpense.entity.ApprovalRecord;
import com.smartexpense.vo.ApprovalRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApprovalRecordMapper extends BaseMapper<ApprovalRecord> {

    @Select("SELECT a.id, a.reimbursement_id, a.trip_id, a.approver_id, u.real_name AS approver_name, " +
            "a.action, a.comment, a.node_name, a.create_time " +
            "FROM approval_record a " +
            "LEFT JOIN sys_user u ON a.approver_id = u.id " +
            "WHERE a.reimbursement_id = #{reimbursementId} " +
            "ORDER BY a.create_time ASC")
    List<ApprovalRecordVO> selectByReimbursementId(@Param("reimbursementId") Long reimbursementId);

    @Select("SELECT a.id, a.reimbursement_id, a.trip_id, a.approver_id, u.real_name AS approver_name, " +
            "a.action, a.comment, a.node_name, a.create_time " +
            "FROM approval_record a " +
            "LEFT JOIN sys_user u ON a.approver_id = u.id " +
            "WHERE a.trip_id = #{tripId} " +
            "ORDER BY a.create_time ASC")
    List<ApprovalRecordVO> selectByTripId(@Param("tripId") Long tripId);
}
