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
        log.info("开始异步OCR识别, invoiceId: {}", invoiceId);
        invoiceService.ocrRecognize(invoiceId);
        log.info("异步OCR识别完成, invoiceId: {}", invoiceId);
    }
}
