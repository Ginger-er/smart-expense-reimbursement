package com.smartexpense.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartexpense.entity.Trip;
import com.smartexpense.vo.TripDetailVO;
import com.smartexpense.vo.TripVO;

public interface TripService {

    Trip create(Trip trip);

    Trip update(Trip trip);

    void delete(Long id);

    Trip getById(Long id);

    Page<TripVO> list(Integer pageNum, Integer pageSize, Integer status,
                      String keyword, String startDate, String endDate);

    Trip submit(Long id);

    /** 审批出差申请：1通过 2驳回 */
    Trip approve(Long id, Integer action, String comment);

    /** 出差申请详情（含审批记录 + 申请人） */
    TripDetailVO getDetail(Long id);
}
