package com.smartexpense.service;

import com.smartexpense.entity.Invoice;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InvoiceService {

    /** 上传发票文件，可关联出差/报销单 */
    Invoice upload(MultipartFile file, Long tripId, Long reimbursementId);

    /** OCR识别发票信息 */
    Invoice ocrRecognize(Long invoiceId);

    /** 校验发票真伪 */
    Invoice validate(Long invoiceId);

    /** 人工确认/修正发票信息 */
    Invoice confirm(Long invoiceId, Invoice invoice);

    /** 查询发票列表 */
    List<Invoice> list(Long tripId, Long reimbursementId);

    /** 根据ID查询 */
    Invoice getById(Long id);

    /** 查询发票详情（带数据范围权限校验：员工看自己、领导看本部门、财务/管理员看全部） */
    Invoice detail(Long id);

    /** 标记 OCR 识别失败（异步任务兜底，防止发票永远停留在"识别中"） */
    void markOcrFailed(Long invoiceId);

    /** 删除发票 */
    void delete(Long id);
}
