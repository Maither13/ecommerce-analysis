package com.example.ecommerce.module.data.controller;

import com.example.ecommerce.common.PageResult;
import com.example.ecommerce.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 数据管理控制器
 */
@Tag(name = "数据管理", description = "原始数据查询、导出等接口")
@RestController
@RequestMapping("/data")
public class DataController {

    /**
     * 获取原始数据列表
     */
    @Operation(summary = "获取原始数据列表", description = "分页查询用户行为原始数据")
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> getDataList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String eventType) {
        
        // 模拟数据
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", (pageNum - 1) * pageSize + i + 1);
            record.put("userId", "user_" + (i + 1));
            record.put("eventType", eventType != null ? eventType : getRandomEventType());
            record.put("pageUrl", "/product/" + (i + 1));
            record.put("timestamp", "2026-06-15 10:" + String.format("%02d", i) + ":00");
            record.put("deviceType", getRandomDeviceType());
            record.put("browser", getRandomBrowser());
            record.put("ipAddress", "192.168.1." + (i + 1));
            records.add(record);
        }

        PageResult<Map<String, Object>> result = PageResult.of(1000L, records);
        return Result.success(result);
    }

    /**
     * 导出数据
     */
    @Operation(summary = "导出数据", description = "导出指定条件的数据为CSV或Excel文件")
    @PostMapping("/export")
    public Result<String> exportData(@RequestBody Map<String, Object> params) {
        String format = (String) params.getOrDefault("format", "csv");
        String startDate = (String) params.get("startDate");
        String endDate = (String) params.get("endDate");
        String eventType = (String) params.get("eventType");

        // 生成文件名
        String fileName = "export_" + System.currentTimeMillis() + "." + format;

        // 这里简化实现,实际应该生成真实文件
        return Result.success(fileName);
    }

    /**
     * 获取数据统计信息
     */
    @Operation(summary = "获取数据统计信息", description = "获取数据总量、今日新增等统计信息")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", 1000000L);
        stats.put("todayCount", 5000L);
        stats.put("yesterdayCount", 4800L);
        stats.put("growthRate", "4.17%");

        return Result.success(stats);
    }

    private String getRandomEventType() {
        String[] types = {"page_view", "click", "add_cart", "order", "payment"};
        return types[new Random().nextInt(types.length)];
    }

    private String getRandomDeviceType() {
        String[] devices = {"PC", "Mobile", "Tablet"};
        return devices[new Random().nextInt(devices.length)];
    }

    private String getRandomBrowser() {
        String[] browsers = {"Chrome", "Firefox", "Safari", "Edge"};
        return browsers[new Random().nextInt(browsers.length)];
    }
}
