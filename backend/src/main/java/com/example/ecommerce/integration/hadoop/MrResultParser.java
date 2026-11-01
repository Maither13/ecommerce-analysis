package com.example.ecommerce.integration.hadoop;

import com.example.ecommerce.module.stat.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MrResultParser {

    private static final Logger log = LoggerFactory.getLogger(MrResultParser.class);

    // =========================================================
    // Task1: BasicMetrics
    // 输出格式: BASIC_METRICS\tStatResultWritable{pv=..., uv=..., ...}
    // =========================================================
    public static StatBasicMetrics parseBasicMetrics(String output, LocalDate statDate, String jobId) {
        StatBasicMetrics m = new StatBasicMetrics();
        m.setStatDate(statDate);
        m.setJobId(jobId);
        m.setPv(extractLong(output, "pv=(\\d+)"));
        m.setUv(extractLong(output, "uv=(\\d+)"));
        m.setNewUserCount(extractLong(output, "newUserCount=(\\d+)"));
        m.setOldUserCount(extractLong(output, "oldUserCount=(\\d+)"));
        m.setNewUserRatio(extractDecimal(output, "newUserRatio=([\\d.E+-]+)"));
        m.setAvgPages(extractDecimal(output, "avgPages=([\\d.E+-]+)"));
        m.setTotalPagesSum(extractLong(output, "totalPagesSum=(\\d+)"));
        m.setMaxPages(extractInt(output, "maxPages=(\\d+)"));
        m.setMinPages(extractInt(output, "minPages=(\\d+)"));
        return m;
    }

    // =========================================================
    // Task2: MultiDimension
    // 每行格式: dim_key\tStatResultWritable{uv=..., dimensionType='X', dimensionValue='Y', ...}
    // =========================================================
    public static List<StatMultiDimension> parseMultiDimension(String output, LocalDate statDate, String jobId) {
        List<StatMultiDimension> results = new ArrayList<>();
        for (String line : output.split("\n")) {
            if (line.trim().isEmpty()) continue;
            try {
                String dimType = extractString(line, "dimensionType='([^']*)'");
                String dimValue = extractString(line, "dimensionValue='([^']*)'");
                if (dimType == null || dimType.isEmpty()) continue;

                StatMultiDimension d = new StatMultiDimension();
                d.setStatDate(statDate);
                d.setJobId(jobId);
                d.setDimensionType(dimType);
                d.setDimensionValue(dimValue);
                d.setUserCount(extractLong(line, "uv=(\\d+)"));
                d.setPv(extractLong(line, "pv=(\\d+)"));
                d.setAvgPages(extractDecimal(line, "avgPages=([\\d.E+-]+)"));
                d.setConvertedCount(extractLong(line, "convertedCount=(\\d+)"));
                d.setConversionRate(extractDecimal(line, "conversionRate=([\\d.E+-]+)"));
                results.add(d);
            } catch (Exception e) {
                log.warn("Skipping multi-dimension line: {}", line.substring(0, Math.min(line.length(), 80)));
            }
        }
        return results;
    }

    // =========================================================
    // Task3: ConversionFunnel
    // 首行格式: funnel_ALL_ALL\thome_page,1,100000,1.0000,0.0000,1.0000,ALL,ALL
    // 后续行无 key:         listing_page,2,...
    // 字段顺序: stepName,stepOrder,userCount,conversionRate,lossRate,overallRate,filterType,filterValue
    // =========================================================
    public static List<StatFunnelStep> parseFunnelSteps(String output, LocalDate statDate, String jobId) {
        List<StatFunnelStep> results = new ArrayList<>();
        for (String line : output.split("\n")) {
            if (line.trim().isEmpty()) continue;
            // 去掉前缀 key (如 "funnel_ALL_ALL\t")
            String data = line.contains("\t") ? line.substring(line.indexOf('\t') + 1) : line;
            data = data.trim();
            String[] parts = data.split(",");
            if (parts.length < 8) continue;
            try {
                StatFunnelStep step = new StatFunnelStep();
                step.setStepName(parts[0].trim());
                step.setStepOrder(Integer.parseInt(parts[1].trim()));
                step.setUserCount(Long.parseLong(parts[2].trim()));
                step.setConversionRate(bd(parts[3]));
                step.setLossRate(bd(parts[4]));
                step.setOverallConversionRate(bd(parts[5]));
                step.setFilterType(parts[6].trim());
                step.setFilterValue(parts[7].trim());
                step.setStatDate(statDate);
                step.setJobId(jobId);
                results.add(step);
            } catch (Exception e) {
                log.warn("Skipping funnel line: {}", data.substring(0, Math.min(data.length(), 80)));
            }
        }
        return results;
    }

    // =========================================================
    // Task4: UserComparison
    // 输出格式（两块，key=new_user / old_user）:
    //   new_user\tuser_type=new_user\nuser_count=31525\navg_pages=5.81\n...
    //   old_user\tuser_type=old_user\n...
    // =========================================================
    public static List<StatUserComparison> parseUserComparison(String output, LocalDate statDate, String jobId) {
        List<StatUserComparison> results = new ArrayList<>();
        String currentUserType = null;

        for (String line : output.split("\n")) {
            if (line.trim().isEmpty()) continue;

            String data;
            if (line.contains("\t")) {
                String key = line.substring(0, line.indexOf('\t')).trim();
                data = line.substring(line.indexOf('\t') + 1).trim();
                if ("new_user".equals(key)) currentUserType = "new";
                else if ("old_user".equals(key)) currentUserType = "old";
            } else {
                data = line.trim();
            }

            if (currentUserType == null || !data.contains("=")) continue;

            String[] kv = data.split("=", 2);
            if (kv.length != 2) continue;
            String metricName = kv[0].trim();
            String rawValue = kv[1].trim().replaceAll(",$", ""); // 去掉末尾逗号

            if ("user_type".equals(metricName)) continue;
            // distribution 字段是 "key:ratio,key:ratio,..." 形式，按子项展开
            if (metricName.endsWith("_distribution")) {
                parseDistribution(results, currentUserType, metricName, rawValue, statDate, jobId);
                continue;
            }

            try {
                StatUserComparison c = new StatUserComparison();
                c.setUserType(currentUserType);
                c.setMetricCategory(metricCategory(metricName));
                c.setMetricName(metricName);
                c.setMetricValue(new BigDecimal(rawValue).setScale(4, RoundingMode.HALF_UP));
                c.setMetricUnit(metricUnit(metricName));
                c.setStatDate(statDate);
                c.setJobId(jobId);
                results.add(c);
            } catch (Exception e) {
                log.warn("Skipping user comparison metric: {}={}", metricName, rawValue);
            }
        }
        return results;
    }

    /** 解析 source/device distribution 为多条记录 */
    private static void parseDistribution(List<StatUserComparison> results, String userType,
                                          String metricName, String value,
                                          LocalDate statDate, String jobId) {
        // value 格式: "Direct:0.3200,Ads:0.2800,..."
        String category = metricName.startsWith("source") ? "source" : "device";
        for (String entry : value.split(",")) {
            String[] kv = entry.split(":");
            if (kv.length != 2) continue;
            try {
                StatUserComparison c = new StatUserComparison();
                c.setUserType(userType);
                c.setMetricCategory(category);
                c.setMetricName(metricName + "_" + kv[0].trim());
                c.setMetricValue(new BigDecimal(kv[1].trim()).setScale(4, RoundingMode.HALF_UP));
                c.setMetricUnit("ratio");
                c.setStatDate(statDate);
                c.setJobId(jobId);
                results.add(c);
            } catch (Exception ignored) {}
        }
    }

    // =========================================================
    // 工具方法
    // =========================================================
    private static long extractLong(String text, String pattern) {
        Matcher m = Pattern.compile(pattern).matcher(text);
        return m.find() ? Long.parseLong(m.group(1)) : 0L;
    }

    private static int extractInt(String text, String pattern) {
        Matcher m = Pattern.compile(pattern).matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static BigDecimal extractDecimal(String text, String pattern) {
        Matcher m = Pattern.compile(pattern).matcher(text);
        if (m.find()) {
            try {
                return new BigDecimal(m.group(1)).setScale(4, RoundingMode.HALF_UP);
            } catch (Exception ignored) {}
        }
        return BigDecimal.ZERO;
    }

    private static String extractString(String text, String pattern) {
        Matcher m = Pattern.compile(pattern).matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s.trim()).setScale(4, RoundingMode.HALF_UP);
    }

    private static String metricCategory(String name) {
        if (name.endsWith("_rate") && !name.startsWith("conversion")) return "pages_preference";
        switch (name) {
            case "user_count": case "avg_pages": case "max_pages":
            case "min_pages": case "total_pages": case "conversion_rate": return "basic";
            default: return "basic";
        }
    }

    private static String metricUnit(String name) {
        if (name.contains("rate")) return "ratio";
        if (name.contains("pages")) return "pages";
        if (name.contains("count")) return "count";
        return "";
    }
}
