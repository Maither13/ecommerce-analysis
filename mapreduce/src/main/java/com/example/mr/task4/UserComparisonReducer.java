package com.example.mr.task4;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 新老用户对比 Reducer
 * <p>
 * 功能: 分别聚合新用户和老用户的各项指标,计算对比数据
 *
 * @author Lingma
 * @since 2026-06-09
 */
public class UserComparisonReducer extends Reducer<Text, Text, Text, Text> {

    /**
     * Reduce 方法
     *
     * @param key      "new_user" 或 "old_user"
     * @param values   该类型用户的所有数据
     * @param context  上下文
     */
    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        String userType = key.toString();

        long userCount = 0;           // 用户数
        long totalPagesSum = 0;       // 总访问页数
        int maxPages = 0;
        int minPages = Integer.MAX_VALUE;

        // 页面访问统计
        long homePageCount = 0;
        long listingPageCount = 0;
        long productPageCount = 0;
        long paymentPageCount = 0;
        long confirmationPageCount = 0;

        // 流量来源统计
        Map<String, Long> sourceCount = new HashMap<>();
        // 设备类型统计
        Map<String, Long> deviceCount = new HashMap<>();

        // 遍历所有值进行聚合
        for (Text value : values) {
            String[] parts = value.toString().split(",");
            if (parts.length != 8) {
                continue;
            }

            userCount++;
            int pages = Integer.parseInt(parts[0]);
            String source = parts[1];
            String device = parts[2];

            totalPagesSum += pages;
            maxPages = Math.max(maxPages, pages);
            minPages = Math.min(minPages, pages);

            // 页面访问统计
            if (Integer.parseInt(parts[3]) >= 1) homePageCount++;
            if (Integer.parseInt(parts[4]) >= 1) listingPageCount++;
            if (Integer.parseInt(parts[5]) >= 1) productPageCount++;
            if (Integer.parseInt(parts[6]) >= 1) paymentPageCount++;
            if (Integer.parseInt(parts[7]) >= 1) confirmationPageCount++;

            // 流量来源统计
            sourceCount.put(source, sourceCount.getOrDefault(source, 0L) + 1);
            // 设备类型统计
            deviceCount.put(device, deviceCount.getOrDefault(device, 0L) + 1);
        }

        if (userCount == 0) {
            minPages = 0;
        }

        // 计算基础指标
        double avgPages = userCount > 0 ? (double) totalPagesSum / userCount : 0.0;
        double conversionRate = userCount > 0 ? (double) confirmationPageCount / userCount : 0.0;

        // 构造输出结果
        StringBuilder result = new StringBuilder();
        result.append(String.format("user_type=%s\n", userType));
        result.append(String.format("user_count=%d\n", userCount));
        result.append(String.format("avg_pages=%.2f\n", avgPages));
        result.append(String.format("max_pages=%d\n", maxPages));
        result.append(String.format("min_pages=%d\n", minPages));
        result.append(String.format("total_pages=%d\n", totalPagesSum));
        result.append(String.format("conversion_rate=%.4f\n", conversionRate));
        result.append(String.format("home_page_rate=%.4f\n", userCount > 0 ? (double) homePageCount / userCount : 0.0));
        result.append(String.format("listing_page_rate=%.4f\n", userCount > 0 ? (double) listingPageCount / userCount : 0.0));
        result.append(String.format("product_page_rate=%.4f\n", userCount > 0 ? (double) productPageCount / userCount : 0.0));
        result.append(String.format("payment_page_rate=%.4f\n", userCount > 0 ? (double) paymentPageCount / userCount : 0.0));
        result.append(String.format("confirmation_page_rate=%.4f\n", userCount > 0 ? (double) confirmationPageCount / userCount : 0.0));

        // 流量来源分布
        result.append("source_distribution=");
        for (Map.Entry<String, Long> entry : sourceCount.entrySet()) {
            result.append(String.format("%s:%.4f,", entry.getKey(), (double) entry.getValue() / userCount));
        }
        result.append("\n");

        // 设备类型分布
        result.append("device_distribution=");
        for (Map.Entry<String, Long> entry : deviceCount.entrySet()) {
            result.append(String.format("%s:%.4f,", entry.getKey(), (double) entry.getValue() / userCount));
        }

        context.write(key, new Text(result.toString()));

        // 记录统计信息
        System.out.println("========== " + (userType.equals("new_user") ? "新用户" : "老用户") + " 行为统计 ==========");
        System.out.println("用户数:          " + userCount);
        System.out.println("平均访问页数:    " + String.format("%.2f", avgPages));
        System.out.println("最大访问页数:    " + maxPages);
        System.out.println("最小访问页数:    " + minPages);
        System.out.println("转化率:          " + String.format("%.2f%%", conversionRate * 100));
        System.out.println("首页访问率:      " + String.format("%.2f%%", (userCount > 0 ? (double) homePageCount / userCount : 0.0) * 100));
        System.out.println("列表页访问率:    " + String.format("%.2f%%", (userCount > 0 ? (double) listingPageCount / userCount : 0.0) * 100));
        System.out.println("商品页访问率:    " + String.format("%.2f%%", (userCount > 0 ? (double) productPageCount / userCount : 0.0) * 100));
        System.out.println("支付页访问率:    " + String.format("%.2f%%", (userCount > 0 ? (double) paymentPageCount / userCount : 0.0) * 100));
        System.out.println("确认页访问率:    " + String.format("%.2f%%", (userCount > 0 ? (double) confirmationPageCount / userCount : 0.0) * 100));
        System.out.println("====================================================");
    }
}
