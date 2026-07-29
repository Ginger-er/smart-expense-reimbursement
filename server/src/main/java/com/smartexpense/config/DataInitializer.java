package com.smartexpense.config;

import cn.dev33.satoken.secure.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartexpense.entity.AbnormalRecord;
import com.smartexpense.entity.ApprovalRecord;
import com.smartexpense.entity.Invoice;
import com.smartexpense.entity.Reimbursement;
import com.smartexpense.entity.SysDept;
import com.smartexpense.entity.SysUser;
import com.smartexpense.entity.Trip;
import com.smartexpense.mapper.AbnormalRecordMapper;
import com.smartexpense.mapper.ApprovalRecordMapper;
import com.smartexpense.mapper.InvoiceMapper;
import com.smartexpense.mapper.ReimbursementMapper;
import com.smartexpense.mapper.SysDeptMapper;
import com.smartexpense.mapper.SysUserMapper;
import com.smartexpense.mapper.TripMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final ReimbursementMapper reimbursementMapper;
    private final TripMapper tripMapper;
    private final InvoiceMapper invoiceMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final AbnormalRecordMapper abnormalRecordMapper;

    @Override
    public void run(String... args) {
        initDepts();
        initUsers();
        initDemoData();
        initDemoAbnormalRecords();
    }

    private void initDepts() {
        if (deptMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;
        deptMapper.insert(buildDept(1L, 0L, "总公司", 0));
        deptMapper.insert(buildDept(2L, 1L, "技术部", 1));
        deptMapper.insert(buildDept(3L, 1L, "市场部", 2));
        deptMapper.insert(buildDept(4L, 1L, "财务部", 3));
        deptMapper.insert(buildDept(5L, 1L, "人事部", 4));
        log.info("部门初始数据已创建");
    }

    private void initUsers() {
        if (userMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        String pwd = BCrypt.hashpw("123456", BCrypt.gensalt());
        userMapper.insert(buildUser(1L, "admin", pwd, "系统管理员", 1L, 4, "13800000001"));
        userMapper.insert(buildUser(2L, "zhangsan", pwd, "张三", 2L, 1, "13800000002"));
        userMapper.insert(buildUser(3L, "lisi", pwd, "李四", 2L, 2, "13800000003"));
        userMapper.insert(buildUser(4L, "wangwu", pwd, "王五", 4L, 3, "13800000004"));
        log.info("测试用户已创建，密码均为: 123456");
    }

    /** 演示数据：报销 6 条 + 出差 5 条，覆盖各自全部状态，方便跑起来就能体验完整流程 */
    private void initDemoData() {
        initDemoReimbursements();
        initDemoTrips();
    }

    /** 演示预警记录 3 条（对应 3 条规则），预警页开箱即有内容可看 */
    private void initDemoAbnormalRecords() {
        if (abnormalRecordMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;

        abnormalRecordMapper.insert(buildAbnormalRecord(
                "A001", "重复发票", "31002408881241|2", null, 2L, 2L,
                "发票号 31002408881241 同时出现在多个报销单中，疑似重复报销"));
        abnormalRecordMapper.insert(buildAbnormalRecord(
                "A002", "发票日期异常", "inv-1", null, 1L, 2L,
                "发票开票日期不在关联出差单的行程范围内，请核对行程与票据时间"));
        abnormalRecordMapper.insert(buildAbnormalRecord(
                "A003", "金额突增", "2|2026-08", 1L, null, 2L,
                "本月报销总额较上月增长超过 1.5 倍阈值，请关注报销合理性"));
        log.info("演示预警记录已创建");
    }

    private AbnormalRecord buildAbnormalRecord(String code, String name, String bizKey,
                                               Long reimbursementId, Long invoiceId, Long userId, String message) {
        AbnormalRecord r = new AbnormalRecord();
        r.setRuleCode(code);
        r.setRuleName(name);
        r.setBizKey(bizKey);
        r.setReimbursementId(reimbursementId);
        r.setInvoiceId(invoiceId);
        r.setUserId(userId);
        r.setMessage(message);
        r.setHandled(0);
        r.setCreateTime(LocalDateTime.now());
        return r;
    }

    private void initDemoReimbursements() {
        if (reimbursementMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;
        SysUser admin = findByUsername("admin");
        SysUser applicant = findByUsername("zhangsan");
        SysUser leader = findByUsername("lisi");
        SysUser finance = findByUsername("wangwu");
        if (admin == null || applicant == null || leader == null || finance == null) {
            log.warn("演示报销数据未创建：测试用户不完整");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Long adminId = admin.getId();          // 系统管理员
        Long applicantId = applicant.getId();  // 张三（员工）
        Long leaderId = leader.getId();        // 李四（技术部领导）
        Long financeId = finance.getId();      // 王五（财务）

        // 1 草稿
        reimbursementMapper.insert(buildReimbursement(applicantId, "BX202608120001",
                BigDecimal.ZERO, 0, 0, "出差回来补发票", null, null, null, null, now.minusDays(1)));

        // 2 待审批
        Reimbursement r1 = buildReimbursement(applicantId, "BX202608120002",
                new BigDecimal("356.50"), 2, 1, "客户拜访交通餐饮", null, null, null, null, now.minusDays(2));
        reimbursementMapper.insert(r1);
        invoiceMapper.insert(buildInvoice(applicantId, r1.getId(), "31002408881234", new BigDecimal("156.50"), 1, LocalDate.now().minusDays(2)));
        invoiceMapper.insert(buildInvoice(applicantId, r1.getId(), "31002408881235", new BigDecimal("200.00"), 3, LocalDate.now().minusDays(2)));

        // 3 审批中（大额，领导已过一级，待财务二级）
        Reimbursement r2 = buildReimbursement(applicantId, "BX202608120003",
                new BigDecimal("6800.00"), 1, 2, "设备采购", null, null, null, null, now.minusDays(3));
        reimbursementMapper.insert(r2);
        invoiceMapper.insert(buildInvoice(applicantId, r2.getId(), "31002408881236", new BigDecimal("6800.00"), 4, LocalDate.now().minusDays(3)));
        approvalRecordMapper.insert(buildApprovalRecord(null, r2.getId(), leaderId, 1, "同意，符合采购预算", "一级审批", now.minusDays(2)));

        // 4 已通过
        Reimbursement r3 = buildReimbursement(applicantId, "BX202608120004",
                new BigDecimal("1280.00"), 2, 3, "上海出差住宿交通", null, null, null, null, now.minusDays(4));
        reimbursementMapper.insert(r3);
        invoiceMapper.insert(buildInvoice(applicantId, r3.getId(), "31002408881237", new BigDecimal("880.00"), 2, LocalDate.now().minusDays(4)));
        invoiceMapper.insert(buildInvoice(applicantId, r3.getId(), "31002408881238", new BigDecimal("400.00"), 1, LocalDate.now().minusDays(4)));
        approvalRecordMapper.insert(buildApprovalRecord(null, r3.getId(), leaderId, 1, "同意", "一级审批", now.minusDays(3)));

        // 5 已驳回
        Reimbursement r4 = buildReimbursement(applicantId, "BX202608120005",
                new BigDecimal("520.00"), 1, 4, "办公用品", "发票抬头有误，请重新开具", null, null, null, now.minusDays(5));
        reimbursementMapper.insert(r4);
        invoiceMapper.insert(buildInvoice(applicantId, r4.getId(), "31002408881239", new BigDecimal("520.00"), 4, LocalDate.now().minusDays(5)));
        approvalRecordMapper.insert(buildApprovalRecord(null, r4.getId(), leaderId, 2, "发票抬头有误，请重新开具", "一级审批", now.minusDays(4)));

        // 6 已打款（大额，两级审批后财务打款）
        Reimbursement r5 = buildReimbursement(applicantId, "BX202608120006",
                new BigDecimal("6800.00"), 3, 5, "季度差旅汇总", null, now.minusDays(1), adminId, admin.getRealName(), now.minusDays(6));
        reimbursementMapper.insert(r5);
        invoiceMapper.insert(buildInvoice(applicantId, r5.getId(), "31002408881240", new BigDecimal("3000.00"), 1, LocalDate.now().minusDays(6)));
        invoiceMapper.insert(buildInvoice(applicantId, r5.getId(), "31002408881241", new BigDecimal("2000.00"), 2, LocalDate.now().minusDays(6)));
        invoiceMapper.insert(buildInvoice(applicantId, r5.getId(), "31002408881242", new BigDecimal("1800.00"), 3, LocalDate.now().minusDays(6)));
        approvalRecordMapper.insert(buildApprovalRecord(null, r5.getId(), leaderId, 1, "同意", "一级审批", now.minusDays(3)));
        approvalRecordMapper.insert(buildApprovalRecord(null, r5.getId(), financeId, 1, "财务复核通过", "二级审批", now.minusDays(2)));

        log.info("演示报销数据已创建（6 条，覆盖 0-5 全部状态）");
    }

    private void initDemoTrips() {
        if (tripMapper.selectCount(new LambdaQueryWrapper<>()) > 0) return;
        SysUser applicant = findByUsername("zhangsan");
        SysUser leader = findByUsername("lisi");
        if (applicant == null || leader == null) {
            log.warn("演示出差数据未创建：测试用户不完整");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Long applicantId = applicant.getId(); // 张三（员工）
        Long leaderId = leader.getId();       // 李四（技术部领导）

        Trip t0 = buildTrip(applicantId, "CC202608120001", "深圳", "客户现场支持",
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(5), new BigDecimal("3000.00"), 0, now.minusDays(1));
        tripMapper.insert(t0);

        Trip t1 = buildTrip(applicantId, "CC202608120002", "北京", "行业峰会",
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), new BigDecimal("5000.00"), 1, now.minusDays(2));
        tripMapper.insert(t1);

        Trip t2 = buildTrip(applicantId, "CC202608120003", "广州", "供应商考察",
                LocalDate.now().plusDays(6), LocalDate.now().plusDays(8), new BigDecimal("8000.00"), 1, now.minusDays(3));
        tripMapper.insert(t2);

        Trip t3 = buildTrip(applicantId, "CC202608120004", "杭州", "技术交流",
                LocalDate.now().minusDays(5), LocalDate.now().minusDays(3), new BigDecimal("4000.00"), 3, now.minusDays(6));
        tripMapper.insert(t3);
        approvalRecordMapper.insert(buildApprovalRecord(t3.getId(), null, leaderId, 1, "同意", "审批", now.minusDays(5)));

        Trip t4 = buildTrip(applicantId, "CC202608120005", "成都", "展会参展",
                LocalDate.now().plusDays(15), LocalDate.now().plusDays(18), new BigDecimal("6000.00"), 4, now.minusDays(7));
        tripMapper.insert(t4);
        approvalRecordMapper.insert(buildApprovalRecord(t4.getId(), null, leaderId, 2, "预算超支，请压缩", "审批", now.minusDays(6)));

        log.info("演示出差数据已创建（5 条，覆盖 0草稿/1待审批/3已通过/4已驳回）");
    }

    private SysUser findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    private SysDept buildDept(Long id, Long parentId, String name, int sort) {
        SysDept d = new SysDept();
        d.setId(id); d.setParentId(parentId); d.setDeptName(name); d.setSortOrder(sort);
        d.setStatus(1); d.setCreateTime(LocalDateTime.now());
        return d;
    }

    private SysUser buildUser(Long id, String username, String pwd, String realName, Long deptId, int role, String phone) {
        SysUser u = new SysUser();
        u.setId(id); u.setUsername(username); u.setPassword(pwd);
        u.setRealName(realName); u.setDeptId(deptId); u.setRole(role);
        u.setPhone(phone); u.setStatus(1);
        u.setCreateTime(LocalDateTime.now()); u.setUpdateTime(LocalDateTime.now());
        return u;
    }

    private Reimbursement buildReimbursement(Long userId, String no, BigDecimal amount, int invoiceCount,
                                             int status, String remark, String rejectReason,
                                             LocalDateTime payTime, Long payUserId, String payUserName,
                                             LocalDateTime createTime) {
        Reimbursement r = new Reimbursement();
        r.setUserId(userId);
        r.setReimburseNo(no);
        r.setTotalAmount(amount);
        r.setInvoiceCount(invoiceCount);
        r.setStatus(status);
        r.setRemark(remark);
        r.setRejectReason(rejectReason);
        r.setPayTime(payTime);
        r.setPayUserId(payUserId);
        r.setPayUserName(payUserName);
        r.setCreateTime(createTime);
        r.setUpdateTime(createTime);
        return r;
    }

    private Invoice buildInvoice(Long userId, Long reimbursementId, String invoiceNo, BigDecimal amount, int type, LocalDate invoiceDate) {
        Invoice inv = new Invoice();
        inv.setUserId(userId);
        inv.setReimbursementId(reimbursementId);
        inv.setInvoiceNo(invoiceNo);
        inv.setAmount(amount);
        inv.setType(type);
        inv.setInvoiceDate(invoiceDate);
        inv.setOcrStatus(1);
        inv.setVerifyStatus(1);
        inv.setCreateTime(LocalDateTime.now());
        return inv;
    }

    private ApprovalRecord buildApprovalRecord(Long tripId, Long reimbursementId, Long approverId,
                                               int action, String comment, String nodeName, LocalDateTime createTime) {
        ApprovalRecord a = new ApprovalRecord();
        a.setTripId(tripId);
        a.setReimbursementId(reimbursementId);
        a.setApproverId(approverId);
        a.setAction(action);
        a.setComment(comment);
        a.setNodeName(nodeName);
        a.setCreateTime(createTime);
        return a;
    }

    private Trip buildTrip(Long userId, String no, String destination, String purpose,
                           LocalDate startDate, LocalDate endDate, BigDecimal budget, int status, LocalDateTime createTime) {
        Trip t = new Trip();
        t.setUserId(userId);
        t.setTripNo(no);
        t.setDestination(destination);
        t.setPurpose(purpose);
        t.setStartDate(startDate);
        t.setEndDate(endDate);
        t.setBudgetAmount(budget);
        t.setStatus(status);
        t.setCreateTime(createTime);
        t.setUpdateTime(createTime);
        return t;
    }
}
