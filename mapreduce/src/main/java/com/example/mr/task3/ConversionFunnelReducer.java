package com.example.mr.task3;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * 转化漏斗分析 Reducer
 * <p>
 * 功能: 统计各步骤的用户数,计算转化率和流失率
 *
 * @author Lingma
 * @since 2026-06-09
 */
public class ConversionFunnelReducer extends Reducer<Text, Text, Text, Text> {

    /**
     * Reduce 方法
     *
     * @param key      漏斗Key (如 funnel_ALL_ALL)
     * @param values   该维度下的所有漏斗数据
     * @param context  上下文
     */
    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        long step1Count = 0;  // 首页访问用户数
        long step2Count = 0;  // 列表页访问用户数
        long step3Count = 0;  // 商品页访问用户数
        long step4Count = 0;  // 支付页访问用户数
        long step5Count = 0;  // 确认页访问用户数

        // 遍历所有值进行聚合
        for (Text value : values) {
            String[] parts = value.toString().split(",");
            if (parts.length != 5) {
                continue;
            }

            if (Integer.parseInt(parts[0]) >= 1) step1Count++;
            if (Integer.parseInt(parts[1]) >= 1) step2Count++;
            if (Integer.parseInt(parts[2]) >= 1) step3Count++;
            if (Integer.parseInt(parts[3]) >= 1) step4Count++;
            if (Integer.parseInt(parts[4]) >= 1) step5Count++;
        }

        // 计算各步骤的转化率和流失率
        double rate1to2 = step1Count > 0 ? (double) step2Count / step1Count : 0.0;
        double rate2to3 = step2Count > 0 ? (double) step3Count / step2Count : 0.0;
        double rate3to4 = step3Count > 0 ? (double) step4Count / step3Count : 0.0;
        double rate4to5 = step4Count > 0 ? (double) step5Count / step4Count : 0.0;

        double loss1to2 = 1.0 - rate1to2;
        double loss2to3 = 1.0 - rate2to3;
        double loss3to4 = 1.0 - rate3to4;
        double loss4to5 = 1.0 - rate4to5;

        // 计算整体转化率
        double overallRate = step1Count > 0 ? (double) step5Count / step1Count : 0.0;

        // 解析维度类型和值
        String keyStr = key.toString();
        String[] keyParts = keyStr.split("_", 3);
        String filterType = keyParts.length > 1 ? keyParts[1] : "ALL";
        String filterValue = keyParts.length > 2 ? keyParts[2] : "ALL";

        // 输出结果(格式化为CSV)
        // 格式: step_name,step_order,user_count,conversion_rate,loss_rate,overall_rate,filter_type,filter_value
        StringBuilder result = new StringBuilder();
        result.append(String.format("home_page,1,%d,1.0000,0.0000,1.0000,%s,%s\n", step1Count, filterType, filterValue));
        result.append(String.format("listing_page,2,%d,%.4f,%.4f,%.4f,%s,%s\n", step2Count, rate1to2, loss1to2, (double) step2Count / step1Count, filterType, filterValue));
        result.append(String.format("product_page,3,%d,%.4f,%.4f,%.4f,%s,%s\n", step3Count, rate2to3, loss2to3, (double) step3Count / step1Count, filterType, filterValue));
        result.append(String.format("payment_page,4,%d,%.4f,%.4f,%.4f,%s,%s\n", step4Count, rate3to4, loss3to4, (double) step4Count / step1Count, filterType, filterValue));
        result.append(String.format("confirmation_page,5,%d,%.4f,%.4f,%.4f,%s,%s", step5Count, rate4to5, loss4to5, overallRate, filterType, filterValue));

        context.write(key, new Text(result.toString()));

        // 记录统计信息
        System.out.println("========== 转化漏斗 [" + filterType + "=" + filterValue + "] ==========");
        System.out.println("第1步 首页:      " + step1Count + "人");
        System.out.println("第2步 列表页:    " + step2Count + "人 (转化率: " + String.format("%.2f%%", rate1to2 * 100) + ")");
        System.out.println("第3步 商品页:    " + step3Count + "人 (转化率: " + String.format("%.2f%%", rate2to3 * 100) + ")");
        System.out.println("第4步 支付页:    " + step4Count + "人 (转化率: " + String.format("%.2f%%", rate3to4 * 100) + ")");
        System.out.println("第5步 确认页:    " + step5Count + "人 (转化率: " + String.format("%.2f%%", rate4to5 * 100) + ")");
        System.out.println("整体转化率:      " + String.format("%.2f%%", overallRate * 100));
        System.out.println("================================================");
    }
}
