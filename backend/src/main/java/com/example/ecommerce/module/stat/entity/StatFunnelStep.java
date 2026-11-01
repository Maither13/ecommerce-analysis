package com.example.ecommerce.module.stat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stat_funnel_step")
public class StatFunnelStep implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 步骤名称: home_page / listing_page / product_page / payment_page / confirmation_page */
    private String stepName;

    private Integer stepOrder;
    private Long userCount;
    private BigDecimal conversionRate;
    private BigDecimal lossRate;
    private BigDecimal overallConversionRate;

    /** 筛选类型: new_user / source / device / ALL */
    private String filterType;
    private String filterValue;

    private LocalDate statDate;
    private String jobId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
