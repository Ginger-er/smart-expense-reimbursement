package com.smartexpense.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知实体，对应 sys_notice 表
 */
@Data
@TableName("sys_notice")
public class SysNotice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    /** 类型：1审批结果 2系统通知 */
    private Integer type;

    /** 跳转链接 */
    private String link;

    /** 是否已读：0未读 1已读 */
    @TableField("is_read")
    private Integer isRead;

    @TableField("create_time")
    private LocalDateTime createTime;
}
