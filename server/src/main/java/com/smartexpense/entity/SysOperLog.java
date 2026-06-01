package com.smartexpense.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class SysOperLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID（登录失败等未登录场景为 null） */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 操作标题 */
    private String title;

    /** 类名.方法名 */
    private String method;

    /** 请求方式 */
    private String requestMethod;

    /** 请求地址 */
    private String requestUrl;

    /** 请求参数（已脱敏） */
    private String requestParams;

    /** 来源IP */
    private String ip;

    /** 状态: 1成功 0失败 */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    /** 耗时(毫秒) */
    private Long costMs;

    @TableField("create_time")
    private LocalDateTime createTime;
}
