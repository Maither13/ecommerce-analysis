package com.example.mr.common;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * 统计结果 Writable 类
 * 用于在 MapReduce 中传递统计结果数据
 *
 * @author Lingma
 * @since 2026-06-09
 */
public class StatResultWritable implements Writable {

    private long pv;                    // 页面浏览量
    private long uv;                    // 独立用户数
    private long newUserCount;          // 新用户数
    private long oldUserCount;          // 老用户数
    private double newUserRatio;        // 新用户占比
    private double avgPages;            // 人均访问页数
    private long totalPagesSum;         // 总访问页数
    private int maxPages;               // 最大访问页数
    private int minPages;               // 最小访问页数
    private long convertedCount;        // 转化用户数
    private double conversionRate;      // 转化率
    private String dimensionType;       // 维度类型
    private String dimensionValue;      // 维度值

    public StatResultWritable() {
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(pv);
        out.writeLong(uv);
        out.writeLong(newUserCount);
        out.writeLong(oldUserCount);
        out.writeDouble(newUserRatio);
        out.writeDouble(avgPages);
        out.writeLong(totalPagesSum);
        out.writeInt(maxPages);
        out.writeInt(minPages);
        out.writeLong(convertedCount);
        out.writeDouble(conversionRate);
        out.writeUTF(dimensionType != null ? dimensionType : "");
        out.writeUTF(dimensionValue != null ? dimensionValue : "");
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.pv = in.readLong();
        this.uv = in.readLong();
        this.newUserCount = in.readLong();
        this.oldUserCount = in.readLong();
        this.newUserRatio = in.readDouble();
        this.avgPages = in.readDouble();
        this.totalPagesSum = in.readLong();
        this.maxPages = in.readInt();
        this.minPages = in.readInt();
        this.convertedCount = in.readLong();
        this.conversionRate = in.readDouble();
        this.dimensionType = in.readUTF();
        this.dimensionValue = in.readUTF();
    }

    // Getters and Setters
    public long getPv() {
        return pv;
    }

    public void setPv(long pv) {
        this.pv = pv;
    }

    public long getUv() {
        return uv;
    }

    public void setUv(long uv) {
        this.uv = uv;
    }

    public long getNewUserCount() {
        return newUserCount;
    }

    public void setNewUserCount(long newUserCount) {
        this.newUserCount = newUserCount;
    }

    public long getOldUserCount() {
        return oldUserCount;
    }

    public void setOldUserCount(long oldUserCount) {
        this.oldUserCount = oldUserCount;
    }

    public double getNewUserRatio() {
        return newUserRatio;
    }

    public void setNewUserRatio(double newUserRatio) {
        this.newUserRatio = newUserRatio;
    }

    public double getAvgPages() {
        return avgPages;
    }

    public void setAvgPages(double avgPages) {
        this.avgPages = avgPages;
    }

    public long getTotalPagesSum() {
        return totalPagesSum;
    }

    public void setTotalPagesSum(long totalPagesSum) {
        this.totalPagesSum = totalPagesSum;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public int getMinPages() {
        return minPages;
    }

    public void setMinPages(int minPages) {
        this.minPages = minPages;
    }

    public long getConvertedCount() {
        return convertedCount;
    }

    public void setConvertedCount(long convertedCount) {
        this.convertedCount = convertedCount;
    }

    public double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(double conversionRate) {
        this.conversionRate = conversionRate;
    }

    public String getDimensionType() {
        return dimensionType;
    }

    public void setDimensionType(String dimensionType) {
        this.dimensionType = dimensionType;
    }

    public String getDimensionValue() {
        return dimensionValue;
    }

    public void setDimensionValue(String dimensionValue) {
        this.dimensionValue = dimensionValue;
    }

    /**
     * 格式化为 CSV 行
     */
    public String toCsvLine(String prefix) {
        return String.format("%s,%d,%d,%d,%d,%.4f,%.2f,%d,%d,%d,%d,%.4f,%s,%s",
                prefix, pv, uv, newUserCount, oldUserCount, newUserRatio,
                avgPages, totalPagesSum, maxPages, minPages, convertedCount,
                conversionRate, dimensionType, dimensionValue);
    }

    @Override
    public String toString() {
        return "StatResultWritable{" +
                "pv=" + pv +
                ", uv=" + uv +
                ", newUserCount=" + newUserCount +
                ", oldUserCount=" + oldUserCount +
                ", newUserRatio=" + newUserRatio +
                ", avgPages=" + avgPages +
                ", totalPagesSum=" + totalPagesSum +
                ", maxPages=" + maxPages +
                ", minPages=" + minPages +
                ", convertedCount=" + convertedCount +
                ", conversionRate=" + conversionRate +
                ", dimensionType='" + dimensionType + '\'' +
                ", dimensionValue='" + dimensionValue + '\'' +
                '}';
    }
}
