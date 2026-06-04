package com.smartexpense.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartexpense.entity.ApprovalRecord;
import com.smartexpense.entity.SysUser;
import com.smartexpense.entity.Trip;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.ApprovalRecordMapper;
import com.smartexpense.mapper.SysUserMapper;
import com.smartexpense.mapper.TripMapper;
import com.smartexpense.service.NoticeService;
import com.smartexpense.service.TripService;
import com.smartexpense.vo.TripDetailVO;
import com.smartexpense.vo.TripVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripServiceImpl extends ServiceImpl<TripMapper, Trip> implements TripService {

    private final TripMapper tripMapper;
    private final SysUserMapper userMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final NoticeService noticeService;

    @Override
    @Transactional
    public Trip create(Trip trip) {
        String tripNo = "CC" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + RandomUtil.randomNumbers(6);
        trip.setTripNo(tripNo);
        trip.setUserId(StpUtil.getLoginIdAsLong()); // 归属当前登录用户，不信任前端传值
        trip.setStatus(0); // 草稿
        trip.setCreateTime(LocalDateTime.now());
        trip.setUpdateTime(LocalDateTime.now());
        save(trip);

        log.info("出差申请创建成功, id: {}, tripNo: {}", trip.getId(), tripNo);
        return trip;
    }

    @Override
    @Transactional
    public Trip update(Trip trip) {
        Trip existing = getById(trip.getId());
        if (existing == null) {
            throw new BusinessException("出差申请不存在");
        }
        // 草稿可修改；已驳回的修改后回到草稿状态，可重新提交
        if (existing.getStatus() != 0 && existing.getStatus() != 4) {
            throw new BusinessException("只有草稿或已驳回状态才能修改");
        }
        SysUser current = currentUser();
        if (current.getRole() != 4 && !existing.getUserId().equals(current.getId())) {
            throw new BusinessException("只能操作自己的单据");
        }
        // 字段白名单：仅允许更新可编辑业务字段，status/userId/tripNo 等一律忽略，防止越权改状态跳过审批
        existing.setDestination(trip.getDestination());
        existing.setPurpose(trip.getPurpose());
        existing.setStartDate(trip.getStartDate());
        existing.setEndDate(trip.getEndDate());
        existing.setBudgetAmount(trip.getBudgetAmount());
        existing.setStatus(0); // 修改后重置为草稿
        existing.setUpdateTime(LocalDateTime.now());
        updateById(existing);
        log.info("出差申请更新成功, id: {}", existing.getId());
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Trip existing = getById(id);
        if (existing.getStatus() != 0) {
            throw new BusinessException("只有草稿状态才能删除");
        }
        SysUser current = currentUser();
        if (current.getRole() != 4 && !existing.getUserId().equals(current.getId())) {
            throw new BusinessException("只能操作自己的单据");
        }
        removeById(id);
        log.info("出差申请删除成功, id: {}", id);
    }

    @Override
    public Trip getById(Long id) {
        Trip trip = tripMapper.selectById(id);
        if (trip == null) {
            throw new BusinessException("出差申请不存在");
        }
        return trip;
    }

    @Override
    public Page<TripVO> list(Integer pageNum, Integer pageSize, Integer status,
                             String keyword, String startDate, String endDate) {
        SysUser current = currentUser();
        Integer role = current.getRole();
        Long currentUserId = current.getId();
        Long deptId = role == 2 ? current.getDeptId() : null;
        return tripMapper.selectPageVO(new Page<>(pageNum, pageSize), status, role, currentUserId, deptId, keyword, startDate, endDate);
    }

    private SysUser currentUser() {
        return userMapper.selectById(StpUtil.getLoginIdAsLong());
    }

    /** 详情查看权限：草稿私有，员工看自己，领导看本部门，财务/管理员看全部 */
    private void checkViewPermission(SysUser current, Long ownerId, Long ownerDeptId, Integer status) {
        if (status != null && status == 0) {
            if (!ownerId.equals(current.getId())) {
                throw new BusinessException("只能查看自己的单据");
            }
            return;
        }
        int role = current.getRole();
        if (role == 1 && !ownerId.equals(current.getId())) {
            throw new BusinessException("只能查看自己的单据");
        }
        if (role == 2 && (ownerDeptId == null || !ownerDeptId.equals(current.getDeptId()))) {
            throw new BusinessException("只能查看本部门的单据");
        }
    }

    @Override
    @Transactional
    public Trip submit(Long id) {
        Trip trip = getById(id);
        if (trip.getStatus() != 0 && trip.getStatus() != 4) {
            throw new BusinessException("只有草稿或已驳回状态的出差单才能提交");
        }
        SysUser current = currentUser();
        if (current.getRole() != 4 && !trip.getUserId().equals(current.getId())) {
            throw new BusinessException("只能操作自己的单据");
        }
        trip.setStatus(1); // 已提交
        trip.setUpdateTime(LocalDateTime.now());
        updateById(trip);
        log.info("出差申请提交成功, id: {}", id);
        return trip;
    }

    @Override
    @Transactional
    public Trip approve(Long id, Integer action, String comment) {
        if (action == null || (action != 1 && action != 2)) {
            throw new BusinessException("无效的审批操作");
        }
        Trip trip = getById(id);
        if (trip.getStatus() != 1) {
            throw new BusinessException("当前状态不可审批");
        }

        // 领导只能审本部门的出差单
        SysUser current = currentUser();
        // 防止自审自批：任何角色都不能审批自己提交的单据
        if (current.getId().equals(trip.getUserId())) {
            throw new BusinessException("不能审批自己提交的出差单");
        }
        if (current.getRole() == 2) {
            SysUser applicant = userMapper.selectById(trip.getUserId());
            if (applicant == null || !current.getDeptId().equals(applicant.getDeptId())) {
                throw new BusinessException("只能审批本部门的出差单");
            }
        }

        ApprovalRecord record = new ApprovalRecord();
        record.setTripId(id);
        record.setApproverId(current.getId());
        record.setAction(action);
        record.setComment(comment);
        record.setNodeName("审批");
        record.setCreateTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);

        if (action == 1) {
            trip.setStatus(3); // 已通过
        } else {
            trip.setStatus(4); // 已驳回
        }
        trip.setUpdateTime(LocalDateTime.now());
        updateById(trip);

        String reason = action == 2 && comment != null && !comment.isEmpty() ? "：" + comment : "";
        noticeService.send(trip.getUserId(), action == 1 ? "出差审批通过" : "出差被驳回",
                "出差申请 " + trip.getTripNo() + (action == 1 ? " 已审批通过" : " 已被驳回") + reason,
                "/trip/detail/" + id);

        log.info("出差申请审批成功, id: {}, action: {}", id, action);
        return trip;
    }

    @Override
    public TripDetailVO getDetail(Long id) {
        TripDetailVO vo = tripMapper.selectDetailById(id);
        if (vo == null) {
            throw new BusinessException("出差申请不存在");
        }
        SysUser current = currentUser();
        checkViewPermission(current, vo.getUserId(), vo.getDeptId(), vo.getStatus());
        vo.setApprovalRecords(approvalRecordMapper.selectByTripId(id));
        return vo;
    }
}
