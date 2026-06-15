package com.smartexpense.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.smartexpense.vo.ReimbursementDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReimbursementServiceImplTest {

    @Mock
    private ReimbursementMapper reimbursementMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private InvoiceMapper invoiceMapper;
    @Mock
    private ApprovalRecordMapper approvalRecordMapper;
    @Mock
    private NoticeService noticeService;
    @Mock
    private RedisLock redisLock;

    @InjectMocks
    private ReimbursementServiceImpl service;

    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(service, "baseMapper", reimbursementMapper);
        // 默认透传：锁内 action 直接执行、加锁成功——让业务断言聚焦在业务逻辑上
        lenient().when(redisLock.executeWithLock(anyString(), anyLong(), any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(2)).get());
        lenient().when(redisLock.tryLock(anyString(), anyString(), anyLong())).thenReturn(true);
    }

    private SysUser user(long id, int role, long deptId) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setRole(role);
        u.setDeptId(deptId);
        u.setRealName("测试用户");
        return u;
    }

    private Reimbursement reimbursement(long id, long userId, int status, String amount) {
        Reimbursement r = new Reimbursement();
        r.setId(id);
        r.setUserId(userId);
        r.setStatus(status);
        r.setTotalAmount(amount == null ? null : new BigDecimal(amount));
        return r;
    }

    @Test
    void submit_shouldSumInvoiceAmount() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));
            when(reimbursementMapper.selectById(1L)).thenReturn(reimbursement(1L, 2L, 0, null));

            Invoice i1 = new Invoice();
            i1.setAmount(new BigDecimal("120.50"));
            Invoice i2 = new Invoice();
            i2.setAmount(new BigDecimal("30.00"));
            when(invoiceMapper.selectList(any())).thenReturn(List.of(i1, i2));

            Reimbursement result = service.submit(1L);

            assertEquals(1, result.getStatus());
            assertEquals(0, new BigDecimal("150.50").compareTo(result.getTotalAmount()));
            assertEquals(2, result.getInvoiceCount());
        }
    }

    @Test
    void submit_withoutInvoice_shouldThrow() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));
            when(reimbursementMapper.selectById(1L)).thenReturn(reimbursement(1L, 2L, 0, null));
            when(invoiceMapper.selectList(any())).thenReturn(List.of());

            assertThrows(BusinessException.class, () -> service.submit(1L));
        }
    }

    @Test
    void approve_bigAmount_byManager_shouldGoSecondLevel() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(userMapper.selectById(3L)).thenReturn(user(3L, 2, 2L));
            when(reimbursementMapper.selectById(1L)).thenReturn(reimbursement(1L, 2L, 1, "8000"));
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));

            Reimbursement result = service.approve(1L, 1, "同意");

            assertEquals(2, result.getStatus());
            verify(noticeService).send(eq(2L), eq("报销进入二级审批"), anyString(), anyString());
        }
    }

    @Test
    void approve_smallAmount_shouldPassDirectly() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(userMapper.selectById(3L)).thenReturn(user(3L, 2, 2L));
            when(reimbursementMapper.selectById(1L)).thenReturn(reimbursement(1L, 2L, 1, "1000"));
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));

            Reimbursement result = service.approve(1L, 1, "同意");

            assertEquals(3, result.getStatus());
            verify(noticeService).send(eq(2L), eq("报销审批通过"), anyString(), anyString());
        }
    }

    @Test
    void pay_onlyApprovedCanPay() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(userMapper.selectById(1L)).thenReturn(user(1L, 4, 1L));
            when(reimbursementMapper.selectById(1L)).thenReturn(reimbursement(1L, 2L, 3, "1000"));
            when(reimbursementMapper.payIfApproved(any(), any(), any(), any(), any())).thenReturn(1);

            Reimbursement result = service.pay(1L);

            assertEquals(5, result.getStatus());
            assertNotNull(result.getPayTime());
            assertEquals(1L, result.getPayUserId());
        }
    }

    @Test
    void pay_notApproved_shouldThrow() {
        when(reimbursementMapper.selectById(1L)).thenReturn(reimbursement(1L, 2L, 1, "1000"));

        assertThrows(BusinessException.class, () -> service.pay(1L));
    }

    @Test
    void approve_reject_shouldSetRejected() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(userMapper.selectById(3L)).thenReturn(user(3L, 2, 2L));
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));
            when(reimbursementMapper.selectById(1L)).thenReturn(reimbursement(1L, 2L, 1, "1000"));

            Reimbursement result = service.approve(1L, 2, "发票有问题");

            assertEquals(4, result.getStatus());
            assertEquals("发票有问题", result.getRejectReason());
            verify(noticeService).send(eq(2L), eq("报销被驳回"), anyString(), anyString());
        }
    }

    @Test
    void approve_notApprovableStatus_shouldThrow() {
        when(reimbursementMapper.selectById(1L)).thenReturn(reimbursement(1L, 2L, 3, "1000"));

        assertThrows(BusinessException.class, () -> service.approve(1L, 1, "同意"));
    }

    @Test
    void approve_secondLevel_byManager_shouldThrow() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(userMapper.selectById(3L)).thenReturn(user(3L, 2, 2L));
            when(reimbursementMapper.selectById(1L)).thenReturn(reimbursement(1L, 2L, 2, "8000"));

            assertThrows(BusinessException.class, () -> service.approve(1L, 1, "同意"));
        }
    }

    @Test
    void approve_otherDept_byManager_shouldThrow() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(userMapper.selectById(3L)).thenReturn(user(3L, 2, 2L));
            when(reimbursementMapper.selectById(1L)).thenReturn(reimbursement(1L, 2L, 1, "1000"));
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 5L));

            assertThrows(BusinessException.class, () -> service.approve(1L, 1, "同意"));
        }
    }

    @Test
    void submit_notDraft_shouldThrow() {
        when(reimbursementMapper.selectById(1L)).thenReturn(reimbursement(1L, 2L, 1, "1000"));

        assertThrows(BusinessException.class, () -> service.submit(1L));
    }

    @Test
    void submit_rejected_shouldResubmitAndClearReason() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));
            Reimbursement rejected = reimbursement(1L, 2L, 4, "1000");
            rejected.setRejectReason("发票有问题");
            when(reimbursementMapper.selectById(1L)).thenReturn(rejected);

            Invoice i1 = new Invoice();
            i1.setAmount(new BigDecimal("1000"));
            when(invoiceMapper.selectList(any())).thenReturn(List.of(i1));

            Reimbursement result = service.submit(1L);

            assertEquals(1, result.getStatus());
            assertEquals(null, result.getRejectReason());
        }
    }

    @Test
    void approve_ownReimbursement_shouldThrow() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 2, 2L));
            when(reimbursementMapper.selectById(1L)).thenReturn(reimbursement(1L, 2L, 1, "1000"));

            assertThrows(BusinessException.class, () -> service.approve(1L, 1, "同意"));
        }
    }

    @Test
    void submit_duplicateConcurrent_shouldThrow() {
        when(redisLock.tryLock(anyString(), anyString(), anyLong())).thenReturn(false); // 已有请求在提交

        assertThrows(BusinessException.class, () -> service.submit(1L));
    }

    @Test
    void submit_success_shouldReleaseIdempotencyKey() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));
            when(reimbursementMapper.selectById(1L)).thenReturn(reimbursement(1L, 2L, 0, null));
            Invoice i1 = new Invoice();
            i1.setAmount(new BigDecimal("100"));
            when(invoiceMapper.selectList(any())).thenReturn(List.of(i1));

            service.submit(1L);

            verify(redisLock).unlock(eq("idem:submit:1"), anyString());
        }
    }

    @Test
    void approve_shouldExecuteWithinLock() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(userMapper.selectById(3L)).thenReturn(user(3L, 2, 2L));
            when(reimbursementMapper.selectById(1L)).thenReturn(reimbursement(1L, 2L, 1, "1000"));
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));

            service.approve(1L, 1, "同意");

            verify(redisLock).executeWithLock(eq("lock:reimb:approve:1"), eq(10L), any());
        }
    }

    @Test
    void pay_shouldExecuteWithinLock() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(userMapper.selectById(1L)).thenReturn(user(1L, 4, 1L));
            when(reimbursementMapper.selectById(1L)).thenReturn(reimbursement(1L, 2L, 3, "1000"));
            when(reimbursementMapper.payIfApproved(any(), any(), any(), any(), any())).thenReturn(1);

            service.pay(1L);

            verify(redisLock).executeWithLock(eq("lock:reimb:pay:1"), eq(10L), any());
        }
    }

    @Test
    void detail_own_shouldReturn() {
        ReimbursementDetailVO detail = new ReimbursementDetailVO();
        detail.setUserId(2L);
        detail.setDeptId(2L);
        detail.setStatus(3);
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));
            when(reimbursementMapper.selectDetailById(1L)).thenReturn(detail);
            when(invoiceMapper.selectList(any())).thenReturn(List.of());
            when(approvalRecordMapper.selectByReimbursementId(1L)).thenReturn(List.of());

            ReimbursementDetailVO result = service.detail(1L);

            assertNotNull(result);
        }
    }

    @Test
    void detail_otherUser_shouldThrow() {
        ReimbursementDetailVO detail = new ReimbursementDetailVO();
        detail.setUserId(99L);
        detail.setDeptId(3L);
        detail.setStatus(3);
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));
            when(reimbursementMapper.selectDetailById(1L)).thenReturn(detail);

            assertThrows(BusinessException.class, () -> service.detail(1L));
        }
    }

    @Test
    void list_employee_shouldPassNullDept() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));
            when(reimbursementMapper.selectPageVO(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new Page<>());

            service.list(1, 10, null, null, null, null);

            verify(reimbursementMapper).selectPageVO(any(), isNull(), eq(1), eq(2L), isNull(), isNull(), isNull(), isNull());
        }
    }

    @Test
    void list_manager_shouldPassDeptId() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(userMapper.selectById(3L)).thenReturn(user(3L, 2, 2L));
            when(reimbursementMapper.selectPageVO(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new Page<>());

            service.list(1, 10, null, null, null, null);

            verify(reimbursementMapper).selectPageVO(any(), isNull(), eq(2), eq(3L), eq(2L), isNull(), isNull(), isNull());
        }
    }
}
