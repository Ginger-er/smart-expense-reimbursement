package com.smartexpense.service.abnormal;

import com.smartexpense.entity.AbnormalRecord;
import com.smartexpense.mapper.AbnormalQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A001 重复发票：同一张发票（同发票号）出现在多个不同报销单中，疑似重复报销。
 */
@Component
@RequiredArgsConstructor
public class DuplicateInvoiceRule implements AbnormalRule {

    private final AbnormalQueryMapper queryMapper;

    @Override
    public String code() {
        return "A001";
    }

    @Override
    public String name() {
        return "重复发票";
    }

    @Override
    public List<AbnormalRecord> check(LocalDateTime start, LocalDateTime end) {
        List<AbnormalRecord> records = new ArrayList<>();
        for (Map<String, Object> row : queryMapper.duplicateInvoiceNos(start, end)) {
            String invoiceNo = String.valueOf(row.get("invoiceNo"));
            Long userId = toLong(row.get("userId"));
            String reimbIds = String.valueOf(row.get("reimbursementIds"));

            AbnormalRecord r = new AbnormalRecord();
            r.setRuleCode(code());
            r.setRuleName(name());
            r.setBizKey(invoiceNo + "|" + userId);
            r.setUserId(userId);
            r.setMessage("发票号 " + invoiceNo + " 同时出现在多个报销单(" + reimbIds + ")中，疑似重复报销");
            records.add(r);
        }
        return records;
    }

    private Long toLong(Object o) {
        return o == null ? null : Long.valueOf(String.valueOf(o));
    }
}
