package com.example.ecommerce.module.stat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ecommerce.common.PageResult;
import com.example.ecommerce.module.stat.entity.StatBasicMetrics;
import com.example.ecommerce.module.stat.entity.StatFunnelStep;
import com.example.ecommerce.module.stat.entity.StatMultiDimension;
import com.example.ecommerce.module.stat.entity.StatUserComparison;
import com.example.ecommerce.module.stat.mapper.StatBasicMetricsMapper;
import com.example.ecommerce.module.stat.mapper.StatFunnelStepMapper;
import com.example.ecommerce.module.stat.mapper.StatMultiDimensionMapper;
import com.example.ecommerce.module.stat.mapper.StatUserComparisonMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计分析服务类
 */
@Service
public class StatService extends ServiceImpl<StatBasicMetricsMapper, StatBasicMetrics> {

    @Resource
    private StatMultiDimensionMapper multiDimensionMapper;

    @Resource
    private StatFunnelStepMapper funnelStepMapper;

    @Resource
    private StatUserComparisonMapper userComparisonMapper;

    // ==================== 基础指标 ====================

    /** 最新一天汇总（用于看板顶部卡片） */
    public Map<String, Object> getLatestBasicMetrics() {
        LambdaQueryWrapper<StatBasicMetrics> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(StatBasicMetrics::getStatDate).last("LIMIT 1");
        StatBasicMetrics latest = this.getOne(wrapper);

        Map<String, Object> result = new LinkedHashMap<>();
        if (latest != null) {
            result.put("pv", latest.getPv());
            result.put("uv", latest.getUv());
            result.put("newUserCount", latest.getNewUserCount());
            result.put("oldUserCount", latest.getOldUserCount());
            result.put("newUserRatio", latest.getNewUserRatio());
            result.put("avgPages", latest.getAvgPages());
            result.put("statDate", latest.getStatDate());
            result.put("conversionRate", getLatestOverallConversionRate());
        }
        return result;
    }

    private BigDecimal getLatestOverallConversionRate() {
        LambdaQueryWrapper<StatFunnelStep> w = new LambdaQueryWrapper<>();
        w.eq(StatFunnelStep::getFilterType, "ALL")
         .orderByDesc(StatFunnelStep::getStatDate)
         .orderByDesc(StatFunnelStep::getStepOrder)
         .last("LIMIT 1");
        StatFunnelStep last = funnelStepMapper.selectOne(w);
        return last != null ? last.getOverallConversionRate() : BigDecimal.ZERO;
    }

    /** 按日期范围汇总基础指标 */
    public Map<String, Object> getBasicMetrics(String startDate, String endDate) {
        LambdaQueryWrapper<StatBasicMetrics> wrapper = buildDateWrapper(startDate, endDate);
        List<StatBasicMetrics> list = this.list(wrapper);

        long totalPv = list.stream().mapToLong(m -> nvl(m.getPv())).sum();
        long totalUv = list.stream().mapToLong(m -> nvl(m.getUv())).sum();
        double avgPages = list.stream().filter(m -> m.getAvgPages() != null)
                .mapToDouble(m -> m.getAvgPages().doubleValue()).average().orElse(0.0);
        double newUserRatio = list.stream().filter(m -> m.getNewUserRatio() != null)
                .mapToDouble(m -> m.getNewUserRatio().doubleValue()).average().orElse(0.0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalPv", totalPv);
        result.put("totalUv", totalUv);
        result.put("avgPages", String.format("%.2f", avgPages));
        result.put("newUserRatio", String.format("%.4f", newUserRatio));
        result.put("list", list);
        return result;
    }

    /** PV/UV 趋势（按天） */
    public Map<String, Object> getPvUvTrend(String startDate, String endDate) {
        LambdaQueryWrapper<StatBasicMetrics> wrapper = buildDateWrapper(startDate, endDate);
        wrapper.orderByAsc(StatBasicMetrics::getStatDate);
        List<StatBasicMetrics> list = this.list(wrapper);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", list.stream().map(m -> m.getStatDate().toString()).collect(Collectors.toList()));
        result.put("pvData", list.stream().map(m -> nvl(m.getPv())).collect(Collectors.toList()));
        result.put("uvData", list.stream().map(m -> nvl(m.getUv())).collect(Collectors.toList()));
        return result;
    }

    /** 最近 N 天历史指标列表 */
    public List<StatBasicMetrics> getBasicMetricsHistory(Integer days) {
        if (days == null || days <= 0) days = 30;
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);
        LambdaQueryWrapper<StatBasicMetrics> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(StatBasicMetrics::getStatDate, start)
               .le(StatBasicMetrics::getStatDate, end)
               .orderByAsc(StatBasicMetrics::getStatDate);
        return this.list(wrapper);
    }

    // ==================== 多维度分析 ====================

