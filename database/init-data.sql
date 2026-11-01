-- =============================================
-- 电商平台用户行为分析系统 - 初始化数据脚本
-- 创建日期: 2026-06-09
-- 说明: 本脚本可重复执行，不会产生重复数据
-- =============================================

USE ecommerce_user_behavior;

-- =============================================
-- 1. 插入默认角色
-- =============================================
INSERT IGNORE INTO sys_role (role_code, role_name, description, status) VALUES
('ROLE_ADMIN', '管理员', '系统管理员,拥有所有权限', 1),
('ROLE_USER', '普通用户', '普通用户,只能查看和提交任务', 1);

-- =============================================
-- 2. 插入默认管理员账户
-- 密码: admin123 (BCrypt加密后的值)
-- =============================================
INSERT IGNORE INTO sys_user (username, password, nickname, email, status) VALUES
('admin', '$2a$10$mua0T6kJaZmRBtz5rLheeuKmrUJMSzRwhU.GuuP9351IdObPRDT1G', '系统管理员', 'admin@example.com', 1);

-- 分配管理员角色
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.role_code = 'ROLE_ADMIN';

-- =============================================
-- 3. 插入示例普通用户
-- 密码: user123
-- =============================================
INSERT IGNORE INTO sys_user (username, password, nickname, email, phone, status) VALUES
('user001', '$2a$10$L6B3aP5JFQJGMJ02YNnsOOFHTnXrzVHgr55ocOAWJWX18t0RWO9x6', '张三', 'user001@example.com', '13800138001', 1),
('user002', '$2a$10$L6B3aP5JFQJGMJ02YNnsOOFHTnXrzVHgr55ocOAWJWX18t0RWO9x6', '李四', 'user002@example.com', '13800138002', 1),
('user003', '$2a$10$L6B3aP5JFQJGMJ02YNnsOOFHTnXrzVHgr55ocOAWJWX18t0RWO9x6', '王五', 'user003@example.com', '13800138003', 1);

-- 分配普通用户角色
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username IN ('user001', 'user002', 'user003') AND r.role_code = 'ROLE_USER';

-- =============================================
-- 4. 插入默认报表配置(8张自定义报表)
-- =============================================
INSERT IGNORE INTO stat_report_config (report_code, report_name, report_type, data_source, query_sql, refresh_interval, status, sort_order) VALUES
('RPT_BASIC_OVERVIEW', '基础指标概览', 'dashboard', 'stat_basic_metrics',
 'SELECT * FROM stat_basic_metrics ORDER BY stat_date DESC LIMIT 1',
 300, 1, 1),

('RPT_PV_UV_TREND', 'PV/UV 趋势分析', 'chart', 'stat_basic_metrics',
 'SELECT stat_date, pv, uv FROM stat_basic_metrics ORDER BY stat_date',
 0, 1, 2),

('RPT_NEW_USER_RATIO', '新用户占比分析', 'chart', 'stat_basic_metrics',
 'SELECT stat_date, new_user_ratio FROM stat_basic_metrics ORDER BY stat_date',
 0, 1, 3),

('RPT_AGE_DISTRIBUTION', '年龄分布统计', 'chart', 'stat_multi_dimension',
 'SELECT dimension_value, user_count, conversion_rate FROM stat_multi_dimension WHERE dimension_type=''age'' ORDER BY dimension_value',
 0, 1, 4),

('RPT_CONVERSION_FUNNEL', '转化漏斗分析', 'chart', 'stat_funnel_step',
 'SELECT step_name, step_order, user_count, conversion_rate, loss_rate FROM stat_funnel_step WHERE filter_type=''ALL'' ORDER BY step_order',
 0, 1, 5),

('RPT_USER_COMPARISON', '新老用户对比', 'table', 'stat_user_comparison',
 'SELECT user_type, metric_category, metric_name, metric_value FROM stat_user_comparison ORDER BY user_type, metric_category',
 0, 1, 6),

