/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : aiqutepets

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 25/12/2025 16:18:18
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for app_rich_content
-- ----------------------------
DROP TABLE IF EXISTS `app_rich_content`;
CREATE TABLE `app_rich_content`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content_key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '内容标识(唯一Key): guide-玩伴指南, agreement-陪伴约定',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `content_html` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '富文本内容(HTML格式)',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_key`(`content_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'App富文本内容表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of app_rich_content
-- ----------------------------
INSERT INTO `app_rich_content` VALUES (1, 'guide', '玩伴指南', '<p>亲爱的小朋友，我是多尼兔！<br>我可以给你讲故事，还能陪你聊天哦。</p><h3>如何使用我？</h3><p>1. 按下肚子说话...</p>', '2025-12-19 17:45:16', '2025-12-19 17:45:16');
INSERT INTO `app_rich_content` VALUES (2, 'agreement', '陪伴约定', '<p>为了养成好习惯，我们需要做一个小小的约定：</p><ul><li>每天使用不超过30分钟</li><li>晚上9点就要睡觉觉</li></ul>', '2025-12-19 17:45:16', '2025-12-19 17:45:16');

-- ----------------------------
-- Table structure for device_action_log
-- ----------------------------
DROP TABLE IF EXISTS `device_action_log`;
CREATE TABLE `device_action_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_uid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `action_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '行为: chat, pet_cat, feed, play',
  `duration_or_count` int NULL DEFAULT 0 COMMENT '时长或次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_uid`(`device_uid` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备交互行为日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of device_action_log
-- ----------------------------

