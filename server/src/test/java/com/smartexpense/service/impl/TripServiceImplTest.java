package com.smartexpense.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartexpense.entity.SysUser;
import com.smartexpense.entity.Trip;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.ApprovalRecordMapper;
import com.smartexpense.mapper.SysUserMapper;
import com.smartexpense.mapper.TripMapper;
import com.smartexpense.service.NoticeService;
import com.smartexpense.vo.TripDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceImplTest {

    @Mock
    private TripMapper tripMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private ApprovalRecordMapper approvalRecordMapper;
    @Mock
    private NoticeService noticeService;

    @InjectMocks
    private TripServiceImpl service;

    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(service, "baseMapper", tripMapper);
    }

    private SysUser user(long id, int role, long deptId) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setRole(role);
        u.setDeptId(deptId);
        return u;
    }

    private Trip trip(long id, long userId, int status) {
        Trip t = new Trip();
        t.setId(id);
        t.setUserId(userId);
        t.setStatus(status);
        return t;
    }

    @Test
    void create_shouldGenerateTripNoAndDraftStatus() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);

            Trip result = service.create(new Trip());

            assertEquals(0, result.getStatus());
            assertEquals(2L, result.getUserId());
            assertNotNull(result.getTripNo());
            assertTrue(result.getTripNo().startsWith("CC"));
        }
    }

    @Test
    void submit_notDraft_shouldThrow() {
        when(tripMapper.selectById(1L)).thenReturn(trip(1L, 2L, 1));

        assertThrows(BusinessException.class, () -> service.submit(1L));
    }

    @Test
    void submit_draft_shouldSubmit() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));
            when(tripMapper.selectById(1L)).thenReturn(trip(1L, 2L, 0));

            Trip result = service.submit(1L);

            assertEquals(1, result.getStatus());
        }
    }

    @Test
    void approve_invalidAction_shouldThrow() {
        assertThrows(BusinessException.class, () -> service.approve(1L, 3, "x"));
    }

    @Test
    void approve_pass_shouldSetApproved() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(userMapper.selectById(3L)).thenReturn(user(3L, 3, 4L));
            when(tripMapper.selectById(1L)).thenReturn(trip(1L, 2L, 1));

            Trip result = service.approve(1L, 1, "同意");

            assertEquals(3, result.getStatus());
            verify(noticeService).send(eq(2L), eq("出差审批通过"), anyString(), anyString());
        }
    }

    @Test
    void approve_reject_shouldSetRejected() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(userMapper.selectById(3L)).thenReturn(user(3L, 3, 4L));
            when(tripMapper.selectById(1L)).thenReturn(trip(1L, 2L, 1));

            Trip result = service.approve(1L, 2, "预算超支");

            assertEquals(4, result.getStatus());
            verify(noticeService).send(eq(2L), eq("出差被驳回"), anyString(), anyString());
        }
    }

    @Test
    void approve_manager_ownDept_shouldApprove() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(userMapper.selectById(3L)).thenReturn(user(3L, 2, 2L));
            when(tripMapper.selectById(1L)).thenReturn(trip(1L, 9L, 1));
            when(userMapper.selectById(9L)).thenReturn(user(9L, 1, 2L));

            Trip result = service.approve(1L, 1, "同意");

            assertEquals(3, result.getStatus());
        }
    }

    @Test
    void approve_manager_crossDept_shouldThrow() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(userMapper.selectById(3L)).thenReturn(user(3L, 2, 2L));
            when(tripMapper.selectById(1L)).thenReturn(trip(1L, 9L, 1));
            when(userMapper.selectById(9L)).thenReturn(user(9L, 1, 5L));

            assertThrows(BusinessException.class, () -> service.approve(1L, 1, "同意"));
        }
    }

    @Test
    void approve_ownTrip_shouldThrow() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 2, 2L));
            when(tripMapper.selectById(1L)).thenReturn(trip(1L, 2L, 1));

            assertThrows(BusinessException.class, () -> service.approve(1L, 1, "同意"));
        }
    }

    @Test
    void update_rejected_shouldResetToDraft() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));
            when(tripMapper.selectById(1L)).thenReturn(trip(1L, 2L, 4));

            Trip update = trip(1L, 2L, 4);
            update.setDestination("上海");
            Trip result = service.update(update);

            assertEquals(0, result.getStatus());
            assertEquals("上海", result.getDestination());
        }
    }

    @Test
    void update_shouldIgnoreStatusAndUserIdFromClient() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));
            when(tripMapper.selectById(1L)).thenReturn(trip(1L, 2L, 0));

            Trip update = trip(1L, 99L, 3); // 恶意传入：改归属人 + 直接置为已通过
            update.setDestination("北京");
            Trip result = service.update(update);

            assertEquals(0, result.getStatus()); // 状态不被篡改
            assertEquals(2L, result.getUserId()); // 归属人不变
        }
    }

    @Test
    void getDetail_shouldFillApprovalRecords() {
        TripDetailVO detail = new TripDetailVO();
        detail.setUserId(2L);
        detail.setDeptId(2L);
        detail.setStatus(3);
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));
            when(tripMapper.selectDetailById(1L)).thenReturn(detail);
            when(approvalRecordMapper.selectByTripId(1L)).thenReturn(List.of());

            TripDetailVO vo = service.getDetail(1L);

            assertNotNull(vo);
        }
    }

    @Test
    void getDetail_otherUser_shouldThrow() {
        TripDetailVO detail = new TripDetailVO();
        detail.setUserId(99L);
        detail.setDeptId(3L);
        detail.setStatus(3);
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));
            when(tripMapper.selectDetailById(1L)).thenReturn(detail);

            assertThrows(BusinessException.class, () -> service.getDetail(1L));
        }
    }

    @Test
    void list_employee_shouldPassNullDept() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(userMapper.selectById(2L)).thenReturn(user(2L, 1, 2L));
            when(tripMapper.selectPageVO(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new Page<>());

            service.list(1, 10, null, null, null, null);

            verify(tripMapper).selectPageVO(any(), isNull(), eq(1), eq(2L), isNull(), isNull(), isNull(), isNull());
        }
    }

    @Test
    void list_manager_shouldPassDeptId() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(3L);
            when(userMapper.selectById(3L)).thenReturn(user(3L, 2, 2L));
            when(tripMapper.selectPageVO(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(new Page<>());

            service.list(1, 10, null, null, null, null);

            verify(tripMapper).selectPageVO(any(), isNull(), eq(2), eq(3L), eq(2L), isNull(), isNull(), isNull());
        }
    }
}
