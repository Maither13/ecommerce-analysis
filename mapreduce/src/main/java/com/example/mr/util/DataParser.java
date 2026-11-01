package com.example.mr.util;

import com.example.mr.common.UserBehaviorWritable;

/**
 * 数据解析工具类
 *
 * @author Lingma
 * @since 2026-06-09
 */
public class DataParser {

    /**
     * 从 CSV 行解析用户行为数据
     *
     * @param line CSV 行字符串
     * @return UserBehaviorWritable 对象,解析失败返回 null
     */
    public static UserBehaviorWritable parseFromCsv(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        try {
            String[] fields = line.split(",");
            if (fields.length < 13) {
                return null;
            }

            UserBehaviorWritable data = new UserBehaviorWritable();
            data.setIsNewUser(Integer.parseInt(fields[0]));
            data.setAge(Integer.parseInt(fields[1]));
            data.setSex(fields[2]);
            data.setMarket(Integer.parseInt(fields[3]));
            data.setDevice(fields[4]);
            data.setOperativeSystem(fields[5]);
            data.setSource(fields[6]);
            data.setTotalPagesVisited(Integer.parseInt(fields[7]));
            data.setHomePage(Integer.parseInt(fields[8]));
            data.setListingPage(Integer.parseInt(fields[9]));
            data.setProductPage(Integer.parseInt(fields[10]));
            data.setPaymentPage(Integer.parseInt(fields[11]));
            data.setConfirmationPage(Integer.parseInt(fields[12]));

            return data;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 判断是否跳过该行(表头或空行)
     *
     * @param line      行内容
     * @param lineNum   行号
     * @return true 表示应该跳过
     */
    public static boolean shouldSkipLine(String line, long lineNum) {
        if (line == null || line.trim().isEmpty()) {
            return true;
        }
        // 第一行且包含表头字段名
        if (lineNum == 0 && line.contains("new_user")) {
            return true;
        }
        return false;
    }
}
