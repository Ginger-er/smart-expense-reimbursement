package com.smartexpense.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PageResult<T> extends Result<List<T>> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Long pageNum;

    /** 每页条数 */
    private Long pageSize;

    public static <T> PageResult<T> success(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        return result;
    }

    public static <T> PageResult<T> success(List<T> list, Long total, Long pageNum, Long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(list);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }
}
