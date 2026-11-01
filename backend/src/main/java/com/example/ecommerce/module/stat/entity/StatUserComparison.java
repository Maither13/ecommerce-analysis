package com.example.ecommerce.module.stat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stat_user_comparison")
public class StatUserComparison implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户类型: new / old */
    private String userType;

    /** 指标类别: basic / pages_preference / source / device */
    private String metricCategory;

    private String metricName;
    private BigDecimal metricValue;
    private String metricUnit;
    private LocalDate statDate;
    private String jobId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
