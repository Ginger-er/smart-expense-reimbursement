package com.smartexpense.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.smartexpense.entity.SysNotice;
import com.smartexpense.mapper.SysNoticeMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceImplTest {

    @Mock
    private SysNoticeMapper noticeMapper;

    @InjectMocks
    private NoticeServiceImpl service;

    @BeforeEach
    void initTableInfo() {
        // 纯单测无 Spring 上下文，手动初始化 MyBatis-Plus 实体元数据，
        // 否则 LambdaUpdateWrapper.set() 找不到列名缓存
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SysNotice.class);
    }

    private SysNotice notice(long id, long userId, int isRead) {
        SysNotice n = new SysNotice();
        n.setId(id);
        n.setUserId(userId);
        n.setIsRead(isRead);
        n.setTitle("测试通知");
        return n;
    }

    @Test
    void list_shouldQueryByCurrentUser() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);

            service.list();

            verify(noticeMapper).selectList(any());
        }
    }

    @Test
    void unreadCount_shouldReturnCount() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(noticeMapper.selectCount(any())).thenReturn(5L);

            Long count = service.unreadCount();

            assertEquals(5L, count);
        }
    }

    @Test
    void markRead_otherUsersNotice_shouldNotUpdate() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(noticeMapper.selectById(1L)).thenReturn(notice(1L, 99L, 0)); // 别人的通知

            service.markRead(1L);

            verify(noticeMapper, never()).updateById(any());
        }
    }

    @Test
    void markRead_ownNotice_shouldMarkRead() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);
            when(noticeMapper.selectById(1L)).thenReturn(notice(1L, 2L, 0));

            service.markRead(1L);

            verify(noticeMapper).updateById(any(SysNotice.class));
        }
    }

    @Test
    void markAllRead_shouldUpdateOwnUnreadOnly() {
        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(2L);

            service.markAllRead();

            verify(noticeMapper).update(eq(null), any());
        }
    }

    @Test
    void send_shouldInsertNotice() {
        service.send(2L, "标题", "内容", "/link");

        verify(noticeMapper).insert(any(SysNotice.class));
    }

    @Test
    void send_insertFailed_shouldSwallowException() {
        when(noticeMapper.insert(any(SysNotice.class))).thenThrow(new RuntimeException("db down"));

        service.send(2L, "标题", "内容", "/link"); // 通知失败不应抛异常影响业务
    }
}
