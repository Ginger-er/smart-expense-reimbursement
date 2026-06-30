package com.smartexpense.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.smartexpense.entity.Invoice;
import com.smartexpense.entity.Reimbursement;
import com.smartexpense.entity.SysUser;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.InvoiceMapper;
import com.smartexpense.mapper.ReimbursementMapper;
import com.smartexpense.mapper.SysUserMapper;
import com.smartexpense.service.BaiduOcrClient;
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
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceMapper invoiceMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private ReimbursementMapper reimbursementMapper;
    @Mock
    private BaiduOcrClient baiduOcrClient;

    @InjectMocks
    private InvoiceServiceImpl service;

    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(service, "baseMapper", invoiceMapper);
    }

    private SysUser user(long id, int role) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setRole(role);
        return u;
    }

    private Invoice invoice(long id, long userId, Long reimbursementId) {
        Invoice i = new Invoice();
        i.setId(id);
        i.setUserId(userId);
        i.setReimbursementId(reimbursementId);
        return i;
    }

    @Test
    void confirm_partialUpdate_shouldPreserveOcrFields() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1));
            Invoice existing = invoice(1L, 2L, null);
            existing.setInvoiceNo("OCR号码");
            existing.setTaxAmount(new BigDecimal("13.00"));
            existing.setBuyerName("OCR购买方");
            existing.setAmount(new BigDecimal("100"));
            when(invoiceMapper.selectById(1L)).thenReturn(existing);

            // 模拟前端只传 6 个字段（不含 taxAmount/buyerName）
            Invoice updateData = new Invoice();
            updateData.setInvoiceNo("人工修正号码");
            updateData.setType(1);
            updateData.setAmount(new BigDecimal("120"));
            updateData.setSellerName("销售方");

            Invoice result = service.confirm(1L, updateData);

            assertEquals("人工修正号码", result.getInvoiceNo());
            assertEquals("OCR购买方", result.getBuyerName()); // 未传字段保留OCR值
            assertEquals(0, new BigDecimal("13.00").compareTo(result.getTaxAmount()));
            assertEquals(1, result.getType());
        }
    }

    @Test
    void confirm_nonPositiveAmount_shouldThrow() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1));
            when(invoiceMapper.selectById(1L)).thenReturn(invoice(1L, 2L, null));

            Invoice updateData = new Invoice();
            updateData.setAmount(new BigDecimal("-1"));

            assertThrows(BusinessException.class, () -> service.confirm(1L, updateData));
        }
    }

    @Test
    void confirm_nonDraftReimbursement_shouldThrow() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1));
            when(invoiceMapper.selectById(1L)).thenReturn(invoice(1L, 2L, 9L));
            Reimbursement submitted = new Reimbursement();
            submitted.setStatus(1); // 已提交，不可再改发票
            when(reimbursementMapper.selectById(9L)).thenReturn(submitted);

            Invoice updateData = new Invoice();
            updateData.setAmount(new BigDecimal("100"));

            assertThrows(BusinessException.class, () -> service.confirm(1L, updateData));
        }
    }

    @Test
    void confirm_duplicateInvoiceNo_shouldThrow() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1));
            when(invoiceMapper.selectById(1L)).thenReturn(invoice(1L, 2L, 9L));
            Reimbursement draft = new Reimbursement();
            draft.setStatus(0);
            when(reimbursementMapper.selectById(9L)).thenReturn(draft);
            when(invoiceMapper.selectCount(any())).thenReturn(1L); // 已有同号发票

            Invoice updateData = new Invoice();
            updateData.setInvoiceNo("INV-001");
            updateData.setAmount(new BigDecimal("100"));

            assertThrows(BusinessException.class, () -> service.confirm(1L, updateData));
        }
    }

    @Test
    void detail_otherUser_employee_shouldThrow() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1));
            when(invoiceMapper.selectById(1L)).thenReturn(invoice(1L, 99L, null));

            assertThrows(BusinessException.class, () -> service.detail(1L));
        }
    }

    @Test
    void detail_otherDept_manager_shouldThrow() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            SysUser manager = user(3L, 2);
            manager.setDeptId(2L);
            when(userMapper.selectById(3L)).thenReturn(manager);
            when(invoiceMapper.selectById(1L)).thenReturn(invoice(1L, 99L, null));
            SysUser owner = user(99L, 1);
            owner.setDeptId(5L); // 其他部门
            when(userMapper.selectById(99L)).thenReturn(owner);

            assertThrows(BusinessException.class, () -> service.detail(1L));
        }
    }

    @Test
    void markOcrFailed_pendingInvoice_shouldSetFailed() {
        Invoice pending = invoice(1L, 2L, null);
        pending.setOcrStatus(0);
        when(invoiceMapper.selectById(1L)).thenReturn(pending);

        service.markOcrFailed(1L);

        assertEquals(2, pending.getOcrStatus());
    }

    @Test
    void markOcrFailed_alreadyFinished_shouldNotOverride() {
        Invoice done = invoice(1L, 2L, null);
        done.setOcrStatus(1); // 已识别成功
        when(invoiceMapper.selectById(1L)).thenReturn(done);

        service.markOcrFailed(1L);

        assertEquals(1, done.getOcrStatus());
    }

    @Test
    void detail_ownInvoice_shouldReturn() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1));
            when(invoiceMapper.selectById(1L)).thenReturn(invoice(1L, 2L, null));

            Invoice result = service.detail(1L);

            assertEquals(1L, result.getId());
        }
    }
}
