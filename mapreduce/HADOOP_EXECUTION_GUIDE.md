# MapReduce任务在虚拟机Hadoop集群执行指南

##  前置条件

- ✅ 虚拟机IP: `192.168.112.3`
- ✅ Hadoop已安装在虚拟机中(版本 3.3.6+)
- ✅ HDFS和YARN服务已启动
- ✅ 网络连通性已验证(ping成功)

---

##  执行步骤

### 步骤1:在虚拟机中准备数据

SSH连接到虚拟机:
```bash
ssh hadoop@192.168.112.3
# 密码: (您设置的Hadoop用户密码)
```

创建HDFS目录并上传数据:
```bash
# 创建输入输出目录
hdfs dfs -mkdir -p /data/input
hdfs dfs -mkdir -p /data/output

# 上传CSV数据文件(假设文件在当前目录)
hdfs dfs -put user_behavior.csv /data/input/

# 验证上传成功
hdfs dfs -ls /data/input/
```

### 步骤2:编译MapReduce程序

在宿主机(Windows)中编译:
```bash
cd D:\codeSpace\ecommerce-analysis\mapreduce

# 使用Maven打包
mvn clean package

# 生成的jar包位于: target/ecommerce-mapreduce-1.0.0.jar
```

### 步骤3:上传JAR包到虚拟机

方法1: 使用scp命令(在Windows PowerShell中):
```powershell
scp D:\codeSpace\ecommerce-analysis\mapreduce\target\ecommerce-mapreduce-1.0.0.jar hadoop@192.168.112.3:/home/hadoop/
```

方法2: 使用WinSCP等图形化工具传输

### 步骤4:在虚拟机中执行MapReduce任务

SSH连接到虚拟机后执行:

#### 任务1: 基础指标统计
```bash
hadoop jar ecommerce-mapreduce-1.0.0.jar \
  com.example.mr.task1.BasicMetricsDriver \
  /data/input/user_behavior.csv \
  /data/output/basic_metrics_$(date +%Y%m%d_%H%M%S)
```

#### 任务2: 多维度行为分析
```bash
hadoop jar ecommerce-mapreduce-1.0.0.jar \
  com.example.mr.task2.MultiDimensionDriver \
  /data/input/user_behavior.csv \
  /data/output/multi_dimension_$(date +%Y%m%d_%H%M%S)
```

#### 任务3: 转化漏斗分析
```bash
hadoop jar ecommerce-mapreduce-1.0.0.jar \
  com.example.mr.task3.ConversionFunnelDriver \
  /data/input/user_behavior.csv \
  /data/output/conversion_funnel_$(date +%Y%m%d_%H%M%S)
```

#### 任务4: 新老用户对比
```bash
hadoop jar ecommerce-mapreduce-1.0.0.jar \
  com.example.mr.task4.UserComparisonDriver \
  /data/input/user_behavior.csv \
  /data/output/user_comparison_$(date +%Y%m%d_%H%M%S)
```

### 步骤5:查看执行结果

```bash
# 查看输出目录
hdfs dfs -ls /data/output/

# 查看具体任务的输出
hdfs dfs -cat /data/output/basic_metrics_*/part-r-00000

# 下载结果到本地
hdfs dfs -get /data/output/basic_metrics_* ./results/
```

---

##  监控任务执行

### 方法1: YARN ResourceManager Web UI

访问: http://192.168.112.3:8088

可以看到:
- 正在运行的任务
- 已完成的任务
- 任务日志
- 资源使用情况

### 方法2: 命令行监控

```bash
# 查看正在运行的任务
yarn application -list

# 查看特定任务的日志
yarn logs -applicationId <application_id>

# 查看任务状态
yarn application -status <application_id>
```

---

## 📊 将结果导入数据库

MapReduce执行完成后,需要将结果导入MySQL数据库。

### 方法1: 手动导入(推荐用于课程设计)

1. 从HDFS下载结果文件到本地
2. 编写简单的Java程序或Python脚本读取结果
3. 插入到MySQL对应的统计表中