('RPT_SOURCE_ANALYSIS', '流量来源分析', 'chart', 'stat_multi_dimension',
 'SELECT dimension_value, user_count, pv, conversion_rate FROM stat_multi_dimension WHERE dimension_type=''source'' ORDER BY user_count DESC',
 0, 1, 7),

('RPT_DEVICE_ANALYSIS', '设备类型分析', 'chart', 'stat_multi_dimension',
 'SELECT dimension_value, user_count, conversion_rate FROM stat_multi_dimension WHERE dimension_type=''device'' ORDER BY user_count DESC',
 0, 1, 8);

-- =============================================
-- 5. 插入示例统计数据(用于测试展示)
-- =============================================

-- 5.1 基础指标示例数据 (uk_stat_date 防重)
INSERT IGNORE INTO stat_basic_metrics (stat_date, pv, uv, new_user_count, old_user_count, new_user_ratio, avg_pages, total_pages_sum, max_pages, min_pages) VALUES
('2026-06-01', 450000, 90000, 27000, 63000, 0.3000, 5.00, 450000, 50, 1),
('2026-06-02', 480000, 95000, 28500, 66500, 0.3000, 5.05, 479750, 52, 1),
('2026-06-03', 520000, 100000, 30000, 70000, 0.3000, 5.20, 520000, 55, 1),
('2026-06-04', 490000, 98000, 29400, 68600, 0.3000, 5.00, 490000, 48, 1),
('2026-06-05', 510000, 102000, 30600, 71400, 0.3000, 5.00, 510000, 53, 1),
('2026-06-06', 530000, 105000, 31500, 73500, 0.3000, 5.05, 530250, 56, 1),
('2026-06-07', 550000, 110000, 33000, 77000, 0.3000, 5.00, 550000, 58, 1),
('2026-06-08', 500000, 100000, 30000, 70000, 0.3000, 5.00, 500000, 50, 1);

-- 5.2 多维度分析示例数据 (无唯一键，先清除再插入)
DELETE FROM stat_multi_dimension WHERE stat_date = '2026-06-08';
INSERT INTO stat_multi_dimension (dimension_type, dimension_value, user_count, pv, avg_pages, converted_count, conversion_rate, stat_date) VALUES
('age', '18_25', 35000, 175000, 5.00, 3500, 0.1000, '2026-06-08'),
('age', '26_35', 40000, 200000, 5.00, 4400, 0.1100, '2026-06-08'),
('age', '36_45', 20000, 100000, 5.00, 2000, 0.1000, '2026-06-08'),
('age', '46_plus', 5000, 25000, 5.00, 400, 0.0800, '2026-06-08');

-- 性别分布
INSERT INTO stat_multi_dimension (dimension_type, dimension_value, user_count, pv, avg_pages, converted_count, conversion_rate, stat_date) VALUES
('sex', 'Male', 48000, 240000, 5.00, 5040, 0.1050, '2026-06-08'),
('sex', 'Female', 52000, 260000, 5.00, 5460, 0.1050, '2026-06-08');

-- 设备类型
INSERT INTO stat_multi_dimension (dimension_type, dimension_value, user_count, pv, avg_pages, converted_count, conversion_rate, stat_date) VALUES
('device', 'mobile', 65000, 325000, 5.00, 6825, 0.1050, '2026-06-08'),
('device', 'desktop', 35000, 175000, 5.00, 3675, 0.1050, '2026-06-08');

-- 流量来源
INSERT INTO stat_multi_dimension (dimension_type, dimension_value, user_count, pv, avg_pages, converted_count, conversion_rate, stat_date) VALUES
('source', 'Direct', 50000, 250000, 5.00, 6000, 0.1200, '2026-06-08'),
('source', 'Seo', 35000, 175000, 5.00, 3150, 0.0900, '2026-06-08'),
('source', 'Ads', 15000, 75000, 5.00, 1350, 0.0900, '2026-06-08');

