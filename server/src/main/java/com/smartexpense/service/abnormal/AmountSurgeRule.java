package com.smartexpense.service.abnormal;

import com.smartexpense.entity.AbnormalRecord;
import com.smartexpense.mapper.AbnormalQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A003 金额突增：本月已通过/已打款的报销总额超过上月的 150%。
 */
@Component
@RequiredArgsConstructor
public class AmountSurgeRule implements AbnormalRule {

    private final AbnormalQueryMapper queryMapper;

    @Override
    public String code() {
        return "A003";
    }

    @Override
    public String name() {
        return "金额突增";
    }

    @Override
    public List<AbnormalRecord> check(LocalDateTime start, LocalDateTime end) {
        YearMonth month = YearMonth.from(start);
        LocalDateTime monthStart = month.atDay(1).atStartOfDay();
        LocalDateTime prevMonthStart = month.minusMonths(1).atDay(1).atStartOfDay();

        List<AbnormalRecord> records = new ArrayList<>();
        for (Map<String, Object> row : queryMapper.amountSurgeUsers(monthStart, prevMonthStart)) {
            Long userId = Long.valueOf(String.valueOf(row.get("userId")));
            BigDecimal cur = new BigDecimal(String.valueOf(row.get("curTotal")));
            BigDecimal prev = new BigDecimal(String.valueOf(row.get("prevTotal")));
            BigDecimal rate = cur.divide(prev, 2, RoundingMode.HALF_UP);

            AbnormalRecord r = new AbnormalRecord();
            r.setRuleCode(code());
            r.setRuleName(name());
            r.setBizKey(userId + "|" + month.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            r.setUserId(userId);
            r.setMessage("本月报销总额 ¥" + cur.stripTrailingZeros().toPlainString()
                    + " 为上月 ¥" + prev.stripTrailingZeros().toPlainString()
                    + " 的 " + rate.stripTrailingZeros().toPlainString() + " 倍，超过 1.5 倍阈值");
            records.add(r);
        }
        return records;
    }
}
