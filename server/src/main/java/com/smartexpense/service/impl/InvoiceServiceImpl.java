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
import com.smartexpense.entity.Trip;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.InvoiceMapper;
import com.smartexpense.mapper.ReimbursementMapper;
import com.smartexpense.mapper.SysUserMapper;
import com.smartexpense.mapper.TripMapper;
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
    private final TripMapper tripMapper;
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

        // 关联出差单时校验：与报销单一致——存在、属于当前用户、且为草稿
        if (tripId != null) {
            Trip trip = tripMapper.selectById(tripId);
            if (trip == null) {
                throw new BusinessException("关联的出差申请不存在");
            }
            SysUser current = userMapper.selectById(StpUtil.getLoginIdAsLong());
            if (current.getRole() != 4 && !trip.getUserId().equals(current.getId())) {
                throw new BusinessException("只能向自己的出差单上传发票");
            }
            if (trip.getStatus() != 0) {
                throw new BusinessException("只有草稿状态的出差单才能上传发票");
            }
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        if (suffix == null || suffix.isEmpty()) {
            suffix = "jpg";
        }
        // 扩展名白名单：阻止 HTML/SVG 等可执行文件上传后在应用同源下渲染（存储型 XSS）
        if (!List.of("jpg", "jpeg", "png", "pdf").contains(suffix.toLowerCase())) {
            throw new BusinessException("仅支持 JPG/PNG/PDF 格式的发票文件");
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

        // 关联报销单必须为草稿：提交/审批后不允许改金额，防止汇总与明细不一致
        if (invoice.getReimbursementId() != null) {
            Reimbursement reimb = reimbursementMapper.selectById(invoice.getReimbursementId());
            if (reimb == null || reimb.getStatus() != 0) {
                throw new BusinessException("只有草稿状态的报销单才能修改发票");
            }
        }

        // 金额合法性校验（前端 min=0 只是交互提示，服务端必须兜底）
        if (updateData.getAmount() != null && updateData.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("发票金额必须大于0");
        }

        // 同一报销单内发票号唯一：防止同一张发票重复录入导致报销金额翻倍
        String finalInvoiceNo = updateData.getInvoiceNo() != null ? updateData.getInvoiceNo() : invoice.getInvoiceNo();
        if (finalInvoiceNo != null && !finalInvoiceNo.isEmpty() && invoice.getReimbursementId() != null) {
            Long dup = invoiceMapper.selectCount(new LambdaQueryWrapper<Invoice>()
                    .eq(Invoice::getReimbursementId, invoice.getReimbursementId())
                    .eq(Invoice::getInvoiceNo, finalInvoiceNo)
                    .ne(Invoice::getId, invoiceId));
            if (dup != null && dup > 0) {
                throw new BusinessException("该发票号已存在于本报销单中，请勿重复录入");
            }
        }

        // 人工修正OCR结果：仅覆盖前端传过来的非空字段，防止清空 OCR 已识别的税额/发票代码等
        if (updateData.getInvoiceNo() != null) {
            invoice.setInvoiceNo(updateData.getInvoiceNo());
        }
        if (updateData.getInvoiceCode() != null) {
            invoice.setInvoiceCode(updateData.getInvoiceCode());
        }
        if (updateData.getAmount() != null) {
            invoice.setAmount(updateData.getAmount());
        }
        if (updateData.getTaxAmount() != null) {
            invoice.setTaxAmount(updateData.getTaxAmount());
        }
        if (updateData.getInvoiceDate() != null) {
            invoice.setInvoiceDate(updateData.getInvoiceDate());
        }
        if (updateData.getType() != null) {
            invoice.setType(updateData.getType());
        }
        if (updateData.getSellerName() != null) {
            invoice.setSellerName(updateData.getSellerName());
        }
        if (updateData.getBuyerName() != null) {
            invoice.setBuyerName(updateData.getBuyerName());
        }
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
            // deptId 来自当前登录用户的数据库记录，非请求输入，无注入风险；
            // 如未来改为请求参数，必须换参数化写法
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
    public void markOcrFailed(Long invoiceId) {
        Invoice invoice = invoiceMapper.selectById(invoiceId);
        // 仅当发票仍处于"识别中"时落为失败，避免覆盖后续人工操作
        if (invoice != null && invoice.getOcrStatus() != null && invoice.getOcrStatus() == 0) {
            invoice.setOcrStatus(2);
            updateById(invoice);
            log.info("OCR识别失败状态已标记, id: {}", invoiceId);
        }
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
        // 与 confirm 一致：报销单离开草稿后不允许删发票，防止汇总金额与明细不一致
        if (invoice.getReimbursementId() != null) {
            Reimbursement reimb = reimbursementMapper.selectById(invoice.getReimbursementId());
            if (reimb == null || reimb.getStatus() != 0) {
                throw new BusinessException("只有草稿状态的报销单才能删除发票");
            }
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
