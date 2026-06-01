package com.smartexpense.service;

import com.smartexpense.entity.SysDept;

import java.util.List;

public interface DeptService {

    List<SysDept> list();

    List<SysDept> tree();

    SysDept create(SysDept dept);

    SysDept update(SysDept dept);

    void delete(Long id);
}
