package com.example.ecommerce.module.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("mr_job_log")
public class MrJobLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID(唯一)
     */
    private String jobId;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务类型(basic/multi_dimension/funnel/comparison)
     */
    private String jobType;

    /**
     * 提交用户ID(NULL表示系统任务)
     */
    private Long userId;

    /**
     * 状态(PENDING/RUNNING/SUCCESS/FAILED/CANCELLED)
     */
    private String status;

    /**
     * 输入路径(HDFS)
     */
    private String inputPath;

    /**
     * 输出路径(HDFS)
     */
    private String outputPath;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 耗时(秒)
     */
    private Long duration;

    /**
     * 进度(0-100)
     */
    private Integer progress;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
