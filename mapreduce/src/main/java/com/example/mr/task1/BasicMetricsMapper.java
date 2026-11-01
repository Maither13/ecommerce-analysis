package com.example.mr.task1;

import com.example.mr.common.UserBehaviorWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

/**
 * 基础指标统计 Mapper
 * <p>
 * 功能: 读取用户行为数据,提取新用户标识和访问页数
 * <p>
 * 输入: CSV 文件每一行(跳过表头)
 * 输出: Key="ALL", Value=(isNewUser, totalPagesVisited)
 *
 * @author Lingma
 * @since 2026-06-09
 */
public class BasicMetricsMapper extends Mapper<LongWritable, Text, Text, UserBehaviorWritable> {

    private static final Text OUTPUT_KEY = new Text("ALL");
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

        // 跳过表头(第一行)
        if (key.get() == 0 && line.contains("new_user")) {
            return;
        }

        try {
            // 解析 CSV 行
            String[] fields = line.split(",");
            if (fields.length < 13) {
                // 数据不完整,跳过
                return;
            }

            // 提取关键字段
            int isNewUser = Integer.parseInt(fields[0]);
            int totalPages = Integer.parseInt(fields[7]);

            // 设置输出值
            outputValue.setIsNewUser(isNewUser);
            outputValue.setTotalPagesVisited(totalPages);

            // 输出 Key="ALL", Value=(isNewUser, totalPages)
            context.write(OUTPUT_KEY, outputValue);

        } catch (NumberFormatException e) {
            // 数据格式错误,记录日志并跳过
            System.err.println("解析数据失败: " + line);
        }
    }
}
