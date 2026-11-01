package com.example.ecommerce.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 每页大小
     */
    private Long size;

    /**
     * 当前页码
     */
    private Long current;

    /**
     * 总页数
     */
    private Long pages;

    public PageResult() {
    }

    public PageResult(List<T> records, Long total, Long size, Long current) {
        this.records = records;
        this.total = total;
        this.size = size;
        this.current = current;
        this.pages = (size == 0) ? 0L : (total + size - 1) / size;
    }

    /**
     * 从 MyBatis Plus IPage 转换
     */
    public static <T> PageResult<T> of(com.baomidou.mybatisplus.core.metadata.IPage<T> page) {
        return new PageResult<>(
                page.getRecords(),
                page.getTotal(),
                page.getSize(),
                page.getCurrent()
        );
    }

    /**
     * 从总数和记录列表创建分页结果
     */
    public static <T> PageResult<T> of(Long total, List<T> records) {
        return new PageResult<>(records, total, (long) records.size(), 1L);
    }
}