-- 5.3 转化漏斗示例数据 (无唯一键，先清除再插入)
DELETE FROM stat_funnel_step WHERE stat_date = '2026-06-08';
INSERT INTO stat_funnel_step (step_name, step_order, user_count, conversion_rate, loss_rate, overall_conversion_rate, filter_type, stat_date) VALUES
('home_page', 1, 100000, 1.0000, 0.0000, 1.0000, 'ALL', '2026-06-08'),
('listing_page', 2, 65000, 0.6500, 0.3500, 0.6500, 'ALL', '2026-06-08'),
('product_page', 3, 45000, 0.6923, 0.3077, 0.4500, 'ALL', '2026-06-08'),
('payment_page', 4, 25000, 0.5556, 0.4444, 0.2500, 'ALL', '2026-06-08'),
('confirmation_page', 5, 10500, 0.4200, 0.5800, 0.1050, 'ALL', '2026-06-08');

-- 5.4 新老用户对比示例数据 (无唯一键，先清除再插入)
DELETE FROM stat_user_comparison WHERE stat_date = '2026-06-08';
INSERT INTO stat_user_comparison (user_type, metric_category, metric_name, metric_value, metric_unit, stat_date) VALUES
-- 新用户基础指标
('new', 'basic', 'user_count', 30000, '人', '2026-06-08'),
('new', 'basic', 'avg_pages', 4.50, '页', '2026-06-08'),
('new', 'basic', 'conversion_rate', 0.0800, '%', '2026-06-08'),

-- 老用户基础指标
('old', 'basic', 'user_count', 70000, '人', '2026-06-08'),
('old', 'basic', 'avg_pages', 5.20, '页', '2026-06-08'),
('old', 'basic', 'conversion_rate', 0.1150, '%', '2026-06-08'),

-- 新用户页面偏好
('new', 'pages_preference', 'home_page_rate', 1.0000, '%', '2026-06-08'),
('new', 'pages_preference', 'listing_page_rate', 0.6000, '%', '2026-06-08'),
('new', 'pages_preference', 'product_page_rate', 0.4000, '%', '2026-06-08'),
('new', 'pages_preference', 'payment_page_rate', 0.2000, '%', '2026-06-08'),
('new', 'pages_preference', 'confirmation_page_rate', 0.0800, '%', '2026-06-08'),

-- 老用户页面偏好
('old', 'pages_preference', 'home_page_rate', 1.0000, '%', '2026-06-08'),
('old', 'pages_preference', 'listing_page_rate', 0.6800, '%', '2026-06-08'),
('old', 'pages_preference', 'product_page_rate', 0.4800, '%', '2026-06-08'),
('old', 'pages_preference', 'payment_page_rate', 0.2800, '%', '2026-06-08'),
('old', 'pages_preference', 'confirmation_page_rate', 0.1150, '%', '2026-06-08'),

-- 新用户流量来源
('new', 'source', 'direct_rate', 0.4000, '%', '2026-06-08'),
('new', 'source', 'seo_rate', 0.3500, '%', '2026-06-08'),
('new', 'source', 'ads_rate', 0.2500, '%', '2026-06-08'),

-- 老用户流量来源
('old', 'source', 'direct_rate', 0.5500, '%', '2026-06-08'),
('old', 'source', 'seo_rate', 0.3500, '%', '2026-06-08'),
('old', 'source', 'ads_rate', 0.1000, '%', '2026-06-08');

-- =============================================
-- 6. 验证数据插入
-- =============================================
SELECT '角色表数据:' AS info;
SELECT * FROM sys_role;

SELECT '用户表数据:' AS info;
SELECT id, username, nickname, email, status FROM sys_user;

SELECT '报表配置数据:' AS info;
SELECT report_code, report_name, report_type, status FROM stat_report_config;

SELECT '基础指标数据:' AS info;
SELECT stat_date, pv, uv, new_user_ratio, avg_pages FROM stat_basic_metrics;

SELECT '转化漏斗数据:' AS info;
SELECT step_name, step_order, user_count, conversion_rate, loss_rate FROM stat_funnel_step WHERE filter_type='ALL' ORDER BY step_order;
