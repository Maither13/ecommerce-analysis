# 电商平台用户行为分析 - MapReduce 程序

## 项目概述

本项目包含 4 个核心 MapReduce 程序,用于分析电商平台用户行为数据。

## 项目结构

```
mapreduce/
├── pom.xml
└── src/main/java/com/example/mr/
    ├── common/                          # 公共类
    │   ├── UserBehaviorWritable.java    # 用户行为数据 Writable
    │   └── StatResultWritable.java      # 统计结果 Writable
    ├── task1/                           # 任务1: 基础指标统计
    │   ├── BasicMetricsMapper.java
    │   ├── BasicMetricsReducer.java
    │   └── BasicMetricsDriver.java
    ├── task2/                           # 任务2: 多维度行为分析
    │   ├── MultiDimensionMapper.java
    │   ├── MultiDimensionReducer.java
    │   └── MultiDimensionDriver.java
    ├── task3/                           # 任务3: 转化漏斗分析
    │   ├── ConversionFunnelMapper.java
    │   ├── ConversionFunnelReducer.java
    │   └── ConversionFunnelDriver.java
    ├── task4/                           # 任务4: 新老用户对比
    │   ├── UserComparisonMapper.java
    │   ├── UserComparisonReducer.java
    │   └── UserComparisonDriver.java
    ── util/                            # 工具类
        └── DataParser.java
```

## 环境要求

- Java 17+
- Hadoop 3.3.6+
- Maven 3.6+

## 编译打包

```bash
cd mapreduce
mvn clean package
```

生成的 JAR 包位于: `target/ecommerce-mapreduce-1.0.0.jar`

## 运行方式

### 前置准备

1. **启动 Hadoop 集群**
   ```bash
   start-dfs.sh
   start-yarn.sh
   ```

2. **上传数据到 HDFS**
   ```bash
   hdfs dfs -mkdir -p /input
   hdfs dfs -put user_behavior.csv /input/
   ```

### 执行任务

#### 任务 1: 基础指标统计

```bash
hadoop jar target/ecommerce-mapreduce-1.0.0.jar \
  com.example.mr.task1.BasicMetricsDriver \
  /input/user_behavior.csv \
  /output/basic_metrics
```

**输出说明**:
- PV (页面浏览量)
- UV (独立用户数)
- 新用户数、老用户数
- 新用户占比
- 人均访问页数
- 最大/最小访问页数

#### 任务 2: 多维度行为分析

```bash
hadoop jar target/ecommerce-mapreduce-1.0.0.jar \
  com.example.mr.task2.MultiDimensionDriver \
  /input/user_behavior.csv \
  /output/multi_dimension
```

**输出维度**:
- age: 年龄段 (under_18, 18_25, 26_35, 36_45, 46_plus)
- sex: 性别 (Male, Female)
- market: 市场区域 (1, 2, 3, 4)
- device: 设备类型 (mobile, desktop)
- os: 操作系统 (android, iOS, windows, mac, linux, other)
- source: 流量来源 (Direct, Seo, Ads)

**每个维度输出**:
- 用户数
- PV
- 平均访问页数
- 转化用户数
- 转化率

#### 任务 3: 转化漏斗分析

```bash
hadoop jar target/ecommerce-mapreduce-1.0.0.jar \
  com.example.mr.task3.ConversionFunnelDriver \
  /input/user_behavior.csv \
  /output/conversion_funnel
```

**输出内容**:
- 5 个步骤的用户数(首页→列表页→商品页→支付页→确认页)
- 各步骤转化率
- 各步骤流失率
- 整体转化率

**支持筛选维度**:
- ALL: 整体漏斗
- new_user: 按新用户/老用户筛选
- source: 按流量来源筛选
- device: 按设备类型筛选

#### 任务 4: 新老用户对比

```bash
hadoop jar target/ecommerce-mapreduce-1.0.0.jar \
  com.example.mr.task4.UserComparisonDriver \
  /input/user_behavior.csv \
  /output/user_comparison
```

**输出内容**:
- 用户数
- 平均/最大/最小访问页数
- 转化率
- 各页面访问率(首页/列表页/商品页/支付页/确认页)
- 流量来源分布
- 设备类型分布

### 查看结果

```bash
# 查看任务 1 结果
hdfs dfs -cat /output/basic_metrics/part-r-00000

# 查看任务 2 结果
hdfs dfs -cat /output/multi_dimension/part-*

# 查看任务 3 结果
hdfs dfs -cat /output/conversion_funnel/part-*

# 查看任务 4 结果
hdfs dfs -cat /output/user_comparison/part-*
```

## 数据格式

输入文件为 CSV 格式,包含以下字段:

| 字段名 | 类型 | 说明 |
|--------|------|------|
| new_user | int | 是否新用户(0:老用户 1:新用户) |
| age | int | 年龄 |
| sex | string | 性别(Male/Female) |
| market | int | 市场区域(1-4) |
| device | string | 设备类型(mobile/desktop) |
| operative_system | string | 操作系统 |
| source | string | 流量来源(Direct/Seo/Ads) |
| total_pages_visited | int | 总访问页数 |
| home_page | int | 首页访问次数 |
| listing_page | int | 列表页访问次数 |
| product_page | int | 商品页访问次数 |
| payment_page | int | 支付页访问次数 |
| confirmation_page | int | 确认页访问次数 |

## 注意事项

1. **输出目录不能预先存在**,程序会自动删除已存在的输出目录
2. **确保 Hadoop 集群正常运行**,YARN ResourceManager 可访问
3. **数据量较大时**,建议增加 Reduce Task 数量以提高性能
4. **日志输出**会显示在控制台和 YARN 日志中

## 故障排查

### 问题 1: 任务提交失败

**可能原因**:
- Hadoop 集群未启动
- HDFS 权限不足
- JAR 包路径错误

**解决方法**:
```bash
# 检查集群状态
jps

# 检查 HDFS 权限
hdfs dfs -ls /input

# 重新上传 JAR 包
hdfs dfs -put target/ecommerce-mapreduce-1.0.0.jar /jars/
```

### 问题 2: 任务执行缓慢

**优化建议**:
- 增加 Reduce Task 数量
- 使用 Combiner 预聚合(可在 Mapper 中添加)
- 启用数据压缩

### 问题 3: 内存溢出

**解决方法**:
```bash
# 增加 Map/Reduce 任务内存
hadoop jar ... -Dmapreduce.map.memory.mb=2048 -Dmapreduce.reduce.memory.mb=2048 ...
```

## 扩展开发

如需添加新的分析任务,参考以下步骤:

1. 创建新的包 `com.example.mr.taskX`
2. 编写 Mapper、Reducer、Driver 类
3. 在 `pom.xml` 中配置主类
4. 编译打包并测试

## 作者

Lingma AI Assistant

## 许可证

MIT License
