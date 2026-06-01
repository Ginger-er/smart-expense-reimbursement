package com.smartexpense.controller;

import com.smartexpense.annotation.OperLog;
import com.smartexpense.common.Result;
import com.smartexpense.entity.Invoice;
import com.smartexpense.service.InvoiceService;
import com.smartexpense.service.OcrAsyncTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "发票管理", description = "发票上传、识别、校验、确认")
@RestController
@RequestMapping("/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final OcrAsyncTask ocrAsyncTask;

    @Operation(summary = "上传发票文件")
    @OperLog("上传发票")
    @PostMapping("/upload")
    public Result<Invoice> upload(
            @Parameter(description = "发票文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "出差申请ID") @RequestParam(required = false) Long tripId,
            @Parameter(description = "报销单ID") @RequestParam(required = false) Long reimbursementId) {
        Invoice invoice = invoiceService.upload(file, tripId, reimbursementId);
        // 异步OCR识别：上传立即返回，识别在后台线程执行，状态后续更新
        ocrAsyncTask.recognize(invoice.getId());
        return Result.success("上传成功", invoice);
    }

    @Operation(summary = "人工确认/修正发票信息")
    @OperLog("确认发票信息")
    @PostMapping("/confirm")
    public Result<Invoice> confirm(@RequestBody Invoice invoice) {
        return Result.success(invoiceService.confirm(invoice.getId(), invoice));
    }

    @Operation(summary = "发票列表查询")
    @GetMapping("/list")
    public Result<List<Invoice>> list(
            @Parameter(description = "出差申请ID") @RequestParam(required = false) Long tripId,
            @Parameter(description = "报销单ID") @RequestParam(required = false) Long reimbursementId) {
        return Result.success(invoiceService.list(tripId, reimbursementId));
    }

    @Operation(summary = "发票详情")
    @GetMapping("/{id}")
    public Result<Invoice> detail(@PathVariable Long id) {
        return Result.success(invoiceService.getById(id));
    }

    @Operation(summary = "删除发票")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return Result.success();
    }
}
