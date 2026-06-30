package com.smartexpense.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * OCR 异步识别任务：上传接口返回后，在后台线程执行识别，避免阻塞前端上传。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcrAsyncTask {

    private final InvoiceService invoiceService;

    @Async("ocrExecutor")
    public void recognize(Long invoiceId) {
        try {
            log.info("开始异步OCR识别, invoiceId: {}", invoiceId);
            invoiceService.ocrRecognize(invoiceId);
            log.info("异步OCR识别完成, invoiceId: {}", invoiceId);
        } catch (Exception e) {
            // 兜底：异步线程里任何未预期异常都要把发票状态落到"识别失败"，
            // 否则发票会永远停留在"识别中"，前端只能无限轮询
            log.error("异步OCR任务异常, invoiceId: {}", invoiceId, e);
            try {
                invoiceService.markOcrFailed(invoiceId);
            } catch (Exception markEx) {
                log.error("标记OCR失败状态也失败, invoiceId: {}", invoiceId, markEx);
            }
        }
    }
}
