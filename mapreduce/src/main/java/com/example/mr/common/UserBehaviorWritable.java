package com.example.mr.common;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * 用户行为数据 Writable 类
 * 用于在 MapReduce 中传递用户行为数据
 *
 * @author Lingma
 * @since 2026-06-09
 */
public class UserBehaviorWritable implements Writable {

    private int isNewUser;          // 是否新用户(0:老用户 1:新用户)
    private int age;                // 年龄
    private String sex;             // 性别
    private int market;             // 市场区域
    private String device;          // 设备类型
    private String operativeSystem; // 操作系统
    private String source;          // 流量来源
    private int totalPagesVisited;  // 总访问页数
    private int homePage;           // 首页访问次数
    private int listingPage;        // 列表页访问次数
    private int productPage;        // 商品页访问次数
    private int paymentPage;        // 支付页访问次数
    private int confirmationPage;   // 确认页访问次数

    public UserBehaviorWritable() {
    }

    public UserBehaviorWritable(int isNewUser, int totalPagesVisited) {
        this.isNewUser = isNewUser;
        this.totalPagesVisited = totalPagesVisited;
    }

    /**
     * 从 CSV 行解析数据
     *
     * @param line CSV 行字符串
     */
    public void parseFromCsv(String line) {
        String[] fields = line.split(",");
        if (fields.length >= 13) {
            this.isNewUser = Integer.parseInt(fields[0]);
            this.age = Integer.parseInt(fields[1]);
            this.sex = fields[2];
            this.market = Integer.parseInt(fields[3]);
            this.device = fields[4];
            this.operativeSystem = fields[5];
            this.source = fields[6];
            this.totalPagesVisited = Integer.parseInt(fields[7]);
            this.homePage = Integer.parseInt(fields[8]);
            this.listingPage = Integer.parseInt(fields[9]);
            this.productPage = Integer.parseInt(fields[10]);
            this.paymentPage = Integer.parseInt(fields[11]);
            this.confirmationPage = Integer.parseInt(fields[12]);
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(isNewUser);
        out.writeInt(age);
        out.writeUTF(sex != null ? sex : "");
        out.writeInt(market);
        out.writeUTF(device != null ? device : "");
        out.writeUTF(operativeSystem != null ? operativeSystem : "");
        out.writeUTF(source != null ? source : "");
        out.writeInt(totalPagesVisited);
        out.writeInt(homePage);
        out.writeInt(listingPage);
        out.writeInt(productPage);
        out.writeInt(paymentPage);
        out.writeInt(confirmationPage);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        this.isNewUser = in.readInt();
        this.age = in.readInt();
        this.sex = in.readUTF();
        this.market = in.readInt();
        this.device = in.readUTF();
        this.operativeSystem = in.readUTF();
        this.source = in.readUTF();
        this.totalPagesVisited = in.readInt();
        this.homePage = in.readInt();
        this.listingPage = in.readInt();
        this.productPage = in.readInt();
        this.paymentPage = in.readInt();
        this.confirmationPage = in.readInt();
    }

    // Getters and Setters
    public int getIsNewUser() {
        return isNewUser;
    }

    public void setIsNewUser(int isNewUser) {
        this.isNewUser = isNewUser;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public int getMarket() {
        return market;
    }

    public void setMarket(int market) {
        this.market = market;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getOperativeSystem() {
        return operativeSystem;
    }

    public void setOperativeSystem(String operativeSystem) {
        this.operativeSystem = operativeSystem;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getTotalPagesVisited() {
        return totalPagesVisited;
    }

    public void setTotalPagesVisited(int totalPagesVisited) {
        this.totalPagesVisited = totalPagesVisited;
    }

    public int getHomePage() {
        return homePage;
    }

    public void setHomePage(int homePage) {
        this.homePage = homePage;
    }

    public int getListingPage() {
        return listingPage;
    }

    public void setListingPage(int listingPage) {
        this.listingPage = listingPage;
    }

    public int getProductPage() {
        return productPage;
    }

    public void setProductPage(int productPage) {
        this.productPage = productPage;
    }

    public int getPaymentPage() {
        return paymentPage;
    }

    public void setPaymentPage(int paymentPage) {
        this.paymentPage = paymentPage;
    }

    public int getConfirmationPage() {
        return confirmationPage;
    }

    public void setConfirmationPage(int confirmationPage) {
        this.confirmationPage = confirmationPage;
    }

    /**
     * 判断是否完成转化
     */
    public boolean isConverted() {
        return confirmationPage >= 1;
    }

    /**
     * 获取年龄段
     */
    public String getAgeGroup() {
        if (age < 18) {
            return "under_18";
        } else if (age <= 25) {
            return "18_25";
        } else if (age <= 35) {
            return "26_35";
        } else if (age <= 45) {
            return "36_45";
        } else {
            return "46_plus";
        }
    }

    @Override
    public String toString() {
        return "UserBehaviorWritable{" +
                "isNewUser=" + isNewUser +
                ", age=" + age +
                ", sex='" + sex + '\'' +
                ", market=" + market +
                ", device='" + device + '\'' +
                ", operativeSystem='" + operativeSystem + '\'' +
                ", source='" + source + '\'' +
                ", totalPagesVisited=" + totalPagesVisited +
                ", homePage=" + homePage +
                ", listingPage=" + listingPage +
                ", productPage=" + productPage +
                ", paymentPage=" + paymentPage +
                ", confirmationPage=" + confirmationPage +
                '}';
    }
}
