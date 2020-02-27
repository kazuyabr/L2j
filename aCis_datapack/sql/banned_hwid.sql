/*
Navicat MySQL Data Transfer

Source Server         : localhost_3306
Source Server Version : 50524
Source Host           : localhost:3306
Source Database       : l2pvpzonecustom

Target Server Type    : MYSQL
Target Server Version : 50524
File Encoding         : 65001

Date: 2017-10-02 02:13:33
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `banned_hwid`
-- ----------------------------
DROP TABLE IF EXISTS `banned_hwid`;
CREATE TABLE `banned_hwid` (
  `char_name` varchar(35) NOT NULL,
  `hwid` varchar(64) NOT NULL,
  PRIMARY KEY (`hwid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- ----------------------------
-- Records of banned_hwid
-- ----------------------------