    /** 分页查询多维度统计（admin 表格用） */
    public PageResult<StatMultiDimension> getMultiDimensionPage(
            Integer pageNum, Integer pageSize, String dimension, String statDate) {
        LambdaQueryWrapper<StatMultiDimension> wrapper = buildMultiDimWrapper(dimension, statDate);
        wrapper.orderByDesc(StatMultiDimension::getUserCount);
        Page<StatMultiDimension> page = new Page<>(pageNum, pageSize);
        multiDimensionMapper.selectPage(page, wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    /** 全量查询多维度（图表用） */
    public List<StatMultiDimension> getMultiDimensionAll(String dimension, String statDate) {
        LambdaQueryWrapper<StatMultiDimension> wrapper = buildMultiDimWrapper(dimension, statDate);
        wrapper.orderByDesc(StatMultiDimension::getUserCount);
        return multiDimensionMapper.selectList(wrapper);
    }

    private LambdaQueryWrapper<StatMultiDimension> buildMultiDimWrapper(String dimension, String statDate) {
        LambdaQueryWrapper<StatMultiDimension> wrapper = new LambdaQueryWrapper<>();
        if (dimension != null && !dimension.isBlank())
            wrapper.eq(StatMultiDimension::getDimensionType, dimension);
        if (statDate != null && !statDate.isBlank()) {
            wrapper.eq(StatMultiDimension::getStatDate, LocalDate.parse(statDate));
        } else {
            LocalDate latest = getLatestMultiDimDate(dimension);
            if (latest != null) wrapper.eq(StatMultiDimension::getStatDate, latest);
        }
        return wrapper;
    }

    private LocalDate getLatestMultiDimDate(String dimension) {
        LambdaQueryWrapper<StatMultiDimension> w = new LambdaQueryWrapper<>();
        if (dimension != null && !dimension.isBlank())
            w.eq(StatMultiDimension::getDimensionType, dimension);
        w.orderByDesc(StatMultiDimension::getStatDate).last("LIMIT 1");
        StatMultiDimension one = multiDimensionMapper.selectOne(w);
        return one != null ? one.getStatDate() : null;
    }

    // ==================== 转化漏斗 ====================

    /** 获取漏斗步骤（默认 ALL，最新日期） */
    public List<StatFunnelStep> getConversionFunnel(String filterType, String statDate) {
        LambdaQueryWrapper<StatFunnelStep> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StatFunnelStep::getFilterType, filterType != null ? filterType : "ALL");
        if (statDate != null && !statDate.isBlank()) {
            wrapper.eq(StatFunnelStep::getStatDate, LocalDate.parse(statDate));
        } else {
            LocalDate latest = getLatestFunnelDate();
            if (latest != null) wrapper.eq(StatFunnelStep::getStatDate, latest);
        }
        wrapper.orderByAsc(StatFunnelStep::getStepOrder);
        return funnelStepMapper.selectList(wrapper);
    }

    private LocalDate getLatestFunnelDate() {
        LambdaQueryWrapper<StatFunnelStep> w = new LambdaQueryWrapper<>();
        w.eq(StatFunnelStep::getFilterType, "ALL")
         .orderByDesc(StatFunnelStep::getStatDate).last("LIMIT 1");
        StatFunnelStep one = funnelStepMapper.selectOne(w);
        return one != null ? one.getStatDate() : null;
    }

    // ==================== 新老用户对比 ====================

    /** 新老用户对比，按 userType 分组后返回 Map */
    public Map<String, Object> getUserComparison(String statDate) {
        List<StatUserComparison> list = getUserComparisonList(statDate);
        Map<String, Object> result = new LinkedHashMap<>();
        list.stream().collect(Collectors.groupingBy(StatUserComparison::getUserType))
            .forEach((type, items) -> {
                Map<String, Object> group = new LinkedHashMap<>();
                items.forEach(item -> group.put(item.getMetricName(), item.getMetricValue()));
                result.put(type, group);
            });
        return result;
    }

    /** 新老用户对比原始列表 */
    public List<StatUserComparison> getUserComparisonList(String statDate) {
        LambdaQueryWrapper<StatUserComparison> wrapper = new LambdaQueryWrapper<>();
        if (statDate != null && !statDate.isBlank()) {
            wrapper.eq(StatUserComparison::getStatDate, LocalDate.parse(statDate));
        } else {
            LocalDate latest = getLatestComparisonDate();
            if (latest != null) wrapper.eq(StatUserComparison::getStatDate, latest);
        }
        return userComparisonMapper.selectList(wrapper);
    }

    private LocalDate getLatestComparisonDate() {
        LambdaQueryWrapper<StatUserComparison> w = new LambdaQueryWrapper<>();
        w.orderByDesc(StatUserComparison::getStatDate).last("LIMIT 1");
        StatUserComparison one = userComparisonMapper.selectOne(w);
        return one != null ? one.getStatDate() : null;
    }

    // ==================== 系统概览 ====================

    public Map<String, Object> getStatInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("basicMetricsCount", this.count());
        info.put("multiDimensionCount", multiDimensionMapper.selectCount(null));
        info.put("funnelStepCount", funnelStepMapper.selectCount(null));
        info.put("userComparisonCount", userComparisonMapper.selectCount(null));
        info.put("latestStatDate", getLatestFunnelDate());
        return info;
    }

    // ==================== 工具方法 ====================

    private LambdaQueryWrapper<StatBasicMetrics> buildDateWrapper(String startDate, String endDate) {
        LambdaQueryWrapper<StatBasicMetrics> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isBlank())
            wrapper.ge(StatBasicMetrics::getStatDate, LocalDate.parse(startDate));
        if (endDate != null && !endDate.isBlank())
            wrapper.le(StatBasicMetrics::getStatDate, LocalDate.parse(endDate));
        return wrapper;
    }

    private long nvl(Long val) { return val != null ? val : 0L; }
}
