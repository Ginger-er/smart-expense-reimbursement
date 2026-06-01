package com.smartexpense.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartexpense.entity.SysNotice;
import com.smartexpense.mapper.SysNoticeMapper;
import com.smartexpense.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final SysNoticeMapper noticeMapper;

    @Override
    public List<SysNotice> list() {
        Long userId = StpUtil.getLoginIdAsLong();
        return noticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getUserId, userId)
                .orderByDesc(SysNotice::getCreateTime)
                .last("LIMIT 50"));
    }

    @Override
    public Long unreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        return noticeMapper.selectCount(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getUserId, userId)
                .eq(SysNotice::getIsRead, 0));
    }

    @Override
    public void markRead(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice != null && notice.getUserId().equals(StpUtil.getLoginIdAsLong())) {
            notice.setIsRead(1);
            noticeMapper.updateById(notice);
        }
    }

    @Override
    public void markAllRead() {
        Long userId = StpUtil.getLoginIdAsLong();
        noticeMapper.update(null, new LambdaUpdateWrapper<SysNotice>()
                .eq(SysNotice::getUserId, userId)
                .eq(SysNotice::getIsRead, 0)
                .set(SysNotice::getIsRead, 1));
    }

    @Override
    public void send(Long userId, String title, String content, String link) {
        try {
            SysNotice notice = new SysNotice();
            notice.setUserId(userId);
            notice.setTitle(title);
            notice.setContent(content);
            notice.setType(1);
            notice.setLink(link);
            notice.setIsRead(0);
            notice.setCreateTime(LocalDateTime.now());
            noticeMapper.insert(notice);
        } catch (Exception e) {
            // 通知写入失败不影响业务
            log.error("消息通知写入失败: {}", e.getMessage());
        }
    }
}
