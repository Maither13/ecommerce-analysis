package com.example.ecommerce.module.task.controller;

import com.example.ecommerce.common.PageResult;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.module.task.entity.MrJobLog;
import com.example.ecommerce.module.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 任务管理控制器
 */
@Tag(name = "任务管理", description = "MapReduce任务的提交、查询、管理等接口")
@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Resource
    private TaskService taskService;

    /**
     * 提交任务
     */
    @Operation(summary = "提交任务", description = "提交新的MapReduce分析任务")
    @PostMapping
    public Result<Long> submitTask(@RequestBody MrJobLog task) {
        Long taskId = taskService.submitTask(task);
        return Result.success(taskId);
    }

    /**
     * 获取任务列表
     */
    @Operation(summary = "获取任务列表", description = "分页查询任务列表,支持状态筛选和关键词搜索")
    @GetMapping
    public Result<PageResult<MrJobLog>> getTaskList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        PageResult<MrJobLog> result = taskService.getTaskList(pageNum, pageSize, status, keyword);
        return Result.success(result);
    }

    /**
     * 获取任务详情
     */
    @Operation(summary = "获取任务详情", description = "根据ID获取任务详细信息")
    @GetMapping("/{id}")
    public Result<MrJobLog> getTaskDetail(@PathVariable Long id) {
        MrJobLog task = taskService.getTaskDetail(id);
        return Result.success(task);
    }

    /**
     * 删除任务
     */
    @Operation(summary = "删除任务", description = "删除指定任务")
    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return Result.success(null);
    }

    /**
     * 重试任务
     */
    @Operation(summary = "重试任务", description = "重新执行失败或已完成的任务")
    @PostMapping("/{id}/retry")
    public Result<Void> retryTask(@PathVariable Long id) {
        taskService.retryTask(id);
        return Result.success(null);
    }
}
