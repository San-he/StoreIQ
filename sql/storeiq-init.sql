/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306_1
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : storeiq

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 01/08/2026 19:41:33
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. sys_user — 系统用户
-- ============================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(50)   NOT NULL COMMENT '登录账号',
  `password`    VARCHAR(255)  NOT NULL COMMENT '密码',
  `nickname`    VARCHAR(50)   NOT NULL COMMENT '显示名称',
  `phone`       VARCHAR(20)   DEFAULT NULL COMMENT '手机号',
  `email`       VARCHAR(100)  DEFAULT NULL COMMENT '邮箱',
  `role`        VARCHAR(20)   NOT NULL DEFAULT 'viewer' COMMENT '角色: superadmin/admin/manager/viewer',
  `avatar`      VARCHAR(255)  DEFAULT NULL COMMENT '头像URL',
  `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '0=禁用, 1=启用',
  `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

INSERT INTO `sys_user` VALUES
(1, 'superadmin', '123456', '超级管理员', '18800000001', 'superadmin@storeiq.cn', 'superadmin', NULL, 1, NOW()),
(2, 'admin',     '123456', '管理员李明',   '18800000002', 'admin@storeiq.cn',     'admin',      NULL, 1, NOW()),
(3, 'manager01', '123456', '店长王芳',     '18800000003', 'wangfang@storeiq.cn',  'manager',    NULL, 1, NOW()),
(4, 'manager02', '123456', '店长张伟',     '18800000004', 'zhangwei@storeiq.cn',  'manager',    NULL, 1, NOW()),
(5, 'viewer01',  '123456', '观察员赵六',   '18800000005', 'zhaoliu@storeiq.cn',   'viewer',     NULL, 1, NOW());

-- ============================================================
-- 2. store — 店铺
-- ============================================================
DROP TABLE IF EXISTS `store`;
CREATE TABLE `store` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`           VARCHAR(100)  NOT NULL COMMENT '店铺名称',
  `code`           VARCHAR(50)   NOT NULL COMMENT '店铺编码',
  `address`        VARCHAR(255)  DEFAULT NULL COMMENT '详细地址',
  `longitude`      DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
  `latitude`       DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
  `region`         VARCHAR(50)   DEFAULT NULL COMMENT '所属区域',
  `capacity`       INT           DEFAULT 0 COMMENT '总容量（件）',
  `property_type`  VARCHAR(20)   DEFAULT 'lease' COMMENT '租赁(lease) / 购买(owned)',
  `monthly_rent`   DECIMAL(10,2) DEFAULT NULL COMMENT '月租金（租赁时填写）',
  `purchase_cost`  DECIMAL(12,2) DEFAULT NULL COMMENT '购买本金（购买时填写）',
  `contact_person` VARCHAR(50)   DEFAULT NULL COMMENT '联系人',
  `contact_phone`  VARCHAR(20)   DEFAULT NULL COMMENT '联系电话',
  `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '0=停用, 1=运营中',
  `description`    TEXT          DEFAULT NULL COMMENT '备注',
  `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺';

INSERT INTO `store` VALUES
(1,  '北京朝阳SKP店',   'BJ-001', '北京市朝阳区建国路87号SKP商场B1层',        116.4735, 39.9087, '华北', 500, 'owned',  NULL,       2800000.00, '王芳', '18800000003', 1, '旗舰店，自有产权', NOW(), NOW()),
(2,  '上海陆家嘴店',     'SH-001', '上海市浦东新区陆家嘴环路1366号',           121.4998, 31.2352, '华东', 400, 'lease',  15000.00,   NULL,       '王芳', '18800000003', 1, '核心商圈，人流量大', NOW(), NOW()),
(3,  '深圳华强北店',     'SZ-001', '深圳市福田区华强北路1002号赛格广场1层',    114.0859, 22.5446, '华南', 600, 'lease',  12000.00,   NULL,       '张伟', '18800000004', 1, '电子商圈核心位置', NOW(), NOW()),
(4,  '广州天河店',       'GZ-001', '广州市天河区天河路385号太古汇B2层',        113.3295, 23.1255, '华南', 350, 'owned',  NULL,       2200000.00, '张伟', '18800000004', 1, '新开业店铺', NOW(), NOW()),
(5,  '成都春熙路店',     'CD-001', '成都市锦江区春熙路北段23号',               104.0815, 30.6598, '西南', 300, 'lease',  8000.00,    NULL,       '李明', '18800000002', 1, '西南区域首店', NOW(), NOW()),
(6,  '武汉江汉路店',     'WH-001', '武汉市江汉区江汉路步行街126号',            114.2986, 30.5899, '华中', 300, 'lease',  9000.00,    NULL,       '李明', '18800000002', 1, NULL, NOW(), NOW()),
(7,  '杭州西湖店',       'HZ-001', '杭州市上城区延安路258号湖滨银泰in77 C区',  120.1688, 30.2468, '华东', 350, 'lease',  11000.00,   NULL,       '王芳', '18800000003', 1, '西湖商圈，游客消费力强', NOW(), NOW()),
(8,  '南京新街口店',     'NJ-001', '南京市秦淮区中山南路1号新街口百货B1层',    118.7842, 32.0415, '华东', 280, 'owned',  NULL,       1850000.00, '张伟', '18800000004', 1, NULL, NOW(), NOW()),
(9,  '西安钟楼店',       'XA-001', '西安市碑林区南大街2号开元商城1层',         108.9480, 34.2608, '西北', 250, 'lease',  7000.00,    NULL,       '李明', '18800000002', 1, '西北区域首店', NOW(), NOW()),
(10, '沈阳中街店',       'SY-001', '沈阳市沈河区中街路205号恒隆广场1层',       123.4590, 41.8012, '东北', 250, 'lease',  7500.00,    NULL,       '李明', '18800000002', 1, '待开业', NOW(), NOW());

-- ============================================================
-- 3. product_category — 商品分类
-- ============================================================
DROP TABLE IF EXISTS `product_category`;
CREATE TABLE `product_category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(50)  NOT NULL COMMENT '分类名称',
  `code`        VARCHAR(30)  NOT NULL COMMENT '分类编码',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

