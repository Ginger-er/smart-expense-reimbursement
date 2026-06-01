package com.smartexpense.controller;

import com.smartexpense.common.Result;
import com.smartexpense.entity.SysNotice;
import com.smartexpense.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "消息通知", description = "站内通知查询与已读")
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "通知列表")
    @GetMapping("/list")
    public Result<List<SysNotice>> list() {
        return Result.success(noticeService.list());
    }

    @Operation(summary = "未读数")
    @GetMapping("/unread")
    public Result<Long> unread() {
        return Result.success(noticeService.unreadCount());
    }

    @Operation(summary = "标记单条已读")
    @PostMapping("/read/{id}")
    public Result<Void> read(@PathVariable Long id) {
        noticeService.markRead(id);
        return Result.success();
    }

    @Operation(summary = "全部已读")
    @PostMapping("/read-all")
    public Result<Void> readAll() {
        noticeService.markAllRead();
        return Result.success();
    }
}
