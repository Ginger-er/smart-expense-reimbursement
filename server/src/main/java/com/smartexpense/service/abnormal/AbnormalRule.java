package com.smartexpense.service.abnormal;

import com.smartexpense.entity.AbnormalRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异常预警规则（策略模式）：一条规则 = 一个策略实现类。
 *
 * <p>新增规则只需新增实现类并注册为 Spring Bean，规则引擎自动纳入扫描，
 * 符合开闭原则——对扩展开放、对修改关闭。
 */
public interface AbnormalRule {

    /** 规则编码（A001、A002……） */
    String code();

    /** 规则名称 */
    String name();

    /** 在给定时间窗口内扫描，返回命中的异常记录（不含 id/createTime，由扫描服务落库） */
    List<AbnormalRecord> check(LocalDateTime start, LocalDateTime end);
}
