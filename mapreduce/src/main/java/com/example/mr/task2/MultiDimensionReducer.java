package com.example.mr.task2;

import com.example.mr.common.StatResultWritable;
import com.example.mr.common.UserBehaviorWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * 多维度行为分析 Reducer
 * <p>
 * 功能: 按维度分组聚合,统计各维度的用户数、PV、转化数、转化率
 *
 * @author Lingma
 * @since 2026-06-09
 */
public class MultiDimensionReducer extends Reducer<Text, UserBehaviorWritable, Text, StatResultWritable> {

    /**
     * Reduce 方法
     *
     * @param key      维度Key (如 age_18_25)
     * @param values   该维度下的所有用户行为数据
     * @param context  上下文
     */
    @Override
    protected void reduce(Text key, Iterable<UserBehaviorWritable> values, Context context)
            throws IOException, InterruptedException {

        long userCount = 0;       // 用户数
        long pv = 0;              // 页面浏览量
        long totalPagesSum = 0;   // 总访问页数
        long convertedCount = 0;  // 转化用户数

        // 遍历所有值进行聚合
        for (UserBehaviorWritable value : values) {
            userCount++;
            totalPagesSum += value.getTotalPagesVisited();
            pv += value.getTotalPagesVisited();

            // 判断是否转化
            if (value.isConverted()) {
                convertedCount++;
            }
        }

        // 计算平均访问页数
        double avgPages = userCount > 0 ? (double) totalPagesSum / userCount : 0.0;

        // 计算转化率
        double conversionRate = userCount > 0 ? (double) convertedCount / userCount : 0.0;

        // 解析维度类型和维度值
        String keyStr = key.toString();
        String[] parts = keyStr.split("_", 2);
        String dimensionType = parts[0];
        String dimensionValue = parts.length > 1 ? parts[1] : "";

        // 构造结果对象
        StatResultWritable result = new StatResultWritable();
        result.setUv(userCount);
        result.setPv(pv);
        result.setAvgPages(avgPages);
        result.setConvertedCount(convertedCount);
        result.setConversionRate(conversionRate);
        result.setDimensionType(dimensionType);
        result.setDimensionValue(dimensionValue);

        // 输出结果
        context.write(key, result);

        // 记录统计信息(仅输出部分示例)
        if (userCount % 1000 == 0 || userCount < 10) {
            System.out.println(String.format("维度[%s_%s]: 用户数=%d, PV=%d, 转化数=%d, 转化率=%.4f",
                    dimensionType, dimensionValue, userCount, pv, convertedCount, conversionRate));
        }
    }
}
