package com.smartexpense.service.abnormal;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartexpense.entity.AbnormalRecord;

/**
 * 异常预警业务服务：列表查询、标记处理、手动触发扫描。
 */
public interface AbnormalService {

    /** 分页查询预警记录，handled 为空表示不过滤 */
    Page<AbnormalRecord> page(Integer pageNum, Integer pageSize, Integer handled);

    /** 标记预警已处理 */
    void handle(Long id);

    /** 手动触发扫描昨日数据，返回新增记录数 */
    int scan();
}
