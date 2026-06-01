package com.smartexpense.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartexpense.entity.SysDept;
import com.smartexpense.entity.SysUser;
import com.smartexpense.exception.BusinessException;
import com.smartexpense.mapper.SysDeptMapper;
import com.smartexpense.mapper.SysUserMapper;
import com.smartexpense.service.DeptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements DeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<SysDept> list() {
        // 删除即禁用(status=0)，默认列表只返回启用部门
        return deptMapper.selectList(
            new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getStatus, 1)
                .orderByAsc(SysDept::getSortOrder));
    }

    @Override
    public List<SysDept> tree() {
        return list();
    }

    @Override
    @Transactional
    public SysDept create(SysDept dept) {
        if (dept.getStatus() == null) {
            dept.setStatus(1);
        }
        dept.setCreateTime(LocalDateTime.now());
        save(dept);
        log.info("部门创建成功, id: {}, name: {}", dept.getId(), dept.getDeptName());
        return dept;
    }

    @Override
    @Transactional
    public SysDept update(SysDept dept) {
        if (getById(dept.getId()) == null) {
            throw new BusinessException("部门不存在");
        }
        updateById(dept);
        log.info("部门更新成功, id: {}", dept.getId());
        return dept;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (getById(id) == null) {
            throw new BusinessException("部门不存在");
        }
        Long childCount = deptMapper.selectCount(
            new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("存在子部门，请先删除子部门");
        }
        Long userCount = userMapper.selectCount(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (userCount > 0) {
            throw new BusinessException("部门下存在用户，请先移除用户");
        }
        SysDept existing = getById(id);
        existing.setStatus(0); // 软删除
        updateById(existing);
        log.info("部门删除成功, id: {}", id);
    }
}
