package com.example.ecommerce.module.stat.controller;

import com.example.ecommerce.common.PageResult;
import com.example.ecommerce.common.Result;
import com.example.ecommerce.module.stat.entity.StatBasicMetrics;
import com.example.ecommerce.module.stat.entity.StatFunnelStep;
import com.example.ecommerce.module.stat.entity.StatMultiDimension;
import com.example.ecommerce.module.stat.entity.StatUserComparison;
import com.example.ecommerce.module.stat.service.StatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统计分析控制器
 *
 * 所有路径均以 /api/stats 为前缀（context-path=/api）
 *
 * ── 基础指标 ──────────────────────────────────────────
 *   GET  /api/stats/basic/latest          最新一天汇总（看板卡片）
 *   GET  /api/stats/basic/trend           PV/UV 趋势（?startDate&endDate）
 *   GET  /api/stats/basic/history         近 N 天列表（?days=30）
 *   GET  /api/stats/basic-metrics         按范围汇总（?startDate&endDate）
 *
 * ── 多维度 ────────────────────────────────────────────
 *   GET  /api/stats/multi-dimension       分页（?dimension&statDate&pageNum&pageSize）
 *   GET  /api/stats/multi-dimension/all   全量图表数据（?dimension&statDate）
 *
 * ── 转化漏斗 ──────────────────────────────────────────
 *   GET  /api/stats/funnel                (?filterType=ALL&statDate)
 *
 * ── 新老用户对比 ──────────────────────────────────────
 *   GET  /api/stats/comparison            分组 Map
 *   GET  /api/stats/comparison/list       原始列表（?statDate）
 *
 * ── 系统 ──────────────────────────────────────────────
 *   GET  /api/stats/info                  各表数据量统计
 */
@Tag(name = "统计分析", description = "基础指标、多维度分析、转化漏斗、新老用户对比")
@RestController
@RequestMapping("/stats")
public class StatController {

    @Resource
    private StatService statService;

    // =================== 基础指标 ===================

    @Operation(summary = "最新一天汇总指标")
    @GetMapping("/basic/latest")
    public Result<Map<String, Object>> getLatestBasicMetrics() {
        return Result.success(statService.getLatestBasicMetrics());
    }

    @Operation(summary = "PV/UV 趋势数据")
    @GetMapping("/basic/trend")
    public Result<Map<String, Object>> getPvUvTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(statService.getPvUvTrend(startDate, endDate));
    }

    @Operation(summary = "近 N 天历史基础指标列表")
    @GetMapping("/basic/history")
    public Result<List<StatBasicMetrics>> getBasicMetricsHistory(
            @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(statService.getBasicMetricsHistory(days));
    }

    @Operation(summary = "按日期范围汇总基础指标")
    @GetMapping("/basic-metrics")
    public Result<Map<String, Object>> getBasicMetrics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(statService.getBasicMetrics(startDate, endDate));
    }

    // =================== 多维度分析 ===================

    @Operation(summary = "多维度统计（分页，admin 表格）",
               description = "dimension 可选: age / sex / market / device / os / source")
    @GetMapping("/multi-dimension")
    public Result<PageResult<StatMultiDimension>> getMultiDimensionPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String dimension,
            @RequestParam(required = false) String statDate) {
        return Result.success(statService.getMultiDimensionPage(pageNum, pageSize, dimension, statDate));
    }

    @Operation(summary = "多维度全量数据（图表）")
    @GetMapping("/multi-dimension/all")
    public Result<List<StatMultiDimension>> getMultiDimensionAll(
            @RequestParam(required = false) String dimension,
            @RequestParam(required = false) String statDate) {
        return Result.success(statService.getMultiDimensionAll(dimension, statDate));
    }

    // =================== 转化漏斗 ===================

    @Operation(summary = "转化漏斗步骤",
               description = "filterType: ALL / new_user / source / device；默认 ALL")
    @GetMapping("/funnel")
    public Result<List<StatFunnelStep>> getConversionFunnel(
            @RequestParam(required = false) String filterType,
            @RequestParam(required = false) String statDate) {
        return Result.success(statService.getConversionFunnel(filterType, statDate));
    }

    // =================== 新老用户对比 ===================

    @Operation(summary = "新老用户对比（分组 Map）")
    @GetMapping("/comparison")
    public Result<Map<String, Object>> getUserComparison(
            @RequestParam(required = false) String statDate) {
        return Result.success(statService.getUserComparison(statDate));
    }

    @Operation(summary = "新老用户对比原始列表")
    @GetMapping("/comparison/list")
    public Result<List<StatUserComparison>> getUserComparisonList(
            @RequestParam(required = false) String statDate) {
        return Result.success(statService.getUserComparisonList(statDate));
    }

    // =================== 系统 ===================

    @Operation(summary = "统计数据概览（各表行数）")
    @GetMapping("/info")
    public Result<Map<String, Object>> getStatInfo() {
        return Result.success(statService.getStatInfo());
    }
}
