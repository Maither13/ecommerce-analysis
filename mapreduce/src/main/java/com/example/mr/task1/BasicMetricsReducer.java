package com.example.mr.task1;

import com.example.mr.common.StatResultWritable;
import com.example.mr.common.UserBehaviorWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * 基础指标统计 Reducer
 * <p>
 * 功能: 聚合计算 PV、UV、新用户数等指标
 * <p>
 * 输入: Key="ALL", Value=(isNewUser, totalPagesVisited)
 * 输出: Key="BASIC_METRICS", Value=(pv, uv, newUserCount, ...)
 *
 * @author Lingma
 * @since 2026-06-09
 */
public class BasicMetricsReducer extends Reducer<Text, UserBehaviorWritable, Text, StatResultWritable> {

    /**
     * Reduce 方法
     *
     * @param key      "ALL"
     * @param values   所有用户行为数据
     * @param context  上下文
     */
    @Override
    protected void reduce(Text key, Iterable<UserBehaviorWritable> values, Context context)
            throws IOException, InterruptedException {

        long pv = 0;              // 页面浏览量
        long uv = 0;              // 独立用户数
        long newUserCount = 0;    // 新用户数
        long oldUserCount = 0;    // 老用户数
        long totalPagesSum = 0;   // 总访问页数
        int maxPages = 0;
        int minPages = Integer.MAX_VALUE;

        // 遍历所有值进行聚合
        for (UserBehaviorWritable value : values) {
            uv++;
            totalPagesSum += value.getTotalPagesVisited();
            pv += value.getTotalPagesVisited();

            if (value.getIsNewUser() == 1) {
                newUserCount++;
            } else {
                oldUserCount++;
            }

            maxPages = Math.max(maxPages, value.getTotalPagesVisited());
            minPages = Math.min(minPages, value.getTotalPagesVisited());
        }

        // 处理没有数据的情况
        if (uv == 0) {
            minPages = 0;
        }

        // 计算衍生指标
        double newUserRatio = uv > 0 ? (double) newUserCount / uv : 0.0;
        double avgPages = uv > 0 ? (double) totalPagesSum / uv : 0.0;

        // 构造结果对象
        StatResultWritable result = new StatResultWritable();
        result.setPv(pv);
        result.setUv(uv);
        result.setNewUserCount(newUserCount);
        result.setOldUserCount(oldUserCount);
        result.setNewUserRatio(newUserRatio);
        result.setAvgPages(avgPages);
        result.setTotalPagesSum(totalPagesSum);
        result.setMaxPages(maxPages);
        result.setMinPages(minPages);

        // 输出结果
        context.write(new Text("BASIC_METRICS"), result);

        // 记录统计信息
        System.out.println("========== 基础指标统计结果 ==========");
        System.out.println("PV (页面浏览量): " + pv);
        System.out.println("UV (独立用户数): " + uv);
        System.out.println("新用户数: " + newUserCount);
        System.out.println("老用户数: " + oldUserCount);
        System.out.println("新用户占比: " + String.format("%.4f", newUserRatio));
        System.out.println("人均访问页数: " + String.format("%.2f", avgPages));
        System.out.println("总访问页数: " + totalPagesSum);
        System.out.println("最大访问页数: " + maxPages);
        System.out.println("最小访问页数: " + minPages);
        System.out.println("====================================");
    }
}
