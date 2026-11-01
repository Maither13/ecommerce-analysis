package com.example.mr.task3;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

/**
 * 转化漏斗分析 Mapper
 * <p>
 * 功能: 判断每条记录的各页面访问情况,输出到不同的筛选维度
 * <p>
 * Key格式: funnel_{filter_type}_{filter_value}
 * 例如: funnel_ALL_ALL, funnel_new_user_1, funnel_source_Direct
 * <p>
 * Value格式: has_home,has_listing,has_product,has_payment,has_confirmation
 *
 * @author Lingma
 * @since 2026-06-09
 */
public class ConversionFunnelMapper extends Mapper<LongWritable, Text, Text, Text> {

    private static final String VALUE_SEPARATOR = ",";

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

        // 跳过空行和表头
        if (line.isEmpty() || (key.get() == 0 && line.contains("new_user"))) {
            return;
        }

        try {
            // 解析 CSV 行
            String[] fields = line.split(",");
            if (fields.length < 13) {
                return;
            }

            int isNewUser = Integer.parseInt(fields[0]);
            String source = fields[6];
            String device = fields[4];

            // 判断各页面是否访问过
            int hasHome = Integer.parseInt(fields[8]) >= 1 ? 1 : 0;
            int hasListing = Integer.parseInt(fields[9]) >= 1 ? 1 : 0;
            int hasProduct = Integer.parseInt(fields[10]) >= 1 ? 1 : 0;
            int hasPayment = Integer.parseInt(fields[11]) >= 1 ? 1 : 0;
            int hasConfirmation = Integer.parseInt(fields[12]) >= 1 ? 1 : 0;

            // 构造 Value
            String outputValue = String.format("%d,%d,%d,%d,%d",
                    hasHome, hasListing, hasProduct, hasPayment, hasConfirmation);

            // 输出到 ALL 维度
            context.write(new Text("funnel_ALL_ALL"), new Text(outputValue));

            // 输出到新用户/老用户维度
            context.write(new Text("funnel_new_user_" + isNewUser), new Text(outputValue));

            // 输出到流量来源维度
            context.write(new Text("funnel_source_" + source), new Text(outputValue));

            // 输出到设备类型维度
            context.write(new Text("funnel_device_" + device), new Text(outputValue));

        } catch (NumberFormatException e) {
            System.err.println("解析数据失败: " + line);
        }
    }
}
