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
 * A002 日期异常：发票开票日期不在关联出差申请的行程范围内。
 */
@Component
@RequiredArgsConstructor
public class InvoiceDateOutOfTripRangeRule implements AbnormalRule {

    private final AbnormalQueryMapper queryMapper;

    @Override
    public String code() {
        return "A002";
    }

    @Override
    public String name() {
        return "发票日期异常";
    }

    @Override
    public List<AbnormalRecord> check(LocalDateTime start, LocalDateTime end) {
        List<AbnormalRecord> records = new ArrayList<>();
        for (Map<String, Object> row : queryMapper.invoiceDateOutOfTripRange(start, end)) {
            Long invoiceId = toLong(row.get("invoiceId"));
            Long userId = toLong(row.get("userId"));
            String invoiceNo = String.valueOf(row.get("invoiceNo"));
            String tripNo = String.valueOf(row.get("tripNo"));

            AbnormalRecord r = new AbnormalRecord();
            r.setRuleCode(code());
            r.setRuleName(name());
            r.setBizKey("inv-" + invoiceId);
            r.setInvoiceId(invoiceId);
            r.setUserId(userId);
            r.setMessage("发票 " + invoiceNo + " 开票日期(" + row.get("invoiceDate") + ")不在出差单 "
                    + tripNo + " 的行程范围(" + row.get("startDate") + " ~ " + row.get("endDate") + ")内");
            records.add(r);
        }
        return records;
    }

    private Long toLong(Object o) {
        return o == null ? null : Long.valueOf(String.valueOf(o));
    }
}
