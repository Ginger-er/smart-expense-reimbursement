package com.smartexpense.service;

import com.smartexpense.entity.SysNotice;

import java.util.List;

public interface NoticeService {

    /** 当前用户的通知列表（最新 50 条） */
    List<SysNotice> list();

    /** 当前用户未读数 */
    Long unreadCount();

    /** 标记单条已读 */
    void markRead(Long id);

    /** 全部已读 */
    void markAllRead();

    /** 发送通知（失败不影响业务） */
    void send(Long userId, String title, String content, String link);
}
