package com.example.mr.task4;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

/**
 * 新老用户对比 Mapper
 * <p>
 * 功能: 根据 new_user 字段分类输出
 * <p>
 * Key: "new_user" 或 "old_user"
 * Value: pages,source,device,has_home,has_listing,has_product,has_payment,has_confirmation
 *
 * @author Lingma
 * @since 2026-06-09
 */
public class UserComparisonMapper extends Mapper<LongWritable, Text, Text, Text> {

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
            int totalPages = Integer.parseInt(fields[7]);
            String source = fields[6];
            String device = fields[4];

            // 判断各页面是否访问过
            int hasHome = Integer.parseInt(fields[8]) >= 1 ? 1 : 0;
            int hasListing = Integer.parseInt(fields[9]) >= 1 ? 1 : 0;
            int hasProduct = Integer.parseInt(fields[10]) >= 1 ? 1 : 0;
            int hasPayment = Integer.parseInt(fields[11]) >= 1 ? 1 : 0;
            int hasConfirmation = Integer.parseInt(fields[12]) >= 1 ? 1 : 0;

            // 构造 Value
            String outputValue = String.format("%d,%s,%s,%d,%d,%d,%d,%d",
                    totalPages, source, device, hasHome, hasListing, hasProduct, hasPayment, hasConfirmation);

            // 根据新用户标识输出到不同的 Key
            if (isNewUser == 1) {
                context.write(new Text("new_user"), new Text(outputValue));
            } else {
                context.write(new Text("old_user"), new Text(outputValue));
            }

        } catch (NumberFormatException e) {
            System.err.println("解析数据失败: " + line);
        }
    }
}
