package com.example.ecommerce.module.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ecommerce.common.BusinessException;
import com.example.ecommerce.common.PageResult;
import com.example.ecommerce.integration.hadoop.MrIntegrationService;
import com.example.ecommerce.module.task.entity.MrJobLog;
import com.example.ecommerce.module.task.mapper.MrJobLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 任务服务类
 */
@Service
public class TaskService extends ServiceImpl<MrJobLogMapper, MrJobLog> {

    /** 用于异步执行 MapReduce 任务的线程池 */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    @Autowired
    private MrIntegrationService mrIntegrationService;

    /**
     * 提交MapReduce任务
     */
    public Long submitTask(MrJobLog task) {
        String jobId = "JOB_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
        task.setJobId(jobId);
        task.setStatus("PENDING");
        task.setSubmitTime(LocalDateTime.now());
        task.setProgress(0);
        // 若未传 outputPath，根据 jobType 自动生成
        if (task.getOutputPath() == null || task.getOutputPath().isEmpty()) {
            String suffix = task.getJobType() != null ? task.getJobType() : "unknown";
            task.setOutputPath("/output/" + suffix + "_" + System.currentTimeMillis());
        }
        this.save(task);
        // 真实 MR 类型走实际执行，其他走模拟
        if (MrIntegrationService.isRealMrJob(task.getJobType())) {
            Long finalId = task.getId();
            scheduler.schedule(() -> mrIntegrationService.executeJob(finalId), 1, TimeUnit.SECONDS);
        } else {
            simulateExecution(task.getId());
        }
        return task.getId();
    }

    /**
     * 分页查询任务列表
     */
    public PageResult<MrJobLog> getTaskList(Integer pageNum, Integer pageSize, String status, String keyword) {
        Page<MrJobLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MrJobLog> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            wrapper.eq(MrJobLog::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(MrJobLog::getJobName, keyword);
        }
        wrapper.orderByDesc(MrJobLog::getCreateTime);

        Page<MrJobLog> result = this.page(page, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /**
     * 获取任务详情
     */
    public MrJobLog getTaskDetail(Long id) {
        MrJobLog task = this.getById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        return task;
    }

    /**
     * 删除任务
     */
    public void deleteTask(Long id) {
        MrJobLog task = this.getById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if ("RUNNING".equals(task.getStatus())) {
            throw new BusinessException("正在执行的任务不能删除");
        }
        this.removeById(id);
    }

    /**
     * 重试任务 —— 重置为 PENDING 并触发新一轮模拟执行
     */
    public void retryTask(Long id) {
        MrJobLog task = this.getById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (!"SUCCESS".equals(task.getStatus()) && !"FAILED".equals(task.getStatus())) {
            throw new BusinessException("只有失败或已完成的任务可以重试");
        }
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setErrorMsg(null);
        task.setStartTime(null);
        task.setEndTime(null);
        task.setDuration(null);
        this.updateById(task);
        if (MrIntegrationService.isRealMrJob(task.getJobType())) {
            Long finalId = id;
            scheduler.schedule(() -> mrIntegrationService.executeJob(finalId), 1, TimeUnit.SECONDS);
        } else {
            simulateExecution(id);
        }
    }

    /**
     * 更新任务状态（供外部手动调用）
     */
    public void updateTaskStatus(Long id, String status, String errorMsg) {
        MrJobLog task = this.getById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        task.setStatus(status);
        if ("RUNNING".equals(status)) {
            task.setStartTime(LocalDateTime.now());
            task.setProgress(10);
        } else if ("SUCCESS".equals(status)) {
            task.setEndTime(LocalDateTime.now());
            task.setProgress(100);
            if (task.getStartTime() != null) {
                task.setDuration(Duration.between(task.getStartTime(), task.getEndTime()).getSeconds());
            }
        } else if ("FAILED".equals(status)) {
            task.setEndTime(LocalDateTime.now());
            task.setErrorMsg(errorMsg);
        }
        this.updateById(task);
    }

    // ==================== 模拟执行 ====================

    /**
     * 模拟 MapReduce 异步执行：PENDING → RUNNING（进度递增）→ SUCCESS
     * 总耗时约 17 秒，分 5 个阶段推进进度。
     */
    private void simulateExecution(Long taskId) {
        scheduler.schedule(() -> applyProgress(taskId, "RUNNING", 10,  true,  false), 2,  TimeUnit.SECONDS);
        scheduler.schedule(() -> applyProgress(taskId, null,      40,  false, false), 6,  TimeUnit.SECONDS);
        scheduler.schedule(() -> applyProgress(taskId, null,      70,  false, false), 10, TimeUnit.SECONDS);
        scheduler.schedule(() -> applyProgress(taskId, null,      90,  false, false), 14, TimeUnit.SECONDS);
        scheduler.schedule(() -> applyProgress(taskId, "SUCCESS", 100, false, true),  17, TimeUnit.SECONDS);
    }

    private void applyProgress(Long taskId, String newStatus, int progress,
                               boolean isStart, boolean isEnd) {
        try {
            MrJobLog task = this.getById(taskId);
            if (task == null) return;
            // 若任务已被取消或失败，不再推进
            if ("FAILED".equals(task.getStatus()) || "CANCELLED".equals(task.getStatus())) return;

            task.setProgress(progress);
            if (newStatus != null) {
                task.setStatus(newStatus);
            }
            if (isStart) {
                task.setStartTime(LocalDateTime.now());
            }
            if (isEnd) {
                LocalDateTime now = LocalDateTime.now();
                task.setEndTime(now);
                if (task.getStartTime() != null) {
                    task.setDuration(Duration.between(task.getStartTime(), now).getSeconds());
                }
            }
            this.updateById(task);
        } catch (Exception e) {
            // 任务可能已被删除，静默忽略
        }
    }
}
