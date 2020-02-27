/*
Navicat MySQL Data Transfer

Source Server         : localhost_3306
Source Server Version : 50524
Source Host           : localhost:3306
Source Database       : l2pvpzonecustom

Target Server Type    : MYSQL
Target Server Version : 50524
File Encoding         : 65001

Date: 2017-10-02 02:13:39
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `hwid_info`
-- ----------------------------
DROP TABLE IF EXISTS `hwid_info`;
CREATE TABLE `hwid_info` (
  `HWID` varchar(32) NOT NULL DEFAULT '',
  `WindowsCount` int(10) unsigned NOT NULL DEFAULT '1',
  `Account` varchar(45) NOT NULL DEFAULT '',
  `PlayerID` int(10) unsigned NOT NULL DEFAULT '0',
  `LockType` enum('PLAYER_LOCK','ACCOUNT_LOCK','NONE') NOT NULL DEFAULT 'NONE',
  PRIMARY KEY (`HWID`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of hwid_info
-- ----------------------------
