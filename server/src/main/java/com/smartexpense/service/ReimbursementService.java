package com.smartexpense.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartexpense.entity.Reimbursement;
import com.smartexpense.vo.ReimbursementDetailVO;
import com.smartexpense.vo.ReimbursementExportVO;
import com.smartexpense.vo.ReimbursementVO;

import java.util.List;

public interface ReimbursementService {

    /** 创建报销单 */
    Reimbursement create(Reimbursement reimbursement);

    /** 提交报销单 */
    Reimbursement submit(Long id);

    /** 审批报销单 */
    Reimbursement approve(Long id, Integer action, String comment);

    /** 打款（财务/管理员，已通过→已打款） */
    Reimbursement pay(Long id);

    /** 报销单列表 */
    Page<ReimbursementVO> list(Integer pageNum, Integer pageSize, Integer status,
                               String keyword, String startDate, String endDate);

    /** 报销单详情 */
    ReimbursementDetailVO detail(Long id);

    /** 删除报销单 */
    void delete(Long id);

    /** 报销单导出（Excel，按数据范围过滤，不分页） */
    List<ReimbursementExportVO> exportList(Integer status, String keyword, String startDate, String endDate);

    /** 根据ID查询 */
    Reimbursement getById(Long id);
}
