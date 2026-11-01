package com.example.ecommerce.integration.hadoop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ecommerce.module.stat.entity.*;
import com.example.ecommerce.module.stat.mapper.*;
import com.example.ecommerce.module.task.entity.MrJobLog;
import com.example.ecommerce.module.task.mapper.MrJobLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MrIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(MrIntegrationService.class);

    private static final String CLASS_BASIC      = "com.example.mr.task1.BasicMetricsDriver";
    private static final String CLASS_MULTI_DIM  = "com.example.mr.task2.MultiDimensionDriver";
    private static final String CLASS_FUNNEL      = "com.example.mr.task3.ConversionFunnelDriver";
    private static final String CLASS_COMPARISON  = "com.example.mr.task4.UserComparisonDriver";

    @Autowired
    private HadoopSshService sshService;

    @Autowired
    private StatBasicMetricsMapper basicMetricsMapper;

    @Autowired
    private StatMultiDimensionMapper multiDimensionMapper;

    @Autowired
    private StatFunnelStepMapper funnelStepMapper;

    @Autowired
    private StatUserComparisonMapper userComparisonMapper;

    @Autowired
    private MrJobLogMapper mrJobLogMapper;

    /**
     * 是否属于真实 MR 任务类型
     */
    public static boolean isRealMrJob(String jobType) {
        return "basic".equals(jobType)
                || "multi_dimension".equals(jobType)
                || "funnel".equals(jobType)
                || "comparison".equals(jobType);
    }

    /**
     * 异步执行 MR 任务（由 TaskService 在独立线程中调用）
     */
    public void executeJob(Long taskId) {
        MrJobLog task = mrJobLogMapper.selectById(taskId);
        if (task == null) {
            log.warn("Task not found: {}", taskId);
            return;
        }

        LocalDateTime startTime = LocalDateTime.now();
        try {
            // 1. 更新为 RUNNING
            task.setStatus("RUNNING");
            task.setStartTime(startTime);
            task.setProgress(10);
            mrJobLogMapper.updateById(task);

            String mainClass = resolveMainClass(task.getJobType());
            String outputPath = task.getOutputPath();
            if (mainClass == null) {
                throw new RuntimeException("Unknown jobType: " + task.getJobType());
            }

            log.info("Starting MR job [{}]: class={}, output={}", task.getJobId(), mainClass, outputPath);

            // 2. 运行 MR 任务
            task.setProgress(20);
            mrJobLogMapper.updateById(task);
            sshService.runMapReduceJob(mainClass, outputPath);

            // 3. 读取 HDFS 输出
            task.setProgress(75);
            mrJobLogMapper.updateById(task);
            String output = sshService.readHdfsFile(outputPath + "/part-r-00000");
            log.info("MR output received, length={}", output.length());

            // 4. 解析 + 写入 DB
            task.setProgress(88);
            mrJobLogMapper.updateById(task);
            saveResults(task.getJobType(), output, LocalDate.now(), task.getJobId());

            // 5. 标记成功
            LocalDateTime endTime = LocalDateTime.now();
            task.setStatus("SUCCESS");
            task.setEndTime(endTime);
            task.setProgress(100);
            task.setDuration(Duration.between(startTime, endTime).getSeconds());
            mrJobLogMapper.updateById(task);
            log.info("MR job [{}] finished in {}s", task.getJobId(), task.getDuration());

        } catch (Exception e) {
            log.error("MR job [{}] failed: {}", task.getJobId(), e.getMessage(), e);
            task.setStatus("FAILED");
            task.setEndTime(LocalDateTime.now());
            task.setErrorMsg(e.getMessage() != null
                    ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                    : "Unknown error");
            task.setDuration(Duration.between(startTime, LocalDateTime.now()).getSeconds());
            mrJobLogMapper.updateById(task);
        }
    }

    private String resolveMainClass(String jobType) {
        switch (jobType) {
            case "basic":           return CLASS_BASIC;
            case "multi_dimension": return CLASS_MULTI_DIM;
            case "funnel":          return CLASS_FUNNEL;
            case "comparison":      return CLASS_COMPARISON;
            default:                return null;
        }
    }

    private void saveResults(String jobType, String output, LocalDate statDate, String jobId) {
        switch (jobType) {
            case "basic":
                // 删除当天已有数据，保证重跑幂等
                basicMetricsMapper.delete(new LambdaQueryWrapper<StatBasicMetrics>()
                        .eq(StatBasicMetrics::getStatDate, statDate));
                StatBasicMetrics m = MrResultParser.parseBasicMetrics(output, statDate, jobId);
                basicMetricsMapper.insert(m);
                log.info("Saved basic metrics: pv={}, uv={}", m.getPv(), m.getUv());
                break;

            case "multi_dimension":
                multiDimensionMapper.delete(new LambdaQueryWrapper<StatMultiDimension>()
                        .eq(StatMultiDimension::getStatDate, statDate));
                List<StatMultiDimension> dims = MrResultParser.parseMultiDimension(output, statDate, jobId);
                dims.forEach(multiDimensionMapper::insert);
                log.info("Saved {} multi-dimension records", dims.size());
                break;

            case "funnel":
                funnelStepMapper.delete(new LambdaQueryWrapper<StatFunnelStep>()
                        .eq(StatFunnelStep::getStatDate, statDate));
                List<StatFunnelStep> steps = MrResultParser.parseFunnelSteps(output, statDate, jobId);
                steps.forEach(funnelStepMapper::insert);
                log.info("Saved {} funnel step records", steps.size());
                break;

            case "comparison":
                userComparisonMapper.delete(new LambdaQueryWrapper<StatUserComparison>()
                        .eq(StatUserComparison::getStatDate, statDate));
                List<StatUserComparison> comps = MrResultParser.parseUserComparison(output, statDate, jobId);
                comps.forEach(userComparisonMapper::insert);
                log.info("Saved {} user comparison records", comps.size());
                break;
        }
    }
}
