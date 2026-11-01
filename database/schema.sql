-- =============================================
-- 电商平台用户行为分析系统 - 数据库建表脚本
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- 创建日期: 2026-06-09
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ecommerce_user_behavior DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ecommerce_user_behavior;

-- =============================================
-- 1. 用户管理相关表
-- =============================================

-- 1.1 角色表
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码(唯一)',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0:禁用 1:启用)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 1.2 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名(唯一)',
    password VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0:禁用 1:启用)',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除(0:未删除 1:已删除)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 1.3 用户角色关联表
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 1.4 用户扩展表
DROP TABLE IF EXISTS user_profile;
CREATE TABLE user_profile (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID(关联sys_user)',
    real_name VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    gender TINYINT DEFAULT NULL COMMENT '性别(0:未知 1:男 2:女)',
    birthday DATE DEFAULT NULL COMMENT '生日',
    department VARCHAR(100) DEFAULT NULL COMMENT '部门',
    position VARCHAR(100) DEFAULT NULL COMMENT '职位',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户扩展表';

-- =============================================
-- 2. MapReduce任务管理相关表
-- =============================================

-- 2.1 MapReduce任务日志表
DROP TABLE IF EXISTS mr_job_log;
CREATE TABLE mr_job_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    job_id VARCHAR(50) NOT NULL COMMENT '任务ID(唯一)',
    job_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    job_type VARCHAR(50) NOT NULL COMMENT '任务类型(basic/multi_dimension/funnel/comparison)',
    user_id BIGINT DEFAULT NULL COMMENT '提交用户ID(NULL表示系统任务)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING/RUNNING/SUCCESS/FAILED/CANCELLED)',
    input_path VARCHAR(500) DEFAULT NULL COMMENT '输入路径(HDFS)',
    output_path VARCHAR(500) DEFAULT NULL COMMENT '输出路径(HDFS)',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    duration BIGINT DEFAULT NULL COMMENT '耗时(秒)',
    progress INT NOT NULL DEFAULT 0 COMMENT '进度(0-100)',
    error_msg TEXT DEFAULT NULL COMMENT '错误信息',
    submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_id (job_id),
    KEY idx_job_type (job_type),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_submit_time (submit_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MapReduce任务日志表';

-- 2.2 任务结果缓存表
DROP TABLE IF EXISTS job_result;
CREATE TABLE job_result (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    job_id VARCHAR(50) NOT NULL COMMENT '任务ID',
    result_key VARCHAR(100) NOT NULL COMMENT '结果键',
    result_data JSON DEFAULT NULL COMMENT '结果数据(JSON格式)',
    result_path VARCHAR(500) DEFAULT NULL COMMENT '结果文件路径(HDFS/MinIO)',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_job_id (job_id),
    KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务结果缓存表';

-- =============================================
-- 3. 原始数据表
-- =============================================

-- 3.1 原始用户行为数据表
DROP TABLE IF EXISTS raw_data;
CREATE TABLE raw_data (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    new_user TINYINT NOT NULL COMMENT '是否新用户(0:老用户 1:新用户)',
    age INT NOT NULL COMMENT '年龄',
    sex VARCHAR(10) NOT NULL COMMENT '性别(Male/Female)',
    market INT NOT NULL COMMENT '市场区域(1-4)',
    device VARCHAR(20) NOT NULL COMMENT '设备类型(mobile/desktop)',
    operative_system VARCHAR(20) NOT NULL COMMENT '操作系统',
    source VARCHAR(20) NOT NULL COMMENT '流量来源(Direct/Seo/Ads)',
    total_pages_visited INT NOT NULL COMMENT '总访问页数',
    home_page INT NOT NULL COMMENT '首页访问次数',
    listing_page INT NOT NULL COMMENT '列表页访问次数',
    product_page INT NOT NULL COMMENT '商品页访问次数',
    payment_page INT NOT NULL COMMENT '支付页访问次数',
    confirmation_page INT NOT NULL COMMENT '确认页访问次数',
    import_batch VARCHAR(50) DEFAULT NULL COMMENT '导入批次号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_new_user (new_user),
    KEY idx_age (age),
    KEY idx_sex (sex),
    KEY idx_market (market),
    KEY idx_device (device),
    KEY idx_source (source),
    KEY idx_import_batch (import_batch)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原始用户行为数据表';

-- =============================================
-- 4. 统计分析结果表
-- =============================================

-- 4.1 基础指标统计表
DROP TABLE IF EXISTS stat_basic_metrics;
CREATE TABLE stat_basic_metrics (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    pv BIGINT NOT NULL DEFAULT 0 COMMENT '页面浏览量',
    uv BIGINT NOT NULL DEFAULT 0 COMMENT '独立用户数',
    new_user_count BIGINT NOT NULL DEFAULT 0 COMMENT '新用户数',
    old_user_count BIGINT NOT NULL DEFAULT 0 COMMENT '老用户数',
    new_user_ratio DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '新用户占比',
    avg_pages DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '人均访问页数',
    total_pages_sum BIGINT NOT NULL DEFAULT 0 COMMENT '总访问页数',
    max_pages INT NOT NULL DEFAULT 0 COMMENT '最大访问页数',
    min_pages INT NOT NULL DEFAULT 0 COMMENT '最小访问页数',
    job_id VARCHAR(50) DEFAULT NULL COMMENT '关联任务ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stat_date (stat_date),
    KEY idx_job_id (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基础指标统计表';

-- 4.2 多维度行为分析表
DROP TABLE IF EXISTS stat_multi_dimension;
CREATE TABLE stat_multi_dimension (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    dimension_type VARCHAR(20) NOT NULL COMMENT '维度类型(age/sex/market/device/os/source)',
    dimension_value VARCHAR(50) NOT NULL COMMENT '维度值',
    user_count BIGINT NOT NULL DEFAULT 0 COMMENT '用户数',
    pv BIGINT NOT NULL DEFAULT 0 COMMENT '页面浏览量',
    avg_pages DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '平均访问页数',
    converted_count BIGINT NOT NULL DEFAULT 0 COMMENT '转化用户数',
    conversion_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '转化率',
    stat_date DATE NOT NULL COMMENT '统计日期',
    job_id VARCHAR(50) DEFAULT NULL COMMENT '关联任务ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_dimension (dimension_type, dimension_value),
    KEY idx_stat_date (stat_date),
    KEY idx_job_id (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多维度行为分析表';

-- 4.3 转化漏斗步骤表
DROP TABLE IF EXISTS stat_funnel_step;
CREATE TABLE stat_funnel_step (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    step_name VARCHAR(50) NOT NULL COMMENT '步骤名称(home/listing/product/payment/confirmation)',
    step_order INT NOT NULL COMMENT '步骤顺序(1-5)',
    user_count BIGINT NOT NULL DEFAULT 0 COMMENT '该步骤用户数',
    conversion_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '相对于上一步的转化率',
    loss_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '相对于上一步的流失率',
    overall_conversion_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '整体转化率',
    filter_type VARCHAR(20) DEFAULT NULL COMMENT '筛选类型(new_user/source/device/ALL)',
    filter_value VARCHAR(50) DEFAULT NULL COMMENT '筛选值',
    stat_date DATE NOT NULL COMMENT '统计日期',
    job_id VARCHAR(50) DEFAULT NULL COMMENT '关联任务ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_step_order (step_order),
    KEY idx_filter (filter_type, filter_value),
    KEY idx_stat_date (stat_date),
    KEY idx_job_id (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转化漏斗步骤表';

-- 4.4 新老用户对比统计表
DROP TABLE IF EXISTS stat_user_comparison;
CREATE TABLE stat_user_comparison (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_type VARCHAR(10) NOT NULL COMMENT '用户类型(new/old)',
    metric_category VARCHAR(50) NOT NULL COMMENT '指标类别(basic/pages_preference/conversion/source/device)',
    metric_name VARCHAR(100) NOT NULL COMMENT '指标名称',
    metric_value DECIMAL(15,4) NOT NULL DEFAULT 0.0000 COMMENT '指标值',
    metric_unit VARCHAR(20) DEFAULT NULL COMMENT '指标单位',
    stat_date DATE NOT NULL COMMENT '统计日期',
    job_id VARCHAR(50) DEFAULT NULL COMMENT '关联任务ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_type (user_type, metric_category),
    KEY idx_stat_date (stat_date),
    KEY idx_job_id (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新老用户对比统计表';

-- =============================================
-- 5. 报表和缓存管理表
-- =============================================

-- 5.1 报表配置表
DROP TABLE IF EXISTS stat_report_config;
CREATE TABLE stat_report_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    report_code VARCHAR(50) NOT NULL COMMENT '报表编码(唯一)',
    report_name VARCHAR(100) NOT NULL COMMENT '报表名称',
    report_type VARCHAR(50) NOT NULL COMMENT '报表类型(table/chart/dashboard)',
    data_source VARCHAR(100) NOT NULL COMMENT '数据来源',
    query_sql TEXT DEFAULT NULL COMMENT '查询SQL',
    chart_config JSON DEFAULT NULL COMMENT '图表配置(ECharts option)',
    refresh_interval INT DEFAULT 0 COMMENT '刷新间隔(秒,0表示不自动刷新)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0:禁用 1:启用)',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    create_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_code (report_code),
    KEY idx_status (status),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表配置表';

-- 5.2 缓存记录表
DROP TABLE IF EXISTS cache_record;
CREATE TABLE cache_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    cache_key VARCHAR(200) NOT NULL COMMENT '缓存键',
    cache_type VARCHAR(50) NOT NULL COMMENT '缓存类型(api/result/session)',
    cache_value TEXT DEFAULT NULL COMMENT '缓存值',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
    hit_count BIGINT NOT NULL DEFAULT 0 COMMENT '命中次数',
    miss_count BIGINT NOT NULL DEFAULT 0 COMMENT '未命中次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_access_time DATETIME DEFAULT NULL COMMENT '最后访问时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cache_key (cache_key),
    KEY idx_cache_type (cache_type),
    KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缓存记录表';

-- 5.3 数据导入日志表
DROP TABLE IF EXISTS data_import_log;
CREATE TABLE data_import_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    batch_no VARCHAR(50) NOT NULL COMMENT '批次号(唯一)',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    record_count INT NOT NULL DEFAULT 0 COMMENT '记录数',
    success_count INT NOT NULL DEFAULT 0 COMMENT '成功导入数',
    fail_count INT NOT NULL DEFAULT 0 COMMENT '失败数',
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT '状态(PROCESSING/SUCCESS/PARTIAL/FAILED)',
    error_msg TEXT DEFAULT NULL COMMENT '错误信息',
    import_by BIGINT DEFAULT NULL COMMENT '导入人ID',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    duration BIGINT DEFAULT NULL COMMENT '耗时(秒)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_no (batch_no),
    KEY idx_status (status),
    KEY idx_import_by (import_by),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据导入日志表';