INSERT INTO `product_category` VALUES
(1, '智能手机',   'PHONE',      '各品牌智能手机', NOW()),
(2, '笔记本电脑', 'LAPTOP',     '笔记本及台式机', NOW()),
(3, '平板电脑',   'TABLET',     '平板电脑及电子阅读器', NOW()),
(4, '穿戴设备',   'WEARABLE',   '智能手表/手环/耳机', NOW()),
(5, '智能家居',   'SMART_HOME', '智能音箱/扫地机器人/智能门锁等', NOW());

-- ============================================================
-- 4. product — 商品（SKU档案）
-- ============================================================
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`         VARCHAR(100)  NOT NULL COMMENT '商品名称',
  `code`         VARCHAR(50)   NOT NULL COMMENT '商品编码/SKU',
  `category_id`  BIGINT        NOT NULL COMMENT '分类ID',
  `brand`        VARCHAR(50)   DEFAULT NULL COMMENT '品牌',
  `model`        VARCHAR(50)   DEFAULT NULL COMMENT '型号',
  `unit`         VARCHAR(20)   DEFAULT '台' COMMENT '单位',
  `cost_price`   DECIMAL(10,2) DEFAULT NULL COMMENT '参考成本价',
  `sale_price`   DECIMAL(10,2) DEFAULT NULL COMMENT '建议售价',
  `safety_stock` INT           DEFAULT 10 COMMENT '安全库存预警阈值',
  `image`        VARCHAR(255)  DEFAULT NULL COMMENT '商品图片URL',
  `description`  TEXT          DEFAULT NULL COMMENT '商品描述',
  `status`       TINYINT       NOT NULL DEFAULT 1 COMMENT '0=下架, 1=上架',
  `create_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品';

