package com.example.ecommerce.module.stat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stat_basic_metrics")
public class StatBasicMetrics implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 统计日期
     */
    private LocalDate statDate;

    /**
     * 页面浏览量
     */
    private Long pv;

    /**
     * 独立用户数
     */
    private Long uv;

    /**
     * 新用户数
     */
    private Long newUserCount;

    /**
     * 老用户数
     */
    private Long oldUserCount;

    /**
     * 新用户占比
     */
    private BigDecimal newUserRatio;

    /**
     * 人均访问页数
     */
    private BigDecimal avgPages;

    /**
     * 总访问页数
     */
    private Long totalPagesSum;

    /**
     * 最大访问页数
     */
    private Integer maxPages;

    /**
     * 最小访问页数
     */
    private Integer minPages;

    /**
     * 关联任务ID
     */
    private String jobId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