-- ----------------------------
-- Table structure for device_diary
-- ----------------------------
DROP TABLE IF EXISTS `device_diary`;
CREATE TABLE `device_diary`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_uid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备UID',
  `diary_date` date NOT NULL COMMENT '日记日期',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '日记正文内容',
  `emotion_tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '情绪标签(JSON数组字符串)',
  `ai_generated_time` datetime NULL DEFAULT NULL COMMENT 'AI生成时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_device_date`(`device_uid` ASC, `diary_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备日记历史表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of device_diary
-- ----------------------------

-- ----------------------------
-- Table structure for device_info
-- ----------------------------
DROP TABLE IF EXISTS `device_info`;
CREATE TABLE `device_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_uid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备唯一标识 (印在机身/二维码)',
  `mac` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '蓝牙MAC地址 (辅助校验)',
  `secret_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备通信密钥 (用于签名)',
  `product_model` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'RABBIT-V1' COMMENT '产品型号',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态: 0-未激活 1-已激活',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `firmware_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '当前固件版本(如 v1.0.0)',
  `battery_level` int NULL DEFAULT 0 COMMENT '剩余电量 0-100',
  `online_status` tinyint(1) NULL DEFAULT 0 COMMENT '在线状态: 0-离线 1-在线',
  `last_active_time` datetime NULL DEFAULT NULL COMMENT '最后活跃时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_uid`(`device_uid` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备白名单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of device_info
-- ----------------------------
INSERT INTO `device_info` VALUES (1, 'Q0102_TEST_001', 'AA:BB:CC:DD:EE:FF', 'test_secret_123', 'RABBIT-V1', 1, '2025-12-19 15:23:12', NULL, 0, 0, NULL);

-- ----------------------------
-- Table structure for device_ota_log
-- ----------------------------
DROP TABLE IF EXISTS `device_ota_log`;
CREATE TABLE `device_ota_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_uid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备UID',
  `user_id` bigint NOT NULL COMMENT '操作人ID',
  `target_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标版本号',
  `action_type` tinyint NULL DEFAULT 1 COMMENT '1:发起检查 2:发起升级',
  `status_code` int NULL DEFAULT NULL COMMENT '第三方返回的状态码',
  `api_response` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '第三方返回的原始报文(用于排错)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_uid`(`device_uid` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备OTA操作日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of device_ota_log
-- ----------------------------
INSERT INTO `device_ota_log` VALUES (1, 'Q0102_TEST_001', 1, NULL, 1, 0, '{\"success\":false,\"code\":401,\"message\":\"未提供token\",\"data\":{}}', '2025-12-23 14:34:51');
INSERT INTO `device_ota_log` VALUES (2, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-23 15:23:02');
INSERT INTO `device_ota_log` VALUES (3, 'Q0102_TEST_001', 1, NULL, 1, 0, '{\"success\":false,\"code\":401,\"message\":\"未提供token\",\"data\":{}}', '2025-12-23 15:23:07');
INSERT INTO `device_ota_log` VALUES (4, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-23 15:23:13');
INSERT INTO `device_ota_log` VALUES (5, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-23 15:44:38');
INSERT INTO `device_ota_log` VALUES (6, 'Q0102_TEST_001', 1, NULL, 1, 0, '{\"success\":false,\"code\":401,\"message\":\"未提供token\",\"data\":{}}', '2025-12-24 10:13:07');
INSERT INTO `device_ota_log` VALUES (7, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-24 10:13:10');
INSERT INTO `device_ota_log` VALUES (8, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-24 11:03:22');
INSERT INTO `device_ota_log` VALUES (9, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-24 11:03:48');
INSERT INTO `device_ota_log` VALUES (10, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-24 11:03:49');
INSERT INTO `device_ota_log` VALUES (11, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-24 11:03:49');
INSERT INTO `device_ota_log` VALUES (12, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-24 11:03:55');
INSERT INTO `device_ota_log` VALUES (13, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-24 11:04:54');
INSERT INTO `device_ota_log` VALUES (14, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-24 11:24:57');
INSERT INTO `device_ota_log` VALUES (15, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-24 11:25:19');
INSERT INTO `device_ota_log` VALUES (16, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-24 11:27:51');
INSERT INTO `device_ota_log` VALUES (17, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-24 11:30:20');
INSERT INTO `device_ota_log` VALUES (18, 'Q0102_TEST_001', 1, NULL, 1, 0, '{\"success\":false,\"code\":401,\"message\":\"未提供token\",\"data\":{}}', '2025-12-24 11:34:40');
INSERT INTO `device_ota_log` VALUES (19, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-24 11:43:41');
INSERT INTO `device_ota_log` VALUES (20, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-24 16:06:44');
INSERT INTO `device_ota_log` VALUES (21, 'Q0102_TEST_001', 1, NULL, 2, 0, '发起升级失败: 未提供token', '2025-12-24 16:36:15');

-- ----------------------------
-- Table structure for mp_user
-- ----------------------------
DROP TABLE IF EXISTS `mp_user`;
CREATE TABLE `mp_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `openid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '微信OpenID (唯一标识)',
  `unionid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信UnionID',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户手机号',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `session_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会话密钥 (后端缓存用)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_openid`(`openid` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '小程序用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mp_user
-- ----------------------------
INSERT INTO `mp_user` VALUES (1, 'test_openid_dev_12345', NULL, '19144381232', NULL, NULL, 'mock_session_key_1766648636664', '2025-12-23 13:38:50', '2025-12-25 15:43:57');

-- ----------------------------
-- Table structure for user_device_rel
-- ----------------------------
DROP TABLE IF EXISTS `user_device_rel`;
CREATE TABLE `user_device_rel`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID (mp_user.id)',
  `device_uid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备UID',
  `is_owner` tinyint(1) NULL DEFAULT 0 COMMENT '是否管理员: 1-是 0-否',
  `bind_source` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'bluetooth' COMMENT '绑定来源',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `device_nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户自定义设备昵称',
  `device_avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户自定义设备头像',
  `is_top` tinyint(1) NULL DEFAULT 0 COMMENT '是否置顶显示',
  `is_current` tinyint(1) NULL DEFAULT 0 COMMENT '是否为当前选中设备: 1-是 0-否',
  `last_used_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后使用/切换时间',
  `intimacy_level` int NULL DEFAULT 1 COMMENT '缓存:设备当前级别',
  `intimacy_score` int NULL DEFAULT 0 COMMENT '缓存:亲密值进度百分比(0-100)',
  `current_badge` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '当前获得的最高徽章',
  `growth_stats_json` json NULL COMMENT '缓存:完整的五维成长数据',
  `last_mood_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '缓存:AI今日心情文案',
  `last_mood_date` date NULL DEFAULT NULL COMMENT '缓存:心情生成的日期(yyyy-MM-dd)',
  `has_ota_update` tinyint(1) NULL DEFAULT 0 COMMENT 'OTA更新标记: 0-无更新, 1-有更新',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_device`(`user_id` ASC, `device_uid` ASC) USING BTREE,
  INDEX `idx_user_current`(`user_id` ASC, `is_current` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户设备绑定关系' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_device_rel
-- ----------------------------
INSERT INTO `user_device_rel` VALUES (2, 1, 'Q0102_TEST_001', 1, 'bluetooth', '2025-12-24 10:12:58', '', '', NULL, 0, '2025-12-24 10:12:57', 1, 0, NULL, NULL, NULL, NULL, 0);
INSERT INTO `user_device_rel` VALUES (3, 1, '7EAAD1CD390C', 1, 'bluetooth', '2025-12-24 16:48:33', NULL, NULL, NULL, 0, '2025-12-24 16:48:32', 1, 0, NULL, NULL, NULL, NULL, 0);

SET FOREIGN_KEY_CHECKS = 1;