INSERT INTO `product` VALUES
-- 智能手机
(1,  'iPhone 15 Pro Max',  'SKU-PH-001', 1, 'Apple',   'A2849',   '台', 8200.00,  9999.00,  5,  NULL, '苹果旗舰机型，A17 Pro芯片，钛金属设计', 1, NOW(), NOW()),
(2,  'iPhone 15',          'SKU-PH-002', 1, 'Apple',   'A2846',   '台', 5200.00,  6999.00,  5,  NULL, '苹果标准旗舰，A16芯片，灵动岛设计', 1, NOW(), NOW()),
(3,  'Samsung Galaxy S24', 'SKU-PH-003', 1, 'Samsung', 'SM-S9210', '台', 4800.00, 6499.00,  5,  NULL, '三星旗舰，骁龙8 Gen3，AI智能功能', 1, NOW(), NOW()),
(4,  'Xiaomi 14 Pro',      'SKU-PH-004', 1, '小米',    '2311DRK48C', '台', 3800.00, 4999.00, 8, NULL, '小米旗舰，骁龙8 Gen3，徕卡光学镜头', 1, NOW(), NOW()),
-- 笔记本电脑
(5,  'MacBook Pro 14"',    'SKU-LP-001', 2, 'Apple',   'A2918',   '台', 12800.00, 14999.00, 3, NULL, 'M3 Pro芯片，14英寸Liquid Retina XDR', 1, NOW(), NOW()),
(6,  'ThinkPad X1 Carbon', 'SKU-LP-002', 2, 'Lenovo',  '21HM',    '台', 8500.00,  10999.00, 3, NULL, '商务旗舰，i7-1365U，14英寸2.8K OLED', 1, NOW(), NOW()),
(7,  'MateBook X Pro',     'SKU-LP-003', 2, '华为',    'MRGFG-16', '台', 7800.00, 9999.00,  3, NULL, '华为旗舰轻薄本，3.1K OLED原色屏', 1, NOW(), NOW()),
-- 平板电脑
(8,  'iPad Air 11"',       'SKU-TB-001', 3, 'Apple',   'A2899',   '台', 3800.00,  5499.00,  5,  NULL, 'M2芯片，11英寸Liquid Retina', 1, NOW(), NOW()),
(9,  'MatePad Pro 13.2"',  'SKU-TB-002', 3, '华为',    'PCE-W30', '台', 3500.00,  5199.00,  5,  NULL, '13.2英寸OLED柔性屏，天生会画', 1, NOW(), NOW()),
-- 穿戴设备
(10, 'Apple Watch Ultra 2','SKU-WE-001', 4, 'Apple',   'A2986',   '台', 4800.00,  6499.00,  5,  NULL, '49mm钛金属表壳，运动探险旗舰', 1, NOW(), NOW()),
(11, 'AirPods Pro 2',      'SKU-WE-002', 4, 'Apple',   'A2931',   '台', 1400.00,  1999.00,  10, NULL, 'H2芯片，自适应降噪，USB-C接口', 1, NOW(), NOW()),
(12, 'Xiaomi Band 8 Pro',  'SKU-WE-003', 4, '小米',    'M2341B1', '台', 300.00,   499.00,   20, NULL, '1.74英寸AMOLED屏，150+运动模式', 1, NOW(), NOW()),
-- 智能家居
(13, 'Xiaomi 扫地机器人X20+', 'SKU-SH-001', 5, '小米',  'B106GL',  '台', 1800.00, 2499.00,  10, NULL, '自清洁扫地拖地一体，LDS激光导航', 1, NOW(), NOW()),
(14, 'HomePod mini',       'SKU-SH-002', 5, 'Apple',   'A2374',   '台', 600.00,   849.00,   15, NULL, 'S5芯片，360°环绕音频，智能家居中枢', 1, NOW(), NOW()),
(15, 'Xiaomi 智能门锁Pro', 'SKU-SH-003', 5, '小米',    'MJZNMS01LM', '台', 900.00, 1299.00, 10, NULL, '3D结构光人脸识别，指纹密码NFC六合一', 1, NOW(), NOW());

