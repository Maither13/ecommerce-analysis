package com.example.mr.task3;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * 转化漏斗分析 Driver
 * <p>
 * 使用方法:
 * hadoop jar ecommerce-mapreduce-1.0.0.jar \
 *   com.example.mr.task3.ConversionFunnelDriver \
 *   /input/user_behavior.csv \
 *   /output/conversion_funnel
 *
 * @author Lingma
 * @since 2026-06-09
 */
public class ConversionFunnelDriver extends Configured implements Tool {

    @Override
    public int run(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: ConversionFunnelDriver <input_path> <output_path>");
            return -1;
        }

        String inputPath = args[0];
        String outputPath = args[1];

        Configuration conf = getConf();
        conf.set("mapreduce.job.name", "Conversion Funnel Analysis");

        Job job = Job.getInstance(conf, "Conversion Funnel Analysis");
        job.setJarByClass(ConversionFunnelDriver.class);

        // 设置 Mapper
        job.setMapperClass(ConversionFunnelMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

        // 设置 Reducer
        job.setReducerClass(ConversionFunnelReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        // 设置 Reduce Task 数量
        job.setNumReduceTasks(4);

        // 设置输入输出路径
        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        // 删除已存在的输出目录
        org.apache.hadoop.fs.FileSystem fs = org.apache.hadoop.fs.FileSystem.get(conf);
        Path outPath = new Path(outputPath);
        if (fs.exists(outPath)) {
            fs.delete(outPath, true);
        }

        boolean success = job.waitForCompletion(true);

        if (success) {
            System.out.println("转化漏斗分析任务执行成功!");
        } else {
            System.err.println("转化漏斗分析任务执行失败!");
        }

        return success ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        ConversionFunnelDriver driver = new ConversionFunnelDriver();
        driver.setConf(conf);
        int exitCode = driver.run(args);
        System.exit(exitCode);
    }
}
