package com.smartexpense.service.abnormal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartexpense.entity.AbnormalRecord;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.AbnormalRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AbnormalServiceImpl implements AbnormalService {

    private final AbnormalRecordMapper recordMapper;
    private final AbnormalScanService scanService;

    @Override
    public Page<AbnormalRecord> page(Integer pageNum, Integer pageSize, Integer handled) {
        LambdaQueryWrapper<AbnormalRecord> wrapper = new LambdaQueryWrapper<>();
        if (handled != null) {
            wrapper.eq(AbnormalRecord::getHandled, handled);
        }
        wrapper.orderByDesc(AbnormalRecord::getCreateTime);
        return recordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional
    public void handle(Long id) {
        AbnormalRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("预警记录不存在");
        }
        record.setHandled(1);
        record.setHandleTime(LocalDateTime.now());
        recordMapper.updateById(record);
        log.info("预警记录已处理, id: {}", id);
    }

    @Override
    public int scan() {
        return scanService.scan(LocalDate.now().minusDays(1));
    }
}