示例SQL(基础指标):
```sql
INSERT INTO stat_basic_metrics (
  data_date, pv, uv, new_user_count, 
  avg_visit_pages, conversion_rate, create_time
) VALUES (
  '2026-06-15', 12345, 6789, 1234,
  5.67, 23.45, NOW()
);
```

### 方法2: 自动导入(需要额外开发)

可以编写一个DataImportService,定期从HDFS读取结果并导入数据库。

---

## ⚙️ Hadoop配置检查

确保虚拟机中的Hadoop配置正确:

### core-site.xml
```xml
<configuration>
  <property>
    <name>fs.defaultFS</name>
    <value>hdfs://192.168.112.3:9000</value>
  </property>
</configuration>
```

### hdfs-site.xml
```xml
<configuration>
  <property>
    <name>dfs.replication</name>
    <value>1</value>
  </property>
  <property>
    <name>dfs.namenode.name.dir</name>
    <value>file:///home/hadoop/hadoop/data/namenode</value>
  </property>
  <property>
    <name>dfs.datanode.data.dir</name>
    <value>file:///home/hadoop/hadoop/data/datanode</value>
  </property>
</configuration>
```

### yarn-site.xml
```xml
<configuration>
  <property>
    <name>yarn.resourcemanager.hostname</name>
    <value>192.168.112.3</value>
  </property>
  <property>
    <name>yarn.nodemanager.aux-services</name>
    <value>mapreduce_shuffle</value>
  </property>
</configuration>
```

### mapred-site.xml
```xml
<configuration>
  <property>
    <name>mapreduce.framework.name</name>
    <value>yarn</value>
  </property>
  <property>
    <name>mapreduce.application.classpath</name>
    <value>$HADOOP_MAPRED_HOME/share/hadoop/mapreduce/*:$HADOOP_MAPRED_HOME/share/hadoop/mapreduce/lib/*</value>
  </property>
</configuration>
```

---

##  常见问题排查

### Q1: Connection refused to HDFS

**原因**: HDFS未启动或防火墙阻止

**解决**:
```bash
# 在虚拟机中检查HDFS状态
jps
# 应该看到 NameNode, DataNode, SecondaryNameNode

# 如果没有,启动HDFS
start-dfs.sh

# 关闭防火墙(临时测试用)
systemctl stop firewalld
```

### Q2: YARN ResourceManager无法连接

**原因**: YARN未启动

**解决**:
```bash
# 启动YARN
start-yarn.sh

# 验证
jps
# 应该看到 ResourceManager, NodeManager
```

### Q3: Out of Memory错误

**原因**: 容器内存不足

**解决**: 调整yarn-site.xml中的内存配置:
```xml
<property>
  <name>yarn.scheduler.minimum-allocation-mb</name>
  <value>512</value>
</property>
<property>
  <name>yarn.scheduler.maximum-allocation-mb</name>
  <value>2048</value>
</property>
```

### Q4: 权限拒绝

**原因**: HDFS权限问题

**解决**:
```bash
# 修改HDFS目录权限
hdfs dfs -chmod -R 777 /data

# 或使用hadoop用户执行
su - hadoop
```

---

## ✅ 执行成功标志

当您看到以下结果时,说明MapReduce任务执行成功:

1. ✅ 命令行显示 "任务执行成功!"
2. ✅ YARN Web UI中任务状态为 SUCCEEDED
3. ✅ HDFS输出目录中存在 part-r-00000 文件
4. ✅ 能够查看统计结果数据

---

## 🎯 课程设计建议

对于课程设计,您可以:

1. **截图保存**:
   - YARN ResourceManager Web UI的任务列表
   - 任务执行日志
   - HDFS输出结果
   
2. **记录关键指标**:
   - Map任务数
   - Reduce任务数
   - 执行时间
   - 处理的数据量

3. **写入报告**:
   - 描述MapReduce算法设计
   - 展示执行结果截图
   - 分析性能表现

---

## 📞 需要帮助?

如果在虚拟机Hadoop上执行遇到问题,请提供:
1. 完整的错误信息
2. YARN任务日志
3. Hadoop版本: `hadoop version`

我会帮您快速解决!

---

**祝您MapReduce任务执行顺利!** 🚀
