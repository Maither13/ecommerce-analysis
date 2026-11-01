package com.example.ecommerce.module.stat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stat_multi_dimension")
public class StatMultiDimension implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 维度类型: age / sex / market / device / os / source */
    private String dimensionType;

    /** 维度值 */
    private String dimensionValue;

    private Long userCount;
    private Long pv;
    private BigDecimal avgPages;
    private Long convertedCount;
    private BigDecimal conversionRate;
    private LocalDate statDate;
    private String jobId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
