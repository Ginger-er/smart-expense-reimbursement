package com.smartexpense.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartexpense.entity.Invoice;
import com.smartexpense.entity.Reimbursement;
import com.smartexpense.entity.SysUser;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.InvoiceMapper;
import com.smartexpense.mapper.ReimbursementMapper;
import com.smartexpense.mapper.SysUserMapper;
import com.smartexpense.service.BaiduOcrClient;
import com.smartexpense.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice> implements InvoiceService {

    private final InvoiceMapper invoiceMapper;
    private final SysUserMapper userMapper;
    private final ReimbursementMapper reimbursementMapper;
    private final BaiduOcrClient baiduOcrClient;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public Invoice upload(MultipartFile file, Long tripId, Long reimbursementId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 关联报销单时校验：报销单存在、属于当前用户、且为草稿
        if (reimbursementId != null) {
            Reimbursement reimbursement = reimbursementMapper.selectById(reimbursementId);
            if (reimbursement == null) {
                throw new BusinessException("关联的报销单不存在");
            }
            SysUser current = userMapper.selectById(StpUtil.getLoginIdAsLong());
            if (current.getRole() != 4 && !reimbursement.getUserId().equals(current.getId())) {
                throw new BusinessException("只能向自己的报销单上传发票");
            }
            if (reimbursement.getStatus() != 0) {
                throw new BusinessException("只有草稿状态的报销单才能上传发票");
            }
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        if (suffix == null || suffix.isEmpty()) {
            suffix = "jpg";
        }
        String fileName = IdUtil.fastSimpleUUID() + "." + suffix;

        // 本地磁盘存储
        File dir = new File(uploadDir, "invoice");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File target = new File(dir, fileName);
        try {
            file.transferTo(target.getAbsoluteFile());
        } catch (Exception e) {
            log.error("发票文件保存失败", e);
            throw new BusinessException("发票文件保存失败");
        }

        Invoice invoice = new Invoice();
        invoice.setUserId(StpUtil.getLoginIdAsLong());
        invoice.setTripId(tripId);
        invoice.setReimbursementId(reimbursementId);
        invoice.setFileUrl("/files/invoice/" + fileName);
        invoice.setType(4); // 默认其他，识别后由用户修正
        invoice.setOcrStatus(0);
        invoice.setVerifyStatus(0);
        invoice.setCreateTime(LocalDateTime.now());
        save(invoice);

        log.info("发票上传成功, id: {}, fileName: {}", invoice.getId(), originalFilename);
        return invoice;
    }

    @Override
    @Transactional
    public Invoice ocrRecognize(Long invoiceId) {
        Invoice invoice = getById(invoiceId);
        if (invoice == null) {
            throw new BusinessException("发票不存在");
        }

        try {
            File file = resolveLocalFile(invoice.getFileUrl());
            if (file == null || !file.exists()) {
                throw new BusinessException("发票文件不存在");
            }
            byte[] bytes = FileUtil.readBytes(file);

            // 先试增值税发票识别，失败再试智能票据识别（火车票/行程单/定额发票等）
            Map<String, String> result;
            try {
                result = baiduOcrClient.recognizeVatInvoice(bytes);
            } catch (Exception vatEx) {
                result = baiduOcrClient.recognizeMultipleInvoice(bytes);
            }

            invoice.setInvoiceNo(result.get("invoiceNo"));
            invoice.setInvoiceCode(result.get("invoiceCode"));
            invoice.setAmount(parseAmount(result.get("amount")));
            invoice.setTaxAmount(parseAmount(result.get("taxAmount")));
            invoice.setInvoiceDate(parseDate(result.get("invoiceDate")));
            invoice.setSellerName(result.get("sellerName"));
            invoice.setBuyerName(result.get("buyerName"));
            // 非增值税发票：按票据类型映射为 交通/其他
            String ticketType = result.get("ticketType");
            if (ticketType != null && !ticketType.isEmpty()) {
                invoice.setType(mapTicketType(ticketType));
            }
            invoice.setOcrJson(JSONUtil.toJsonStr(result));
            invoice.setOcrStatus(1);
            log.info("票据OCR识别成功, id: {}, ticketType: {}", invoiceId, ticketType);
        } catch (Exception e) {
            invoice.setOcrStatus(2);
            log.error("票据OCR识别失败, id: {}", invoiceId, e);
        }

        updateById(invoice);
        return invoice;
    }

    @Override
    @Transactional
    public Invoice validate(Long invoiceId) {
        Invoice invoice = getById(invoiceId);
        if (invoice == null) {
            throw new BusinessException("发票不存在");
        }

        // 发票真伪查验暂未接入第三方接口，标记为已通过
        invoice.setVerifyStatus(1);
        updateById(invoice);

        log.info("发票校验完成, id: {}, status: {}", invoiceId, invoice.getVerifyStatus());
        return invoice;
    }

    @Override
    @Transactional
    public Invoice confirm(Long invoiceId, Invoice updateData) {
        Invoice invoice = getById(invoiceId);
        if (invoice == null) {
            throw new BusinessException("发票不存在");
        }

        // 越权防护：只能修正自己的发票（管理员除外）
        SysUser current = userMapper.selectById(StpUtil.getLoginIdAsLong());
        if (current.getRole() != 4 && !invoice.getUserId().equals(current.getId())) {
            throw new BusinessException("只能操作自己的发票");
        }

        // 人工修正OCR结果
        invoice.setInvoiceNo(updateData.getInvoiceNo());
        invoice.setInvoiceCode(updateData.getInvoiceCode());
        invoice.setAmount(updateData.getAmount());
        invoice.setTaxAmount(updateData.getTaxAmount());
        invoice.setInvoiceDate(updateData.getInvoiceDate());
        invoice.setType(updateData.getType());
        invoice.setSellerName(updateData.getSellerName());
        invoice.setBuyerName(updateData.getBuyerName());
        invoice.setOcrStatus(3); // 人工修正

        updateById(invoice);
        log.info("发票信息人工修正完成, id: {}", invoiceId);
        return invoice;
    }

    @Override
    public List<Invoice> list(Long tripId, Long reimbursementId) {
        SysUser current = userMapper.selectById(StpUtil.getLoginIdAsLong());
        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();
        if (tripId != null) {
            wrapper.eq(Invoice::getTripId, tripId);
        }
        if (reimbursementId != null) {
            wrapper.eq(Invoice::getReimbursementId, reimbursementId);
        }
        if (current.getRole() == 1) {
            wrapper.eq(Invoice::getUserId, current.getId());
        } else if (current.getRole() == 2) {
            wrapper.inSql(Invoice::getUserId, "SELECT id FROM sys_user WHERE dept_id = " + current.getDeptId());
        }
        wrapper.orderByDesc(Invoice::getCreateTime);
        return invoiceMapper.selectList(wrapper);
    }

    @Override
    public Invoice getById(Long id) {
        Invoice invoice = invoiceMapper.selectById(id);
        if (invoice == null) {
            throw new BusinessException("发票不存在");
        }
        return invoice;
    }

    @Override
    public Invoice detail(Long id) {
        Invoice invoice = getById(id);
        SysUser current = userMapper.selectById(StpUtil.getLoginIdAsLong());
        if (current.getRole() == 1 && !invoice.getUserId().equals(current.getId())) {
            throw new BusinessException("只能查看自己的发票");
        }
        if (current.getRole() == 2) {
            SysUser owner = userMapper.selectById(invoice.getUserId());
            if (owner == null || owner.getDeptId() == null || !owner.getDeptId().equals(current.getDeptId())) {
                throw new BusinessException("只能查看本部门的发票");
            }
        }
        return invoice;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Invoice invoice = getById(id);
        if (invoice == null) {
            throw new BusinessException("发票不存在");
        }
        SysUser current = userMapper.selectById(StpUtil.getLoginIdAsLong());
        if (current.getRole() != 4 && !invoice.getUserId().equals(current.getId())) {
            throw new BusinessException("只能操作自己的发票");
        }
        removeById(id);
        log.info("发票删除成功, id: {}", id);
    }

    private File resolveLocalFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/files/")) {
            return null;
        }
        String relative = fileUrl.substring("/files/".length());
        return new File(uploadDir, relative);
    }

    private BigDecimal parseAmount(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /** 智能票据类型 → 发票类型(1交通 2住宿 3餐饮 4其他) */
    private int mapTicketType(String t) {
        switch (t == null ? "" : t) {
            case "train_ticket":
            case "air_ticket":
            case "taxi_receipt":
            case "bus_ticket":
            case "toll_invoice":
            case "ship_ticket":
                return 1; // 交通
            default:
                return 4; // 其他（定额发票/小票等无法细分）
        }
    }
}
