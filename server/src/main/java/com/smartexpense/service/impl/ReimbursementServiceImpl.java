package com.smartexpense.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartexpense.entity.ApprovalRecord;
import com.smartexpense.entity.Invoice;
import com.smartexpense.entity.Reimbursement;
import com.smartexpense.entity.SysUser;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.ApprovalRecordMapper;
import com.smartexpense.mapper.InvoiceMapper;
import com.smartexpense.mapper.ReimbursementMapper;
import com.smartexpense.mapper.SysUserMapper;
import com.smartexpense.redis.RedisLock;
import com.smartexpense.service.NoticeService;
import com.smartexpense.service.ReimbursementService;
import com.smartexpense.vo.ReimbursementDetailVO;
import com.smartexpense.vo.ReimbursementExportVO;
import com.smartexpense.vo.ReimbursementVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReimbursementServiceImpl extends ServiceImpl<ReimbursementMapper, Reimbursement> implements ReimbursementService {

    /** 大额报销阈值：达到该金额需两级审批（领导→财务） */
    private static final BigDecimal APPROVE_THRESHOLD = new BigDecimal("5000");

    /** 提交幂等标记 key 前缀：双击/并发重复提交时互斥 */
    private static final String IDEM_SUBMIT_PREFIX = "idem:submit:";
    /** 审批锁 key 前缀：审批是"校验+写记录+改状态"复合操作，串行化防并发双写 */
    private static final String LOCK_APPROVE_PREFIX = "lock:reimb:approve:";
    /** 打款锁 key 前缀：与乐观更新构成双层防护，防并发重复打款 */
    private static final String LOCK_PAY_PREFIX = "lock:reimb:pay:";

    private final ReimbursementMapper reimbursementMapper;
    private final SysUserMapper userMapper;
    private final InvoiceMapper invoiceMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final NoticeService noticeService;
    private final RedisLock redisLock;

    @Override
    @Transactional
    public Reimbursement create(Reimbursement reimbursement) {
        String reimburseNo = "BX" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + RandomUtil.randomNumbers(6);
        reimbursement.setReimburseNo(reimburseNo);
        reimbursement.setUserId(StpUtil.getLoginIdAsLong()); // 归属当前登录用户，不信任前端传值
        reimbursement.setStatus(0); // 草稿
        reimbursement.setTotalAmount(BigDecimal.ZERO);
        reimbursement.setInvoiceCount(0);
        reimbursement.setCreateTime(LocalDateTime.now());
        reimbursement.setUpdateTime(LocalDateTime.now());
        save(reimbursement);

        log.info("报销单创建成功, id: {}, reimburseNo: {}", reimbursement.getId(), reimburseNo);
        return reimbursement;
    }

    @Override
    @Transactional
    public Reimbursement submit(Long id) {
        // 幂等互斥：SETNX 抢占标记，双击/并发提交只会成功一次；
        // Redis 不可用时降级放行，由下方状态机校验（status 0/4 才可提交）兜底
        String token = UUID.randomUUID().toString();
        if (!redisLock.tryLock(IDEM_SUBMIT_PREFIX + id, token, 10)) {
            throw new BusinessException("提交处理中，请勿重复提交");
        }
        try {
            Reimbursement reimbursement = getById(id);
            if (reimbursement == null) {
                throw new BusinessException("报销单不存在");
            }
            if (reimbursement.getStatus() != 0 && reimbursement.getStatus() != 4) {
                throw new BusinessException("只有草稿或已驳回状态的报销单才能提交");
            }
            SysUser current = currentUser();
            if (current.getRole() != 4 && !reimbursement.getUserId().equals(current.getId())) {
                throw new BusinessException("只能操作自己的单据");
            }

            // 统计关联的发票
            List<Invoice> invoices = invoiceMapper.selectList(
                    new LambdaQueryWrapper<Invoice>().eq(Invoice::getReimbursementId, id));
            if (invoices.isEmpty()) {
                throw new BusinessException("请先关联发票");
            }
            boolean hasNullAmount = invoices.stream().anyMatch(i -> i.getAmount() == null);
            if (hasNullAmount) {
                throw new BusinessException("存在未填写金额的发票，请先补全金额");
            }
            BigDecimal totalAmount = invoices.stream()
                    .map(Invoice::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            reimbursement.setTotalAmount(totalAmount);
            reimbursement.setInvoiceCount(invoices.size());
            reimbursement.setStatus(1); // 待审批
            reimbursement.setRejectReason(null); // 重新提交时清除上次驳回原因

            reimbursement.setUpdateTime(LocalDateTime.now());
            updateById(reimbursement);

            log.info("报销单提交成功, id: {}, totalAmount: {}", id, totalAmount);
            return reimbursement;
        } finally {
            // 无论成功失败立即释放，失败后用户可立即重试，不用等 10s 过期
            redisLock.unlock(IDEM_SUBMIT_PREFIX + id, token);
        }
    }

    @Override
    @Transactional
    public Reimbursement approve(Long id, Integer action, String comment) {
        // 审批是"校验 → 插审批记录 → 改状态"的复合操作，并发时可能写入两条审批记录、
        // 状态互相覆盖（last-write-wins），用分布式锁串行化同一单据的审批；
        // 拿不到锁说明有请求正在处理，直接提示前端
        return redisLock.executeWithLock(LOCK_APPROVE_PREFIX + id, 10, () -> {
            if (action == null || (action != 1 && action != 2)) {
                throw new BusinessException("无效的审批操作");
            }

            Reimbursement reimbursement = getById(id);
            if (reimbursement == null) {
                throw new BusinessException("报销单不存在");
            }
            if (reimbursement.getStatus() != 1 && reimbursement.getStatus() != 2) {
                throw new BusinessException("当前状态不可审批");
            }

            SysUser current = currentUser();
            int role = current.getRole();

            // 防止自审自批：任何角色都不能审批自己提交的单据，否则审批流形同虚设
            if (current.getId().equals(reimbursement.getUserId())) {
                throw new BusinessException("不能审批自己提交的单据");
            }

            // 二级审批（审批中）只能由财务/管理员操作
            if (reimbursement.getStatus() == 2 && role != 3 && role != 4) {
                throw new BusinessException("当前单据需财务审批");
            }
            // 一级审批（待审批）时，领导只能审本部门
            if (reimbursement.getStatus() == 1 && role == 2) {
                SysUser applicant = userMapper.selectById(reimbursement.getUserId());
                if (applicant == null || !current.getDeptId().equals(applicant.getDeptId())) {
                    throw new BusinessException("只能审批本部门的报销单");
                }
            }

            // 记录审批
            ApprovalRecord record = new ApprovalRecord();
            record.setReimbursementId(id);
            record.setApproverId(current.getId());
            record.setAction(action);
            record.setComment(comment);
            record.setNodeName(reimbursement.getStatus() == 1 ? "一级审批" : "二级审批");
            record.setCreateTime(LocalDateTime.now());
            approvalRecordMapper.insert(record);

            if (action == 2) {
                // 驳回
                reimbursement.setStatus(4);
                reimbursement.setRejectReason(comment);
            } else {
                // 通过：大额单需两级（领导→财务），小额或财务/管理员终审直接通过
                boolean bigAmount = reimbursement.getTotalAmount() != null
                        && reimbursement.getTotalAmount().compareTo(APPROVE_THRESHOLD) >= 0;
                if (reimbursement.getStatus() == 1 && bigAmount && role != 3 && role != 4) {
                    reimbursement.setStatus(2);
                } else {
                    reimbursement.setStatus(3);
                }
            }

            reimbursement.setUpdateTime(LocalDateTime.now());
            updateById(reimbursement);

            // 审批结果通知申请人（仅终态：驳回或最终通过）
            if (action == 2) {
                String reason = comment != null && !comment.isEmpty() ? "：" + comment : "";
                noticeService.send(reimbursement.getUserId(), "报销被驳回",
                        "报销单 " + reimbursement.getReimburseNo() + " 已被驳回" + reason,
                        "/reimbursement/detail/" + id);
            } else if (reimbursement.getStatus() == 2) {
                noticeService.send(reimbursement.getUserId(), "报销进入二级审批",
                        "报销单 " + reimbursement.getReimburseNo() + " 已通过一级审批，进入财务二级审批",
                        "/reimbursement/detail/" + id);
            } else if (reimbursement.getStatus() == 3) {
                noticeService.send(reimbursement.getUserId(), "报销审批通过",
                        "报销单 " + reimbursement.getReimburseNo() + " 已审批通过，等待打款",
                        "/reimbursement/detail/" + id);
            }

            log.info("报销单审批成功, id: {}, action: {}, newStatus: {}", id, action, reimbursement.getStatus());
            return reimbursement;
        });
    }

    @Override
    @Transactional
    public Reimbursement pay(Long id) {
        // 双层防护：分布式锁挡掉并发重复打款请求；
        // 即使锁失效（Redis 宕机/多实例锁粒度问题），下方乐观更新 WHERE status=3 依然兜底
        return redisLock.executeWithLock(LOCK_PAY_PREFIX + id, 10, () -> {
            Reimbursement reimbursement = getById(id);
            if (reimbursement == null) {
                throw new BusinessException("报销单不存在");
            }
            if (reimbursement.getStatus() != 3) {
                throw new BusinessException("只有已通过的报销单才能打款");
            }
            SysUser current = currentUser();
            LocalDateTime now = LocalDateTime.now();

            // 乐观锁：仅当状态仍为「已通过」时更新，防止并发重复打款
            int rows = reimbursementMapper.payIfApproved(id, now, current.getId(), current.getRealName(), now);
            if (rows == 0) {
                throw new BusinessException("只有已通过的报销单才能打款");
            }

            reimbursement.setStatus(5);
            reimbursement.setPayTime(now);
            reimbursement.setPayUserId(current.getId());
            reimbursement.setPayUserName(current.getRealName());
            reimbursement.setUpdateTime(now);

            noticeService.send(reimbursement.getUserId(), "报销已打款",
                    "报销单 " + reimbursement.getReimburseNo() + " 已完成打款，金额 " + reimbursement.getTotalAmount() + " 元",
                    "/reimbursement/detail/" + id);

            log.info("报销单打款成功, id: {}", id);
            return reimbursement;
        });
    }

    @Override
    public Page<ReimbursementVO> list(Integer pageNum, Integer pageSize, Integer status,
                                      String keyword, String startDate, String endDate) {
        SysUser current = currentUser();
        Integer role = current.getRole();
        Long currentUserId = current.getId();
        Long deptId = role == 2 ? current.getDeptId() : null;
        return reimbursementMapper.selectPageVO(new Page<>(pageNum, pageSize), status, role, currentUserId, deptId, keyword, startDate, endDate);
    }

    @Override
    public List<ReimbursementExportVO> exportList(Integer status, String keyword, String startDate, String endDate) {
        SysUser current = currentUser();
        Integer role = current.getRole();
        Long currentUserId = current.getId();
        Long deptId = role == 2 ? current.getDeptId() : null;
        // 导出复用列表的数据范围过滤。分页插件全局 maxLimit=100（防大查询拖垮 DB），
        // 这里循环分页逐页取回全部数据，直到取到不足一页为止，避免导出被静默截断
        List<ReimbursementVO> all = new ArrayList<>();
        long pageNum = 1;
        while (true) {
            Page<ReimbursementVO> page = reimbursementMapper.selectPageVO(
                    new Page<>(pageNum, 100), status, role, currentUserId, deptId, keyword, startDate, endDate);
            all.addAll(page.getRecords());
            if (page.getRecords().size() < 100) {
                break;
            }
            pageNum++;
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return all.stream().map(vo -> {
            ReimbursementExportVO e = new ReimbursementExportVO();
            e.setOrderNo(vo.getOrderNo());
            e.setApplicantName(vo.getApplicantName());
            e.setDeptName(vo.getDeptName());
            e.setAmount(vo.getAmount());
            e.setStatusText(statusText(vo.getStatus()));
            e.setInvoiceCount(vo.getInvoiceCount());
            e.setRemark(vo.getRemark());
            e.setCreateTime(vo.getCreateTime() == null ? "" : vo.getCreateTime().format(dtf));
            e.setStatus(vo.getStatus());
            return e;
        }).collect(Collectors.toList());
    }

    private String statusText(Integer s) {
        if (s == null) {
            return "";
        }
        switch (s) {
            case 0: return "草稿";
            case 1: return "待审批";
            case 2: return "审批中";
            case 3: return "已通过";
            case 4: return "已驳回";
            case 5: return "已打款";
            default: return "";
        }
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
    public ReimbursementDetailVO detail(Long id) {
        ReimbursementDetailVO vo = reimbursementMapper.selectDetailById(id);
        if (vo == null) {
            throw new BusinessException("报销单不存在");
        }
        SysUser current = currentUser();
        checkViewPermission(current, vo.getUserId(), vo.getDeptId(), vo.getStatus());
        vo.setInvoices(invoiceMapper.selectList(
                new LambdaQueryWrapper<Invoice>().eq(Invoice::getReimbursementId, id)));
        vo.setApprovalRecords(approvalRecordMapper.selectByReimbursementId(id));
        return vo;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Reimbursement existing = getById(id);
        if (existing.getStatus() != 0) {
            throw new BusinessException("只有草稿状态的报销单才能删除");
        }
        SysUser current = currentUser();
        if (current.getRole() != 4 && !existing.getUserId().equals(current.getId())) {
            throw new BusinessException("只能操作自己的单据");
        }
        removeById(id);
        log.info("报销单删除成功, id: {}", id);
    }

    @Override
    public Reimbursement getById(Long id) {
        Reimbursement reimbursement = reimbursementMapper.selectById(id);
        if (reimbursement == null) {
            throw new BusinessException("报销单不存在");
        }
        return reimbursement;
    }
}
