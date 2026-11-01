package com.example.mr.task2;

import com.example.mr.common.UserBehaviorWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

/**
 * 多维度行为分析 Mapper
 * <p>
 * 功能: 按多个维度输出多组 Key-Value
 * <p>
 * 输出维度:
 * - age: 年龄段 (under_18, 18_25, 26_35, 36_45, 46_plus)
 * - sex: 性别 (Male, Female)
 * - market: 市场区域 (1, 2, 3, 4)
 * - device: 设备类型 (mobile, desktop)
 * - os: 操作系统 (android, iOS, windows, mac, linux, other)
 * - source: 流量来源 (Direct, Seo, Ads)
 * <p>
 * Key格式: {dimension_type}_{dimension_value}
 * 例如: age_18_25, sex_Male, market_1, device_mobile, os_android, source_Direct
 *
 * @author Lingma
 * @since 2026-06-09
 */
public class MultiDimensionMapper extends Mapper<LongWritable, Text, Text, UserBehaviorWritable> {

    private final UserBehaviorWritable outputValue = new UserBehaviorWritable();

    /**
     * Map 方法
     *
     * @param key     行偏移量
     * @param value   行内容(CSV格式)
     * @param context 上下文
     */
    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        String line = value.toString().trim();

        // 跳过空行
        if (line.isEmpty()) {
            return;
        }

        // 跳过表头
        if (key.get() == 0 && line.contains("new_user")) {
            return;
        }

        try {
            // 解析 CSV 行
            String[] fields = line.split(",");
            if (fields.length < 13) {
                return;
            }

            // 解析数据到 UserBehaviorWritable
            outputValue.parseFromCsv(line);

            // 按年龄维度输出
            String ageGroup = outputValue.getAgeGroup();
            context.write(new Text("age_" + ageGroup), outputValue);

            // 按性别维度输出
            context.write(new Text("sex_" + outputValue.getSex()), outputValue);

            // 按市场区域维度输出
            context.write(new Text("market_" + outputValue.getMarket()), outputValue);

            // 按设备类型维度输出
            context.write(new Text("device_" + outputValue.getDevice()), outputValue);

            // 按操作系统维度输出
            context.write(new Text("os_" + outputValue.getOperativeSystem()), outputValue);

            // 按流量来源维度输出
            context.write(new Text("source_" + outputValue.getSource()), outputValue);

        } catch (NumberFormatException e) {
            System.err.println("解析数据失败: " + line);
        }
    }
}
