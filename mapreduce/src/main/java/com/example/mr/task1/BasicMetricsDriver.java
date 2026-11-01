package com.example.mr.task1;

import com.example.mr.common.StatResultWritable;
import com.example.mr.common.UserBehaviorWritable;
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
 * 基础指标统计 Driver
 * <p>
 * 功能: 配置并执行 MapReduce 作业
 * <p>
 * 使用方法:
 * hadoop jar ecommerce-mapreduce-1.0.0.jar \
 *   com.example.mr.task1.BasicMetricsDriver \
 *   /input/user_behavior.csv \
 *   /output/basic_metrics
 *
 * @author Lingma
 * @since 2026-06-09
 */
public class BasicMetricsDriver extends Configured implements Tool {

    /**
     * 运行 MapReduce 作业
     *
     * @param args 命令行参数 [输入路径, 输出路径]
     * @return 执行结果码
     */
    @Override
    public int run(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: BasicMetricsDriver <input_path> <output_path>");
            return -1;
        }

        String inputPath = args[0];
        String outputPath = args[1];

        // 获取配置
        Configuration conf = getConf();
        conf.set("mapreduce.job.name", "Basic Metrics Statistics");

        // 创建 Job
        Job job = Job.getInstance(conf, "Basic Metrics Statistics");
        job.setJarByClass(BasicMetricsDriver.class);

        // 设置 Mapper
        job.setMapperClass(BasicMetricsMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(UserBehaviorWritable.class);

        // 设置 Reducer
        job.setReducerClass(BasicMetricsReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(StatResultWritable.class);

        // 设置 Reduce Task 数量
        job.setNumReduceTasks(1);

        // 设置输入输出路径
        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        // 如果输出目录已存在,先删除
        org.apache.hadoop.fs.FileSystem fs = org.apache.hadoop.fs.FileSystem.get(conf);
        Path outPath = new Path(outputPath);
        if (fs.exists(outPath)) {
            fs.delete(outPath, true);
        }

        // 提交作业并等待完成
        boolean success = job.waitForCompletion(true);

        if (success) {
            System.out.println("任务执行成功!");
            System.out.println("Map 任务数: " + job.getCounters().findCounter("org.apache.hadoop.mapreduce.TaskCounter", "MAP_INPUT_RECORDS").getValue());
            System.out.println("Reduce 任务数: " + job.getCounters().findCounter("org.apache.hadoop.mapreduce.TaskCounter", "REDUCE_OUTPUT_RECORDS").getValue());
        } else {
            System.err.println("任务执行失败!");
        }

        return success ? 0 : 1;
    }

    /**
     * 主方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) throws Exception {
        // 直接构造 Configuration 并调用 run()，绕过 ToolRunner/GenericOptionsParser
        // 避免 Hadoop 3.3.6 下 GenericOptionsParser 意外消耗路径参数
        Configuration conf = new Configuration();
        BasicMetricsDriver driver = new BasicMetricsDriver();
        driver.setConf(conf);
        int exitCode = driver.run(args);
        System.exit(exitCode);
    }
}