-- ============================================================
-- 5. inventory — 库存快照
-- ============================================================
DROP TABLE IF EXISTS `inventory`;
CREATE TABLE `inventory` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id`    BIGINT   NOT NULL COMMENT '店铺ID',
  `product_id`  BIGINT   NOT NULL COMMENT '商品ID',
  `quantity`    INT      NOT NULL DEFAULT 0 COMMENT '当前库存数量',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后变动时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_store_product` (`store_id`, `product_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存快照';

INSERT INTO `inventory` VALUES
-- 北京朝阳SKP店 (store_id=1)
(1,   1, 1,  30, NOW()), (2,   1, 2,  45, NOW()), (3,   1, 3,  20, NOW()),
(4,   1, 4,  35, NOW()), (5,   1, 5,  10, NOW()), (6,   1, 6,  15, NOW()),
(7,   1, 7,  12, NOW()), (8,   1, 8,  25, NOW()), (9,   1, 9,  18, NOW()),
(10,  1, 10, 15, NOW()), (11,  1, 11, 60, NOW()), (12,  1, 12, 100, NOW()),
(13,  1, 13, 20, NOW()), (14,  1, 14, 40, NOW()), (15,  1, 15, 10, NOW()),
-- 上海陆家嘴店 (store_id=2)
(16,  2, 1,  25, NOW()), (17,  2, 2,  40, NOW()), (18,  2, 3,  18, NOW()),
(19,  2, 4,  30, NOW()), (20,  2, 5,  8,  NOW()), (21,  2, 8,  20, NOW()),
(22,  2, 10, 12, NOW()), (23,  2, 11, 55, NOW()), (24,  2, 13, 15, NOW()),
-- 深圳华强北店 (store_id=3)
(25,  3, 1,  40, NOW()), (26,  3, 2,  55, NOW()), (27,  3, 3,  30, NOW()),
(28,  3, 4,  50, NOW()), (29,  3, 5,  12, NOW()), (30,  3, 6,  20, NOW()),
(31,  3, 7,  15, NOW()), (32,  3, 8,  35, NOW()), (33,  3, 9,  25, NOW()),
(34,  3, 10, 20, NOW()), (35,  3, 11, 80, NOW()), (36,  3, 12, 150, NOW()),
(37,  3, 13, 30, NOW()), (38,  3, 14, 55, NOW()), (39,  3, 15, 18, NOW()),
-- 广州天河店 (store_id=4)
(40,  4, 1,  18, NOW()), (41,  4, 2,  30, NOW()), (42,  4, 4,  22, NOW()),
(43,  4, 7,  10, NOW()), (44,  4, 8,  15, NOW()), (45,  4, 11, 35, NOW()),
(46,  4, 12, 80, NOW()), (47,  4, 14, 30, NOW()),
-- 成都春熙路店 (store_id=5)
(48,  5, 2,  35, NOW()), (49,  5, 4,  28, NOW()), (50,  5, 6,  12, NOW()),
(51,  5, 8,  18, NOW()), (52,  5, 11, 42, NOW()), (53,  5, 12, 90, NOW()),
(54,  5, 14, 35, NOW()),
-- 武汉江汉路店 (store_id=6)
(55,  6, 1,  15, NOW()), (56,  6, 3,  12, NOW()), (57,  6, 5,  6,  NOW()),
(58,  6, 8,  18, NOW()), (59,  6, 11, 38, NOW()), (60,  6, 13, 22, NOW()),
-- 杭州西湖店 (store_id=7)
(61,  7, 1,  22, NOW()), (62,  7, 2,  38, NOW()), (63,  7, 4,  25, NOW()),
(64,  7, 7,  12, NOW()), (65,  7, 9,  15, NOW()), (66,  7, 10, 14, NOW()),
(67,  7, 11, 50, NOW()), (68,  7, 14, 40, NOW()),
-- 南京新街口店 (store_id=8)
(69,  8, 2,  28, NOW()), (70,  8, 4,  20, NOW()), (71,  8, 6,  10, NOW()),
(72,  8, 8,  15, NOW()), (73,  8, 11, 35, NOW()), (74,  8, 12, 70, NOW()),
(75,  8, 15, 8,  NOW()),
-- 西安钟楼店 (store_id=9)
(76,  9, 2,  20, NOW()), (77,  9, 4,  18, NOW()), (78,  9, 8,  12, NOW()),
(79,  9, 11, 28, NOW()), (80,  9, 12, 55, NOW()), (81,  9, 14, 25, NOW());

-- ============================================================
-- 6. supplier — 供应商
-- ============================================================
DROP TABLE IF EXISTS `supplier`;
CREATE TABLE `supplier` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`           VARCHAR(100) NOT NULL COMMENT '供应商名称',
  `contact_person` VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
  `contact_phone`  VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  `address`        VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `description`    TEXT         DEFAULT NULL COMMENT '备注',
  `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商';

INSERT INTO `supplier` VALUES
(1, '神州数码控股有限公司',     '陈经理', '13900001111', '北京市海淀区中关村南大街2号数码大厦',      'Apple产品授权分销商', NOW()),
(2, '北京英迈电子商贸有限公司', '刘经理', '13900002222', '北京市朝阳区望京SOHO T1-1208',            'Samsung/小米授权分销商', NOW()),
(3, '深圳华强电子供应链有限公司','黄经理', '13900003333', '深圳市福田区华强北路赛格科技园4栋',       '华南区综合供应商', NOW()),
(4, '上海百联电子科技有限公司', '周经理', '13900004444', '上海市浦东新区张江高科技园区碧波路518号', '华东区综合供应商', NOW()),
(5, '成都西部数码科技有限公司', '吴经理', '13900005555', '成都市高新区天府大道南段888号',            '西南区综合供应商，华为授权', NOW());

-- ============================================================
-- 7. purchase_record — 进货记录（入库单）
-- ============================================================
DROP TABLE IF EXISTS `purchase_record`;
CREATE TABLE `purchase_record` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_no`    VARCHAR(50)   DEFAULT NULL COMMENT '批次号',
  `product_id`  BIGINT        NOT NULL COMMENT '商品ID',
  `store_id`    BIGINT        NOT NULL COMMENT '店铺ID',
  `supplier_id` BIGINT        DEFAULT NULL COMMENT '供应商ID',
  `quantity`    INT           NOT NULL COMMENT '入库数量',
  `unit_cost`   DECIMAL(10,2) NOT NULL COMMENT '实际进货单价',
  `total_cost`  DECIMAL(12,2) NOT NULL COMMENT '总成本 = quantity * unit_cost',
  `operator_id` BIGINT        NOT NULL COMMENT '操作人ID',
  `remark`      VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_supplier` (`supplier_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进货记录';

INSERT INTO `purchase_record` VALUES
(1,  'P20260101001', 1,  1, 1, 50, 8100.00,  405000.00,  2, '元旦备货', '2026-01-05 10:30:00'),
(2,  'P20260101002', 2,  1, 1, 60, 5150.00,  309000.00,  2, '元旦备货', '2026-01-05 10:35:00'),
(3,  'P20260101003', 5,  1, 1, 20, 12700.00, 254000.00,  2, '元旦备货', '2026-01-05 10:40:00'),
(4,  'P20260115001', 11, 1, 1, 100, 1380.00, 138000.00,  2, '春节前备货', '2026-01-15 14:00:00'),
(5,  'P20260201001', 1,  3, 3, 60, 8150.00,  489000.00,  2, '春节前备货', '2026-02-01 09:30:00'),
(6,  'P20260201002', 3,  3, 2, 40, 4750.00,  190000.00,  2, '春节前备货', '2026-02-01 09:45:00'),
(7,  'P20260301001', 4,  3, 3, 60, 3750.00,  225000.00,  2, '春季备货', '2026-03-01 11:00:00'),
(8,  'P20260315001', 13, 3, 3, 50, 1780.00,  89000.00,   2, '新品上架', '2026-03-15 16:20:00'),
(9,  'P20260401001', 6,  5, 4, 30, 8450.00,  253500.00,  2, '春季备货', '2026-04-01 10:00:00'),
(10, 'P20260415001', 7,  5, 5, 25, 7750.00,  193750.00,  2, '新品上架', '2026-04-15 14:30:00'),
(11, 'P20260501001', 8,  7, 4, 40, 3750.00,  150000.00,  2, '五一备货', '2026-05-01 09:00:00'),
(12, 'P20260501002', 10, 7, 4, 30, 4750.00,  142500.00,  2, '五一备货', '2026-05-01 09:15:00'),
(13, 'P20260601001', 1,  1, 1, 40, 8200.00,  328000.00,  2, '618备货', '2026-06-01 08:30:00'),
(14, 'P20260601002', 5,  1, 1, 15, 12800.00, 192000.00,  2, '618备货', '2026-06-01 08:45:00'),
(15, 'P20260601003', 11, 1, 1, 80, 1390.00,  111200.00,  2, '618备货', '2026-06-01 09:00:00');

-- ============================================================
-- 8. sale_record — 销售记录（出库单）
-- ============================================================
DROP TABLE IF EXISTS `sale_record`;
CREATE TABLE `sale_record` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id`    BIGINT        NOT NULL COMMENT '商品ID',
  `store_id`      BIGINT        NOT NULL COMMENT '店铺ID',
  `quantity`      INT           NOT NULL COMMENT '销售数量',
  `unit_price`    DECIMAL(10,2) NOT NULL COMMENT '实际售价',
  `total_amount`  DECIMAL(12,2) NOT NULL COMMENT '销售总额',
  `unit_cost`     DECIMAL(10,2) NOT NULL COMMENT '当时成本价',
  `profit`        DECIMAL(12,2) NOT NULL COMMENT '利润 = (unit_price - unit_cost) * quantity',
  `customer_name` VARCHAR(100)  DEFAULT NULL COMMENT '客户名称',
  `operator_id`   BIGINT        NOT NULL COMMENT '操作人ID',
  `remark`        VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '销售时间',
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售记录';

INSERT INTO `sale_record` VALUES
-- 1月销售
(1,  1,  1, 5,  9999.00,  49995.00,  8100.00,  9495.00,  NULL, 3, '元旦促销', '2026-01-10 15:30:00'),
(2,  2,  1, 8,  6999.00,  55992.00,  5150.00,  14792.00, NULL, 3, NULL, '2026-01-12 11:20:00'),
(3,  5,  1, 3,  14999.00, 44997.00,  12700.00, 6897.00,  NULL, 3, NULL, '2026-01-18 16:45:00'),
(4,  11, 1, 15, 1999.00,  29985.00,  1380.00,  9285.00,  NULL, 3, NULL, '2026-01-25 14:10:00'),
-- 2月销售（春节旺季）
(5,  1,  3, 12, 9999.00,  119988.00, 8150.00,  22188.00, NULL, 4, '春节促销', '2026-02-05 10:30:00'),
(6,  2,  3, 15, 6999.00,  104985.00, 5150.00,  27735.00, NULL, 4, '春节促销', '2026-02-06 11:00:00'),
(7,  3,  3, 8,  6499.00,  51992.00,  4750.00,  13992.00, NULL, 4, NULL, '2026-02-10 14:20:00'),
(8,  11, 3, 25, 1999.00,  49975.00,  1380.00,  15475.00, NULL, 4, NULL, '2026-02-14 16:30:00'),
(9,  1,  1, 8,  9899.00,  79192.00,  8100.00,  14392.00, NULL, 3, '情人节活动', '2026-02-14 14:00:00'),
-- 3月销售
(10, 4,  3, 10, 4999.00,  49990.00,  3750.00,  12490.00, NULL, 4, NULL, '2026-03-08 15:30:00'),
(11, 8,  3, 8,  5499.00,  43992.00,  3750.00,  13992.00, NULL, 4, '三八节活动', '2026-03-08 10:15:00'),
(12, 13, 3, 12, 2499.00,  29988.00,  1780.00,  8628.00,  NULL, 4, NULL, '2026-03-20 11:45:00'),
-- 4月销售
(13, 6,  5, 5,  10999.00, 54995.00,  8450.00,  12745.00, NULL, 3, NULL, '2026-04-10 09:30:00'),
(14, 7,  5, 4,  9999.00,  39996.00,  7750.00,  8996.00,  NULL, 3, NULL, '2026-04-15 14:20:00'),
(15, 2,  5, 10, 6899.00,  68990.00,  5150.00,  17490.00, NULL, 3, NULL, '2026-04-22 16:00:00'),
-- 5月销售（五一黄金周）
(16, 8,  7, 10, 5499.00,  54990.00,  3750.00,  17490.00, NULL, 3, '五一促销', '2026-05-02 10:30:00'),
(17, 10, 7, 6,  6499.00,  38994.00,  4750.00,  10494.00, NULL, 3, '五一促销', '2026-05-02 11:15:00'),
(18, 1,  7, 6,  9999.00,  59994.00,  8100.00,  11394.00, NULL, 3, '五一促销', '2026-05-03 09:45:00'),
(19, 2,  7, 12, 6899.00,  82788.00,  5150.00,  20988.00, NULL, 3, '五一促销', '2026-05-04 14:30:00'),
-- 6月销售（618大促）
(20, 1,  1, 10, 9599.00,  95990.00,  8100.00,  14990.00, NULL, 3, '618大促', '2026-06-18 08:00:00'),
(21, 5,  1, 5,  14499.00, 72495.00,  12700.00, 8995.00,  NULL, 3, '618大促', '2026-06-18 09:30:00'),
(22, 11, 1, 30, 1899.00,  56970.00,  1380.00,  15570.00, NULL, 3, '618大促', '2026-06-18 10:00:00'),
(23, 2,  1, 15, 6599.00,  98985.00,  5150.00,  21735.00, NULL, 3, '618大促', '2026-06-18 14:20:00'),
(24, 13, 1, 8,  2399.00,  19192.00,  1780.00,  4952.00,  NULL, 3, '618大促', '2026-06-18 16:45:00'),
-- 7月销售
(25, 4,  3, 12, 4999.00,  59988.00,  3750.00,  14988.00, NULL, 4, NULL, '2026-07-05 10:20:00'),
(26, 8,  3, 10, 5499.00,  54990.00,  3750.00,  17490.00, NULL, 4, NULL, '2026-07-12 14:00:00'),
(27, 1,  3, 8,  9999.00,  79992.00,  8150.00,  14792.00, NULL, 4, NULL, '2026-07-18 11:30:00'),
(28, 11, 3, 20, 1999.00,  39980.00,  1380.00,  12380.00, NULL, 4, NULL, '2026-07-25 16:15:00'),
(29, 2,  7, 10, 6999.00,  69990.00,  5150.00,  18490.00, NULL, 3, '暑期促销', '2026-07-10 10:30:00'),
(30, 10, 7, 5,  6499.00,  32495.00,  4750.00,  8745.00,  NULL, 3, '暑期促销', '2026-07-15 14:00:00');

-- ============================================================
-- 9. operation_log — 操作日志
-- ============================================================
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `operator_id`   BIGINT       DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(50)  DEFAULT NULL COMMENT '操作人名称',
  `module`        VARCHAR(50)  DEFAULT NULL COMMENT '操作模块',
  `action`        VARCHAR(50)  NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/IMPORT/EXPORT',
  `target_type`   VARCHAR(50)  NOT NULL COMMENT '操作对象类型: store/product/purchase/sale等',
  `target_id`     BIGINT       DEFAULT NULL COMMENT '操作对象ID',
  `detail`        TEXT         DEFAULT NULL COMMENT '操作详情',
  `ip_address`    VARCHAR(50)  DEFAULT NULL COMMENT '操作IP',
  `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_operator` (`operator_id`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- ============================================================
-- 10. notification — 通知消息
-- ============================================================
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`       VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content`     TEXT         DEFAULT NULL COMMENT '通知内容',
  `type`        VARCHAR(30)  NOT NULL COMMENT '类型: stock_alert/restock_suggest/monthly_report/cost_reminder',
  `level`       VARCHAR(10)  DEFAULT 'info' COMMENT '级别: info/warning/error',
  `is_read`     TINYINT      DEFAULT 0 COMMENT '0=未读, 1=已读',
  `recipient_id` BIGINT      DEFAULT NULL COMMENT '接收人ID(NULL=全员)',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_recipient` (`recipient_id`),
  KEY `idx_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息';

INSERT INTO `notification` VALUES
(1, '库存预警：Xiaomi Band 8 Pro', '北京朝阳SKP店 Xiaomi Band 8 Pro 库存仅剩5件，低于安全库存阈值20', 'stock_alert', 'warning', 0, NULL, '2026-07-28 08:00:00'),
(2, '库存预警：HomePod mini', '广州天河店 HomePod mini 库存仅剩3件，低于安全库存阈值15', 'stock_alert', 'warning', 0, NULL, '2026-07-28 08:00:00'),
(3, '补货建议：iPhone 15 Pro Max', '基于近3个月销售趋势，建议为深圳华强北店补货iPhone 15 Pro Max 20台', 'restock_suggest', 'info', 0, NULL, '2026-07-29 10:30:00'),
(4, '2026年7月经营月报已生成', '本月全渠道销售额达¥1,253,680，利润¥286,430', 'monthly_report', 'info', 0, NULL, '2026-08-01 00:05:00'),
(5, '续租提醒：上海陆家嘴店', '上海陆家嘴店租赁合同将于2026年9月30日到期，请及时续签', 'cost_reminder', 'warning', 0, NULL, '2026-08-01 09:00:00');

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
SELECT 'StoreIQ 数据库初始化完成！' AS message;
SELECT COUNT(*) AS 用户数 FROM sys_user;
SELECT COUNT(*) AS 店铺数 FROM store;
SELECT COUNT(*) AS 商品数 FROM product;
SELECT COUNT(*) AS 库存记录数 FROM inventory;
