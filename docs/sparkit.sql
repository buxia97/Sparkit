-- =====================================================
-- Sparkit 通用开发框架 数据库初始化脚本
-- 兼容：MySQL 5.5 ~ 8.0
-- 字符集：utf8mb4
-- 排序规则：utf8mb4_general_ci
-- 所有表前缀：sparkit_
-- 说明：
--   1. CREATE_TIME 使用 timestamp + DEFAULT CURRENT_TIMESTAMP
--      取代 datetime，兼容 MySQL 5.5
--   2. UPDATE_TIME 去掉 ON UPDATE CURRENT_TIMESTAMP，
--      由 MyBatis-Plus MetaObjectHandler 在代码层自动维护
--   3. 每张表最多只有一个 timestamp 列使用 CURRENT_TIMESTAMP，
--      避免 MySQL 5.5 的限制
-- =====================================================

-- 创建数据库（按需取消注释）
-- CREATE DATABASE IF NOT EXISTS sparkit DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
-- USE sparkit;

-- =====================================================
-- 一、系统管理模块
-- =====================================================

-- 1.1 管理员用户表
DROP TABLE IF EXISTS `sparkit_admin_user`;
CREATE TABLE `sparkit_admin_user` (
  `admin_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `username` varchar(64) NOT NULL COMMENT '登录用户名，唯一',
  `password` varchar(128) NOT NULL COMMENT '加密密码（BCrypt）',
  `nickname` varchar(64) DEFAULT NULL COMMENT '显示名称',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `gender` tinyint(1) DEFAULT NULL COMMENT '性别：0=未知, 1=男, 2=女',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '所属部门ID',
  `login_ip` varchar(128) DEFAULT NULL COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `login_count` int(11) DEFAULT NULL COMMENT '累计登录次数',
  `pwd_reset_time` datetime DEFAULT NULL COMMENT '密码最后修改时间',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=停用, 1=正常',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=否, 1=是',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`admin_id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员用户表';

-- 1.2 角色表
DROP TABLE IF EXISTS `sparkit_role`;
CREATE TABLE `sparkit_role` (
  `role_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `role_name` varchar(64) NOT NULL COMMENT '角色名称',
  `role_key` varchar(64) NOT NULL COMMENT '角色标识（唯一，如 admin/common）',
  `role_sort` int(11) DEFAULT NULL COMMENT '排序号',
  `data_scope` tinyint(1) DEFAULT NULL COMMENT '数据权限范围：1=全部, 2=自定义, 3=本部门, 4=本部门及子部门, 5=仅本人',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=停用, 1=正常',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 1.3 菜单/权限表（细粒度权限核心）
DROP TABLE IF EXISTS `sparkit_menu`;
CREATE TABLE `sparkit_menu` (
  `menu_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `parent_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '父菜单ID，顶级为0',
  `menu_name` varchar(64) NOT NULL COMMENT '菜单/权限名称',
  `menu_type` char(1) NOT NULL COMMENT 'D=目录, M=菜单, B=按钮/API接口',
  `path` varchar(200) DEFAULT NULL COMMENT '路由地址（菜单）或API路径（按钮）',
  `component` varchar(255) DEFAULT NULL COMMENT '前端组件路径（仅菜单类型使用）',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识（如 sys:user:list）',
  `icon` varchar(100) DEFAULT NULL COMMENT '菜单图标',
  `sort` int(11) DEFAULT '0' COMMENT '排序号',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=隐藏, 1=显示',
  `visible` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否在菜单中显示：0=隐藏, 1=显示',
  `is_frame` tinyint(1) DEFAULT '0' COMMENT '是否外链：0=否, 1=是',
  `method` varchar(10) DEFAULT NULL COMMENT 'HTTP方法（仅API按钮类型）：GET/POST/PUT/DELETE/PATCH',
  `api_path` varchar(200) DEFAULT NULL COMMENT '完整API路径（仅API按钮类型）',
  `query` varchar(255) DEFAULT NULL COMMENT '路由参数',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`menu_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单/权限表 - 细粒度权限核心';

-- 1.4 角色-菜单关联表
DROP TABLE IF EXISTS `sparkit_role_menu`;
CREATE TABLE `sparkit_role_menu` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单关联表';

-- 1.5 管理员-角色关联表
DROP TABLE IF EXISTS `sparkit_admin_user_role`;
CREATE TABLE `sparkit_admin_user_role` (
  `admin_id` bigint(20) NOT NULL COMMENT '管理员ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`admin_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员-角色关联表';

-- 1.6 部门表
DROP TABLE IF EXISTS `sparkit_dept`;
CREATE TABLE `sparkit_dept` (
  `dept_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `parent_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '父部门ID，顶级为0',
  `dept_name` varchar(64) NOT NULL COMMENT '部门名称',
  `leader` varchar(64) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `sort` int(11) DEFAULT '0' COMMENT '排序号',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=停用, 1=正常',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 1.7 岗位表
DROP TABLE IF EXISTS `sparkit_post`;
CREATE TABLE `sparkit_post` (
  `post_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `post_code` varchar(64) NOT NULL COMMENT '岗位编码（唯一）',
  `post_name` varchar(64) NOT NULL COMMENT '岗位名称',
  `sort` int(11) DEFAULT '0' COMMENT '排序号',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=停用, 1=正常',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`post_id`),
  UNIQUE KEY `uk_post_code` (`post_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';

-- 1.8 管理员-岗位关联表
DROP TABLE IF EXISTS `sparkit_admin_user_post`;
CREATE TABLE `sparkit_admin_user_post` (
  `admin_id` bigint(20) NOT NULL COMMENT '管理员ID',
  `post_id` bigint(20) NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`admin_id`, `post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员-岗位关联表';

-- 1.9 统一系统配置表
DROP TABLE IF EXISTS `sparkit_config`;
CREATE TABLE `sparkit_config` (
  `config_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `config_group` varchar(64) NOT NULL COMMENT '配置分组（如：system/upload/sms/payment/ai/security）',
  `config_key` varchar(128) NOT NULL COMMENT '配置键（唯一）',
  `config_name` varchar(128) NOT NULL COMMENT '配置名称/说明',
  `config_value` text COMMENT '配置值',
  `config_type` varchar(20) NOT NULL DEFAULT 'text' COMMENT '值类型：text/image/switch/json/number/textarea',
  `sort` int(11) DEFAULT '0' COMMENT '排序号',
  `built_in` tinyint(1) DEFAULT '0' COMMENT '是否内置：0=否(可删), 1=是(不可删)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_config_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一系统配置表';

-- 1.10 字典类型表
DROP TABLE IF EXISTS `sparkit_dict_type`;
CREATE TABLE `sparkit_dict_type` (
  `dict_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型（唯一，如 sys_user_sex）',
  `dict_name` varchar(100) NOT NULL COMMENT '字典名称',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=停用, 1=正常',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- 1.11 字典数据表
DROP TABLE IF EXISTS `sparkit_dict_data`;
CREATE TABLE `sparkit_dict_data` (
  `data_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型（关联 sparkit_dict_type.dict_type）',
  `dict_label` varchar(100) NOT NULL COMMENT '字典标签',
  `dict_value` varchar(100) NOT NULL COMMENT '字典值',
  `css_class` varchar(100) DEFAULT NULL COMMENT '样式类名',
  `list_class` varchar(100) DEFAULT NULL COMMENT '列表样式',
  `sort` int(11) DEFAULT '0' COMMENT '排序号',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=停用, 1=正常',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否默认：0=否, 1=是',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`data_id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- 1.12 地区表
DROP TABLE IF EXISTS `sparkit_region`;
CREATE TABLE `sparkit_region` (
  `region_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `parent_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '父级ID，根为0',
  `region_code` varchar(20) NOT NULL COMMENT '行政区划代码',
  `region_name` varchar(64) NOT NULL COMMENT '地区名称',
  `region_level` tinyint(1) NOT NULL COMMENT '层级：1=省, 2=市, 3=区/县',
  `sort` int(11) DEFAULT '0' COMMENT '排序号',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=停用, 1=正常',
  PRIMARY KEY (`region_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_region_code` (`region_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地区表';

-- 1.13 国际化表
DROP TABLE IF EXISTS `sparkit_i18n`;
CREATE TABLE `sparkit_i18n` (
  `i18n_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `lang_key` varchar(160) NOT NULL COMMENT '语言Key',
  `lang_code` varchar(20) NOT NULL COMMENT '语言编码：zh_CN / en_US / ja_JP / ko_KR',
  `lang_value` varchar(500) NOT NULL COMMENT '翻译值',
  `module` varchar(64) DEFAULT NULL COMMENT '所属模块',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`i18n_id`),
  UNIQUE KEY `uk_lang_key_code` (`lang_key`, `lang_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国际化表';

-- =====================================================
-- 二、用户模块表（C端）
-- =====================================================

-- 2.1 C端用户表
DROP TABLE IF EXISTS `sparkit_user`;
CREATE TABLE `sparkit_user` (
  `user_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `username` varchar(64) NOT NULL COMMENT '用户名（唯一）',
  `password` varchar(128) DEFAULT NULL COMMENT '加密密码（第三方登录可为空）',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `real_name` varchar(32) DEFAULT NULL COMMENT '真实姓名',
  `id_card` varchar(32) DEFAULT NULL COMMENT '身份证号（加密存储）',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `gender` tinyint(1) DEFAULT NULL COMMENT '0=未知, 1=男, 2=女',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `level` int(11) DEFAULT '0' COMMENT '用户等级',
  `exp` bigint(20) DEFAULT '0' COMMENT '经验值',
  `growth` bigint(20) DEFAULT '0' COMMENT '成长值',
  `real_name_status` tinyint(1) DEFAULT '0' COMMENT '实名状态：0=未认证, 1=已认证, 2=审核中, 3=认证失败',
  `is_blacklisted` tinyint(1) DEFAULT '0' COMMENT '是否黑名单：0=否, 1=是',
  `source` varchar(20) NOT NULL COMMENT '注册来源：register/wechat/qq/weibo/github/dingtalk/wecom',
  `last_login_ip` varchar(128) DEFAULT NULL COMMENT '最后登录IP',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `register_ip` varchar(128) DEFAULT NULL COMMENT '注册IP',
  `register_time` datetime NOT NULL COMMENT '注册时间',
  `verified` tinyint(1) DEFAULT '0' COMMENT '实名认证：0=未认证, 1=已认证',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=禁用, 1=正常',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C端用户表';

-- 2.2 第三方登录绑定表
DROP TABLE IF EXISTS `sparkit_user_third_party`;
CREATE TABLE `sparkit_user_third_party` (
  `bind_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `platform` varchar(20) NOT NULL COMMENT '平台：wechat/wecom/qq/weibo/github/dingtalk',
  `open_id` varchar(128) NOT NULL COMMENT '平台OpenID',
  `union_id` varchar(128) DEFAULT NULL COMMENT '微信UnionID',
  `app_type` varchar(20) DEFAULT NULL COMMENT '来源类型：mp/app/web/h5',
  `nickname` varchar(64) DEFAULT NULL COMMENT '平台昵称',
  `avatar` varchar(500) DEFAULT NULL COMMENT '平台头像',
  `access_token` varchar(500) DEFAULT NULL COMMENT '访问令牌',
  `refresh_token` varchar(500) DEFAULT NULL COMMENT '刷新令牌',
  `token_expire` datetime DEFAULT NULL COMMENT '令牌过期时间',
  `bind_time` datetime NOT NULL COMMENT '绑定时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`bind_id`),
  UNIQUE KEY `uk_platform_openid` (`platform`, `open_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方登录绑定表';

-- 2.3 验证码表
DROP TABLE IF EXISTS `sparkit_user_verify_code`;
CREATE TABLE `sparkit_user_verify_code` (
  `code_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `target` varchar(128) NOT NULL COMMENT '手机号或邮箱地址',
  `target_type` varchar(10) NOT NULL COMMENT '目标类型：phone/email',
  `code` varchar(10) NOT NULL COMMENT '验证码',
  `scene` varchar(32) NOT NULL COMMENT '场景：register/login/reset_password/bind/change/unbind',
  `ip_address` varchar(128) DEFAULT NULL COMMENT '请求IP',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=未使用, 1=已使用, 2=已过期',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`code_id`),
  KEY `idx_target_scene` (`target`, `scene`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验证码表';

-- 2.4 黑名单表
DROP TABLE IF EXISTS `sparkit_user_blacklist`;
CREATE TABLE `sparkit_user_blacklist` (
  `blacklist_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `target_type` varchar(10) NOT NULL COMMENT '类型：user/ip',
  `target_value` varchar(128) NOT NULL COMMENT '用户ID或IP地址',
  `reason` varchar(500) DEFAULT NULL COMMENT '拉黑原因',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=已解除, 1=生效中',
  `create_by` varchar(64) DEFAULT NULL COMMENT '操作人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `unlock_time` datetime DEFAULT NULL COMMENT '解封时间',
  `unlock_by` varchar(64) DEFAULT NULL COMMENT '解封操作人',
  PRIMARY KEY (`blacklist_id`),
  KEY `idx_target` (`target_type`, `target_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑名单表';

-- =====================================================
-- 三、存储模块表
-- =====================================================

-- 3.1 存储源配置表
DROP TABLE IF EXISTS `sparkit_storage_config`;
CREATE TABLE `sparkit_storage_config` (
  `storage_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `storage_name` varchar(64) NOT NULL COMMENT '存储源名称',
  `storage_type` varchar(20) NOT NULL COMMENT '类型：local/ftp/oss/cos/qiniu/s3',
  `source_code` varchar(50) NOT NULL COMMENT '存储源编码（唯一标识）',
  `access_key` varchar(255) DEFAULT NULL COMMENT 'AccessKey',
  `secret_key` varchar(255) DEFAULT NULL COMMENT 'SecretKey（加密存储）',
  `endpoint` varchar(255) DEFAULT NULL COMMENT '终端节点',
  `bucket` varchar(128) DEFAULT NULL COMMENT '桶/容器名称',
  `region` varchar(64) DEFAULT NULL COMMENT '地域',
  `domain` varchar(255) DEFAULT NULL COMMENT '自定义访问域名',
  `base_path` varchar(255) DEFAULT NULL COMMENT '基础存储路径',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否默认存储源：0=否, 1=是',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=停用, 1=启用',
  `sort` int(11) DEFAULT '0' COMMENT '排序号',
  `extra_config` text COMMENT '额外配置（JSON格式，存储各类型特有的配置项）',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=未删除, 1=已删除',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`storage_id`),
  UNIQUE KEY `uk_source_code` (`source_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储源配置表';

-- 3.2 文件记录表
DROP TABLE IF EXISTS `sparkit_file`;
CREATE TABLE `sparkit_file` (
  `file_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `storage_id` bigint(20) NOT NULL COMMENT '存储源ID',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_key` varchar(500) NOT NULL COMMENT '存储Key/路径',
  `file_url` varchar(500) NOT NULL COMMENT '访问URL',
  `file_size` bigint(20) NOT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件分类：image/video/audio/document/other',
  `file_ext` varchar(20) DEFAULT NULL COMMENT '扩展名',
  `mime_type` varchar(128) DEFAULT NULL COMMENT 'MIME类型',
  `md5` varchar(64) DEFAULT NULL COMMENT '文件MD5',
  `sha256` varchar(128) DEFAULT NULL COMMENT '文件SHA256',
  `upload_status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '上传状态：0=上传中, 1=完成, 2=失败',
  `chunk_count` int(11) DEFAULT NULL COMMENT '分片总数',
  `chunk_size` bigint(20) DEFAULT NULL COMMENT '分片大小（字节）',
  `upload_id` varchar(128) DEFAULT NULL COMMENT '分片上传任务ID（云存储返回）',
  `upload_by` varchar(64) DEFAULT NULL COMMENT '上传人',
  `thumbnail_path` varchar(500) DEFAULT NULL COMMENT '视频缩略图路径',
  `compressed_path` varchar(500) DEFAULT NULL COMMENT '压缩后文件路径',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`file_id`),
  KEY `idx_md5` (`md5`),
  KEY `idx_storage_id` (`storage_id`),
  KEY `idx_file_key` (`file_key`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表';

-- 3.3 文件分片记录表
DROP TABLE IF EXISTS `sparkit_file_chunk`;
CREATE TABLE `sparkit_file_chunk` (
  `chunk_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `file_id` bigint(20) NOT NULL COMMENT '文件ID',
  `chunk_index` int(11) NOT NULL COMMENT '分片序号（从0开始）',
  `chunk_md5` varchar(64) DEFAULT NULL COMMENT '分片MD5',
  `chunk_size` bigint(20) NOT NULL COMMENT '分片大小（字节）',
  `chunk_key` varchar(500) DEFAULT NULL COMMENT '分片存储Key',
  `chunk_status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=待上传, 1=已上传',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`chunk_id`),
  KEY `idx_file_id` (`file_id`),
  UNIQUE KEY `uk_file_chunk` (`file_id`, `chunk_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件分片记录表';

-- =====================================================
-- 四、支付模块表
-- =====================================================

-- 4.1 支付渠道配置表
DROP TABLE IF EXISTS `sparkit_payment_config`;
CREATE TABLE `sparkit_payment_config` (
  `config_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `channel` varchar(32) NOT NULL COMMENT '渠道编码',
  `channel_name` varchar(64) NOT NULL COMMENT '渠道名称',
  `app_id` varchar(128) DEFAULT NULL COMMENT 'AppID',
  `mch_id` varchar(128) DEFAULT NULL COMMENT '商户号',
  `api_key` varchar(255) DEFAULT NULL COMMENT 'API Key（加密存储）',
  `private_key` text COMMENT '私钥（加密存储）',
  `public_key` text COMMENT '公钥',
  `notify_url` varchar(500) DEFAULT NULL COMMENT '回调通知地址',
  `return_url` varchar(500) DEFAULT NULL COMMENT '支付完成跳转地址',
  `sandbox` tinyint(1) DEFAULT '0' COMMENT '沙箱模式：0=生产, 1=沙箱',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=停用, 1=启用',
  `sort` int(11) DEFAULT '0' COMMENT '排序号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_channel` (`channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付渠道配置表';

-- 4.2 支付订单表
DROP TABLE IF EXISTS `sparkit_payment_order`;
CREATE TABLE `sparkit_payment_order` (
  `order_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `order_no` varchar(64) NOT NULL COMMENT '业务订单号（唯一）',
  `payment_no` varchar(64) NOT NULL COMMENT '支付流水号（唯一）',
  `channel` varchar(32) NOT NULL COMMENT '支付渠道',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `user_type` varchar(10) NOT NULL COMMENT '用户类型：admin/user',
  `amount` decimal(12,2) NOT NULL COMMENT '订单金额（元）',
  `currency` varchar(10) DEFAULT 'CNY' COMMENT '货币：CNY/USD',
  `subject` varchar(255) NOT NULL COMMENT '商品标题',
  `body` varchar(500) DEFAULT NULL COMMENT '商品描述',
  `trade_type` varchar(20) DEFAULT NULL COMMENT '交易类型：NATIVE/JSAPI/APP/MP',
  `open_id` varchar(128) DEFAULT NULL COMMENT '用户OpenID',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=待支付, 1=支付成功, 2=支付失败, 3=已关闭, 4=已退款',
  `paid_amount` decimal(12,2) DEFAULT NULL COMMENT '实付金额',
  `paid_time` datetime DEFAULT NULL COMMENT '支付时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `notify_data` text COMMENT '回调原始数据',
  `callback_url` varchar(500) DEFAULT NULL COMMENT '业务回调地址',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单表';

-- 4.3 退款表
DROP TABLE IF EXISTS `sparkit_payment_refund`;
CREATE TABLE `sparkit_payment_refund` (
  `refund_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `order_id` bigint(20) NOT NULL COMMENT '关联订单ID',
  `refund_no` varchar(64) NOT NULL COMMENT '退款单号（唯一）',
  `payment_no` varchar(64) NOT NULL COMMENT '原支付流水号',
  `refund_amount` decimal(12,2) NOT NULL COMMENT '退款金额（元）',
  `refund_reason` varchar(500) DEFAULT NULL COMMENT '退款原因',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=退款中, 1=退款成功, 2=退款失败',
  `refund_time` datetime DEFAULT NULL COMMENT '退款完成时间',
  `notify_data` text COMMENT '回调原始数据',
  `create_by` varchar(64) DEFAULT NULL COMMENT '操作人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`refund_id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款表';

-- 4.4 对账表
DROP TABLE IF EXISTS `sparkit_payment_reconciliation`;
CREATE TABLE `sparkit_payment_reconciliation` (
  `recon_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `channel` varchar(32) NOT NULL COMMENT '支付渠道',
  `recon_date` date NOT NULL COMMENT '对账日期',
  `total_count` int(11) DEFAULT NULL COMMENT '总笔数',
  `success_count` int(11) DEFAULT NULL COMMENT '一致笔数',
  `diff_count` int(11) DEFAULT NULL COMMENT '差异笔数',
  `diff_amount` decimal(12,2) DEFAULT NULL COMMENT '差异金额',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=对账中, 1=完成, 2=异常',
  `diff_detail` text COMMENT '差异明细JSON',
  `recon_time` datetime DEFAULT NULL COMMENT '对账完成时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`recon_id`),
  UNIQUE KEY `uk_channel_date` (`channel`, `recon_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账表';

-- =====================================================
-- 五、通知模块表
-- =====================================================

-- 5.1 通知模板表
DROP TABLE IF EXISTS `sparkit_notify_template`;
CREATE TABLE `sparkit_notify_template` (
  `template_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `template_code` varchar(64) NOT NULL COMMENT '模板编码（唯一）',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `channel` varchar(20) NOT NULL COMMENT '渠道：sms/email/wechat/unipush/site',
  `title` varchar(255) DEFAULT NULL COMMENT '模板标题',
  `content` text NOT NULL COMMENT '模板内容（支持变量占位符 ${var}）',
  `variables` varchar(500) DEFAULT NULL COMMENT '变量列表JSON',
  `third_party_id` varchar(128) DEFAULT NULL COMMENT '第三方平台模板ID',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=停用, 1=启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知模板表';

-- 5.2 通知发送记录表
DROP TABLE IF EXISTS `sparkit_notify_record`;
CREATE TABLE `sparkit_notify_record` (
  `record_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `template_id` bigint(20) DEFAULT NULL COMMENT '模板ID',
  `channel` varchar(20) NOT NULL COMMENT '发送渠道',
  `sender` varchar(128) DEFAULT NULL COMMENT '发送方标识',
  `receiver` varchar(128) NOT NULL COMMENT '接收方（手机号/邮箱/OpenID/设备Token）',
  `title` varchar(255) DEFAULT NULL COMMENT '发送标题',
  `content` text NOT NULL COMMENT '发送内容',
  `params` text COMMENT '实际参数JSON',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=发送中, 1=成功, 2=失败',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_receiver` (`receiver`),
  KEY `idx_channel` (`channel`),
  KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知发送记录表';

-- 5.3 站内信表
DROP TABLE IF EXISTS `sparkit_notify_message`;
CREATE TABLE `sparkit_notify_message` (
  `message_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `user_id` bigint(20) NOT NULL COMMENT '接收用户ID',
  `user_type` varchar(10) NOT NULL COMMENT '用户类型：admin/user',
  `title` varchar(255) NOT NULL COMMENT '消息标题',
  `content` text NOT NULL COMMENT '消息内容',
  `message_type` varchar(20) NOT NULL COMMENT '消息类型：system/notice/remind',
  `is_read` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已读：0=未读, 1=已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `sender_id` bigint(20) DEFAULT NULL COMMENT '发送者ID',
  `sender_name` varchar(64) DEFAULT NULL COMMENT '发送者名称',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_user_id_type` (`user_id`, `user_type`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信表';

-- =====================================================
-- 六、AI模块表
-- =====================================================

-- 6.1 AI模型配置表
DROP TABLE IF EXISTS `sparkit_ai_config`;
CREATE TABLE `sparkit_ai_config` (
  `config_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `config_name` varchar(64) NOT NULL COMMENT '配置名称',
  `provider` varchar(32) NOT NULL COMMENT '服务商：deepseek/xiaomi/aliyun_bailian/openai/azure/qwen/zhipu/moonshot/baidu/custom',
  `provider_name` varchar(64) DEFAULT NULL COMMENT '服务商显示名称',
  `model_name` varchar(64) NOT NULL COMMENT '模型名称',
  `model_type` varchar(20) NOT NULL COMMENT '模型类型：text/image/video/audio/multimodal',
  `api_key` varchar(500) NOT NULL COMMENT 'API Key（加密存储）',
  `api_secret` varchar(500) DEFAULT NULL COMMENT 'API Secret（加密存储）',
  `api_base_url` varchar(500) NOT NULL COMMENT 'API基础地址',
  `max_tokens` int(11) DEFAULT NULL COMMENT '最大输出Token数',
  `temperature` decimal(3,2) DEFAULT NULL COMMENT '温度参数',
  `top_p` decimal(3,2) DEFAULT NULL COMMENT 'Top-P采样参数',
  `context_length` int(11) DEFAULT NULL COMMENT '上下文窗口大小',
  `support_function_call` tinyint(1) DEFAULT '0' COMMENT '是否支持Function Call',
  `support_vision` tinyint(1) DEFAULT '0' COMMENT '是否支持视觉',
  `priority` int(11) DEFAULT '0' COMMENT '优先级（数字越小越优先）',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=停用, 1=启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`config_id`),
  KEY `idx_provider` (`provider`),
  KEY `idx_model_type` (`model_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- 6.2 AI会话表
DROP TABLE IF EXISTS `sparkit_ai_session`;
CREATE TABLE `sparkit_ai_session` (
  `session_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `session_uuid` varchar(64) NOT NULL COMMENT '会话唯一标识（对外暴露）',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `user_type` varchar(10) NOT NULL COMMENT '用户类型：admin/user',
  `config_id` bigint(20) DEFAULT NULL COMMENT '使用的AI配置ID',
  `title` varchar(255) DEFAULT NULL COMMENT '会话标题',
  `session_type` varchar(20) NOT NULL COMMENT '会话类型：chat/image_gen/video_gen/audio/code',
  `system_prompt` text COMMENT '系统提示词',
  `model_name` varchar(64) DEFAULT NULL COMMENT '实际使用的模型名称',
  `total_tokens` bigint(20) DEFAULT '0' COMMENT '累计消耗Token数',
  `message_count` int(11) DEFAULT '0' COMMENT '消息总数',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=已归档, 1=活跃中',
  `pinned` tinyint(1) DEFAULT '0' COMMENT '是否置顶：0=否, 1=是',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '最后活跃时间',
  PRIMARY KEY (`session_id`),
  UNIQUE KEY `uk_session_uuid` (`session_uuid`),
  KEY `idx_user_id_type` (`user_id`, `user_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话表';

-- 6.3 AI消息表
DROP TABLE IF EXISTS `sparkit_ai_message`;
CREATE TABLE `sparkit_ai_message` (
  `message_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `session_id` bigint(20) NOT NULL COMMENT '所属会话ID',
  `role` varchar(20) NOT NULL COMMENT '角色：system/user/assistant/tool',
  `content` longtext NOT NULL COMMENT '消息内容',
  `content_type` varchar(20) DEFAULT NULL COMMENT '内容类型：text/image_url/file/audio',
  `reasoning_content` longtext COMMENT '推理过程内容',
  `token_count` int(11) DEFAULT NULL COMMENT '本条消息消耗Token数',
  `model_name` varchar(64) DEFAULT NULL COMMENT '生成时使用的模型名称',
  `finish_reason` varchar(20) DEFAULT NULL COMMENT '结束原因：stop/length/content_filter/tool_calls',
  `metadata` text COMMENT '附加元数据JSON',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父消息ID',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除：0=正常, 1=已删除',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_role` (`role`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI消息表';

-- 6.4 AI图片生成记录表
DROP TABLE IF EXISTS `sparkit_ai_image_record`;
CREATE TABLE `sparkit_ai_image_record` (
  `record_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `session_id` bigint(20) DEFAULT NULL COMMENT '关联会话ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `user_type` varchar(10) NOT NULL COMMENT '用户类型：admin/user',
  `config_id` bigint(20) DEFAULT NULL COMMENT 'AI配置ID',
  `model_name` varchar(64) DEFAULT NULL COMMENT '使用的模型名称',
  `prompt` text NOT NULL COMMENT '生图提示词',
  `negative_prompt` text COMMENT '反向提示词',
  `image_url` varchar(500) DEFAULT NULL COMMENT '生成图片URL',
  `image_size` varchar(20) DEFAULT NULL COMMENT '图片尺寸',
  `quality` varchar(20) DEFAULT NULL COMMENT '质量：standard/hd',
  `style` varchar(20) DEFAULT NULL COMMENT '风格',
  `seed` bigint(20) DEFAULT NULL COMMENT '随机种子',
  `cost` decimal(10,4) DEFAULT NULL COMMENT '消耗费用（元）',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=生成中, 1=成功, 2=失败',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_user_id_type` (`user_id`, `user_type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI图片生成记录表';

-- 6.5 AI视频生成记录表
DROP TABLE IF EXISTS `sparkit_ai_video_record`;
CREATE TABLE `sparkit_ai_video_record` (
  `record_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `session_id` bigint(20) DEFAULT NULL COMMENT '关联会话ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `user_type` varchar(10) NOT NULL COMMENT '用户类型：admin/user',
  `config_id` bigint(20) DEFAULT NULL COMMENT 'AI配置ID',
  `model_name` varchar(64) DEFAULT NULL COMMENT '使用的模型名称',
  `prompt` text NOT NULL COMMENT '生成视频提示词',
  `video_url` varchar(500) DEFAULT NULL COMMENT '生成视频URL',
  `duration` int(11) DEFAULT NULL COMMENT '视频时长（秒）',
  `resolution` varchar(20) DEFAULT NULL COMMENT '分辨率',
  `frame_rate` int(11) DEFAULT NULL COMMENT '帧率',
  `cost` decimal(10,4) DEFAULT NULL COMMENT '消耗费用（元）',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=生成中, 1=成功, 2=失败',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_user_id_type` (`user_id`, `user_type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI视频生成记录表';

-- 6.6 AI API用量统计表
DROP TABLE IF EXISTS `sparkit_ai_api_usage`;
CREATE TABLE `sparkit_ai_api_usage` (
  `usage_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `config_id` bigint(20) NOT NULL COMMENT 'AI配置ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID（NULL表示系统统计）',
  `user_type` varchar(10) DEFAULT NULL COMMENT '用户类型',
  `model_name` varchar(64) NOT NULL COMMENT '模型名称',
  `request_count` int(11) NOT NULL COMMENT '请求次数',
  `prompt_tokens` bigint(20) DEFAULT '0' COMMENT '输入Token数',
  `completion_tokens` bigint(20) DEFAULT '0' COMMENT '输出Token数',
  `total_tokens` bigint(20) DEFAULT '0' COMMENT '总Token数',
  `cost` decimal(10,4) DEFAULT '0.0000' COMMENT '预估费用（元）',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`usage_id`),
  KEY `idx_config_date` (`config_id`, `stat_date`),
  KEY `idx_user_date` (`user_id`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI API用量统计表';

-- 6.7 知识库表
DROP TABLE IF EXISTS `sparkit_ai_knowledge_base`;
CREATE TABLE `sparkit_ai_knowledge_base` (
  `kb_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `kb_name` varchar(128) NOT NULL COMMENT '知识库名称',
  `kb_desc` varchar(500) DEFAULT NULL COMMENT '知识库描述',
  `embedding_model` varchar(64) DEFAULT NULL COMMENT '嵌入模型名称',
  `chunk_size` int(11) DEFAULT '500' COMMENT '分块大小',
  `chunk_overlap` int(11) DEFAULT '50' COMMENT '分块重叠大小',
  `document_count` int(11) DEFAULT '0' COMMENT '文档数量',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=停用, 1=启用',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`kb_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- 6.8 知识库文档表
DROP TABLE IF EXISTS `sparkit_ai_kb_document`;
CREATE TABLE `sparkit_ai_kb_document` (
  `doc_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `kb_id` bigint(20) NOT NULL COMMENT '所属知识库ID',
  `doc_name` varchar(255) NOT NULL COMMENT '文档名称',
  `doc_type` varchar(20) NOT NULL COMMENT '文档类型：txt/pdf/md/html/url/json',
  `source_url` varchar(500) DEFAULT NULL COMMENT '来源URL',
  `file_id` bigint(20) DEFAULT NULL COMMENT '关联文件ID',
  `chunk_count` int(11) DEFAULT '0' COMMENT '分块数量',
  `char_count` bigint(20) DEFAULT '0' COMMENT '字符总数',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=处理中, 1=已完成, 2=失败',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`doc_id`),
  KEY `idx_kb_id` (`kb_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

-- =====================================================
-- 七、新闻模块表
-- =====================================================

-- 7.1 新闻分类表
DROP TABLE IF EXISTS `sparkit_news_category`;
CREATE TABLE `sparkit_news_category` (
  `category_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父分类ID（0表示顶级）',
  `category_name` varchar(64) NOT NULL COMMENT '分类名称',
  `category_code` varchar(64) NOT NULL COMMENT '分类编码（唯一）',
  `icon` varchar(500) DEFAULT NULL COMMENT '分类图标',
  `description` varchar(255) DEFAULT NULL COMMENT '分类描述',
  `sort` int(11) DEFAULT '0' COMMENT '排序号',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=停用, 1=启用',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除：0=正常, 1=已删除',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `uk_category_code` (`category_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻分类表';

-- 7.2 新闻文章表
DROP TABLE IF EXISTS `sparkit_news_article`;
CREATE TABLE `sparkit_news_article` (
  `article_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `category_id` bigint(20) DEFAULT NULL COMMENT '所属分类ID',
  `title` varchar(255) NOT NULL COMMENT '文章标题',
  `sub_title` varchar(255) DEFAULT NULL COMMENT '副标题',
  `summary` varchar(500) DEFAULT NULL COMMENT '文章摘要',
  `content` longtext NOT NULL COMMENT '文章内容（富文本）',
  `content_md` longtext COMMENT 'Markdown格式内容',
  `cover_image` varchar(500) DEFAULT NULL COMMENT '封面图',
  `images` text COMMENT '文章配图JSON数组',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签（逗号分隔）',
  `keywords` varchar(500) DEFAULT NULL COMMENT 'SEO关键词',
  `source` varchar(128) DEFAULT NULL COMMENT '来源',
  `source_url` varchar(500) DEFAULT NULL COMMENT '来源链接',
  `author` varchar(64) DEFAULT NULL COMMENT '作者',
  `is_ai_generated` tinyint(1) DEFAULT '0' COMMENT '是否AI生成：0=否, 1=是',
  `ai_model` varchar(64) DEFAULT NULL COMMENT 'AI生成使用的模型',
  `ai_prompt` text COMMENT 'AI生成使用的提示词',
  `view_count` int(11) DEFAULT '0' COMMENT '浏览次数',
  `like_count` int(11) DEFAULT '0' COMMENT '点赞数',
  `comment_count` int(11) DEFAULT '0' COMMENT '评论数',
  `share_count` int(11) DEFAULT '0' COMMENT '分享数',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=草稿, 1=已发布, 2=已下架',
  `is_top` tinyint(1) DEFAULT '0' COMMENT '是否置顶：0=否, 1=是',
  `is_recommend` tinyint(1) DEFAULT '0' COMMENT '是否推荐：0=否, 1=是',
  `is_original` tinyint(1) DEFAULT '1' COMMENT '是否原创：0=转载, 1=原创',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除：0=正常, 1=已删除',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`article_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_is_top` (`is_top`),
  KEY `idx_is_recommend` (`is_recommend`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻文章表';

-- 7.3 文章标签关联表
DROP TABLE IF EXISTS `sparkit_news_article_tag`;
CREATE TABLE `sparkit_news_article_tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `article_id` bigint(20) NOT NULL COMMENT '文章ID',
  `tag_id` bigint(20) NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_tag` (`article_id`, `tag_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签关联表';

-- 7.4 标签表
DROP TABLE IF EXISTS `sparkit_news_tag`;
CREATE TABLE `sparkit_news_tag` (
  `tag_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `tag_name` varchar(64) NOT NULL COMMENT '标签名称',
  `tag_code` varchar(64) NOT NULL COMMENT '标签编码（唯一）',
  `usage_count` int(11) DEFAULT '0' COMMENT '引用次数',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=停用, 1=启用',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `uk_tag_code` (`tag_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- 7.5 文章评论表
DROP TABLE IF EXISTS `sparkit_news_comment`;
CREATE TABLE `sparkit_news_comment` (
  `comment_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `article_id` bigint(20) NOT NULL COMMENT '文章ID',
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父评论ID（0表示一级评论）',
  `user_id` bigint(20) NOT NULL COMMENT '评论用户ID',
  `user_type` varchar(10) NOT NULL COMMENT '用户类型：admin/user',
  `content` text NOT NULL COMMENT '评论内容',
  `like_count` int(11) DEFAULT '0' COMMENT '点赞数',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=待审核, 1=已通过, 2=已拒绝, 3=已删除',
  `ip_address` varchar(128) DEFAULT NULL COMMENT '评论IP',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_by` varchar(64) DEFAULT NULL COMMENT '审核人',
  PRIMARY KEY (`comment_id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章评论表';

-- 7.6 AI新闻采集/生成任务表
DROP TABLE IF EXISTS `sparkit_news_ai_task`;
CREATE TABLE `sparkit_news_ai_task` (
  `task_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `task_name` varchar(128) NOT NULL COMMENT '任务名称',
  `task_type` varchar(20) NOT NULL COMMENT '任务类型：collect/generate/rewrite/translate',
  `category_id` bigint(20) DEFAULT NULL COMMENT '目标分类ID',
  `ai_config_id` bigint(20) DEFAULT NULL COMMENT 'AI配置ID',
  `source_urls` text COMMENT '采集源URL（JSON数组）',
  `prompt_template` text COMMENT '提示词模板',
  `keywords` varchar(500) DEFAULT NULL COMMENT '关键词（逗号分隔）',
  `generate_count` int(11) DEFAULT '1' COMMENT '每次生成数量',
  `cron_expression` varchar(64) DEFAULT NULL COMMENT 'Cron表达式（定时执行）',
  `last_run_time` datetime DEFAULT NULL COMMENT '上次执行时间',
  `next_run_time` datetime DEFAULT NULL COMMENT '下次执行时间',
  `total_generated` int(11) DEFAULT '0' COMMENT '累计生成数量',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=停用, 1=启用, 2=运行中',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_task_type` (`task_type`),
  KEY `idx_status` (`status`),
  KEY `idx_next_run_time` (`next_run_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI新闻采集/生成任务表';

-- 7.7 AI新闻任务执行日志表
DROP TABLE IF EXISTS `sparkit_news_ai_task_log`;
CREATE TABLE `sparkit_news_ai_task_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `task_id` bigint(20) NOT NULL COMMENT '任务ID',
  `article_id` bigint(20) DEFAULT NULL COMMENT '生成的文章ID',
  `execute_status` tinyint(1) NOT NULL COMMENT '0=执行中, 1=成功, 2=失败',
  `prompt_used` text COMMENT '实际使用的Prompt',
  `response_data` longtext COMMENT 'AI返回原始数据',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `cost` decimal(10,4) DEFAULT NULL COMMENT '消耗费用（元）',
  `execute_time` datetime DEFAULT NULL COMMENT '执行时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_execute_status` (`execute_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI新闻任务执行日志表';

-- =====================================================
-- 八、定时任务表
-- =====================================================

-- 8.1 定时任务定义表
DROP TABLE IF EXISTS `sparkit_job`;
CREATE TABLE `sparkit_job` (
  `job_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `job_name` varchar(128) NOT NULL COMMENT '任务名称',
  `job_group` varchar(128) NOT NULL COMMENT '任务组',
  `job_class` varchar(255) NOT NULL COMMENT '执行类全限定名',
  `cron_expression` varchar(64) NOT NULL COMMENT 'Cron表达式',
  `job_params` text COMMENT '任务参数JSON',
  `description` varchar(500) DEFAULT NULL COMMENT '任务描述',
  `concurrent` tinyint(1) DEFAULT '1' COMMENT '是否允许并发：0=禁止, 1=允许',
  `misfire_policy` tinyint(1) DEFAULT '0' COMMENT '错过策略：0=立即执行, 1=放弃, 2=合并到下次',
  `retry_count` int(11) DEFAULT '0' COMMENT '失败重试次数',
  `retry_interval` int(11) DEFAULT '30' COMMENT '重试间隔（秒）',
  `timeout` int(11) DEFAULT '0' COMMENT '超时时间（秒，0表示不限制）',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=暂停, 1=运行中',
  `last_execute_time` datetime DEFAULT NULL COMMENT '上次执行时间',
  `next_execute_time` datetime DEFAULT NULL COMMENT '下次执行时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`job_id`),
  UNIQUE KEY `uk_job_name_group` (`job_name`, `job_group`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务定义表';

-- 8.2 定时任务执行日志表
DROP TABLE IF EXISTS `sparkit_job_log`;
CREATE TABLE `sparkit_job_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `job_id` bigint(20) NOT NULL COMMENT '任务ID',
  `job_name` varchar(128) NOT NULL COMMENT '任务名称',
  `job_group` varchar(128) NOT NULL COMMENT '任务组',
  `execute_ip` varchar(64) DEFAULT NULL COMMENT '执行服务器IP',
  `execute_status` tinyint(1) NOT NULL COMMENT '0=执行中, 1=成功, 2=失败',
  `execute_params` text COMMENT '实际执行参数',
  `result_message` text COMMENT '执行结果信息',
  `error_stack` text COMMENT '异常堆栈',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint(20) DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_execute_status` (`execute_status`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务执行日志表';

-- =====================================================
-- 九、日志与审计表
-- =====================================================

-- 9.1 操作日志表
DROP TABLE IF EXISTS `sparkit_oper_log`;
CREATE TABLE `sparkit_oper_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `module` varchar(64) DEFAULT NULL COMMENT '操作模块',
  `business_type` varchar(32) DEFAULT NULL COMMENT '业务类型：insert/update/delete/export/import/grant/other',
  `method` varchar(255) DEFAULT NULL COMMENT '请求方法全路径',
  `request_method` varchar(10) DEFAULT NULL COMMENT 'HTTP方法：GET/POST/PUT/DELETE',
  `operator_type` varchar(10) DEFAULT NULL COMMENT '操作人类型：admin/user',
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人名称',
  `operator_ip` varchar(128) DEFAULT NULL COMMENT '操作IP',
  `operator_location` varchar(255) DEFAULT NULL COMMENT '操作地点',
  `request_url` varchar(500) DEFAULT NULL COMMENT '请求URL',
  `request_params` text COMMENT '请求参数',
  `response_result` text COMMENT '响应结果（截取前2000字符）',
  `cost_time` bigint(20) DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `status` tinyint(1) DEFAULT '1' COMMENT '0=失败, 1=成功',
  `error_msg` text COMMENT '错误信息',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_module` (`module`),
  KEY `idx_business_type` (`business_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 9.2 登录日志表
DROP TABLE IF EXISTS `sparkit_login_log`;
CREATE TABLE `sparkit_login_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `login_type` varchar(10) NOT NULL COMMENT '登录类型：admin/user',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `username` varchar(64) NOT NULL COMMENT '登录用户名',
  `login_method` varchar(20) DEFAULT NULL COMMENT '登录方式：password/sms/email/wechat/wecom/qq/weibo/github/dingtalk',
  `ip_address` varchar(128) DEFAULT NULL COMMENT '登录IP',
  `location` varchar(255) DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(128) DEFAULT NULL COMMENT '浏览器类型',
  `os` varchar(128) DEFAULT NULL COMMENT '操作系统',
  `device` varchar(20) DEFAULT NULL COMMENT '设备类型：pc/mobile/tablet',
  `status` tinyint(1) NOT NULL COMMENT '0=失败, 1=成功',
  `fail_reason` varchar(255) DEFAULT NULL COMMENT '失败原因',
  `login_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- 9.3 API访问日志表
DROP TABLE IF EXISTS `sparkit_api_access_log`;
CREATE TABLE `sparkit_api_access_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `app_key` varchar(64) DEFAULT NULL COMMENT '应用标识',
  `user_id` bigint(20) DEFAULT NULL COMMENT '访问用户ID',
  `user_type` varchar(10) DEFAULT NULL COMMENT '用户类型',
  `request_ip` varchar(128) DEFAULT NULL COMMENT '请求IP',
  `request_url` varchar(500) DEFAULT NULL COMMENT '请求URL',
  `http_method` varchar(10) DEFAULT NULL COMMENT 'HTTP方法',
  `request_params` text COMMENT '请求参数',
  `request_body` text COMMENT '请求体',
  `response_code` int(11) DEFAULT NULL COMMENT '响应状态码',
  `response_body` text COMMENT '响应体（截取前1000字符）',
  `cost_time` int(11) DEFAULT NULL COMMENT '耗时（毫秒）',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API访问日志表';

-- 9.4 敏感操作审计日志表
DROP TABLE IF EXISTS `sparkit_sensitive_log`;
CREATE TABLE `sparkit_sensitive_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `action` varchar(64) NOT NULL COMMENT '操作动作',
  `target_type` varchar(32) NOT NULL COMMENT '目标类型：user/role/config/data',
  `target_id` varchar(128) DEFAULT NULL COMMENT '目标ID',
  `target_name` varchar(255) DEFAULT NULL COMMENT '目标名称',
  `detail` text COMMENT '操作详情JSON',
  `operator_id` bigint(20) NOT NULL COMMENT '操作人ID',
  `operator_type` varchar(10) NOT NULL COMMENT '操作人类型：admin/system',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人名称',
  `operator_ip` varchar(128) DEFAULT NULL COMMENT '操作IP',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_action` (`action`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感操作审计日志表';

-- =====================================================
-- 十、多租户与系统配置扩展表
-- =====================================================

-- 10.1 租户表
DROP TABLE IF EXISTS `sparkit_tenant`;
CREATE TABLE `sparkit_tenant` (
  `tenant_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `tenant_code` varchar(64) NOT NULL COMMENT '租户编码（唯一）',
  `tenant_name` varchar(128) NOT NULL COMMENT '租户名称',
  `logo` varchar(500) DEFAULT NULL COMMENT '租户Logo',
  `contact_name` varchar(64) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `contact_email` varchar(128) DEFAULT NULL COMMENT '联系邮箱',
  `db_type` varchar(20) DEFAULT 'shared' COMMENT '数据库隔离方式：shared/independent',
  `db_url` varchar(500) DEFAULT NULL COMMENT '独立数据库连接地址',
  `db_username` varchar(128) DEFAULT NULL COMMENT '数据库用户名（加密）',
  `db_password` varchar(255) DEFAULT NULL COMMENT '数据库密码（加密）',
  `domain` varchar(255) DEFAULT NULL COMMENT '专属域名',
  `expire_time` datetime DEFAULT NULL COMMENT '到期时间',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '0=停用, 1=启用, 2=已过期',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`tenant_id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`),
  KEY `idx_domain` (`domain`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

-- 10.2 租户个性化配置表
DROP TABLE IF EXISTS `sparkit_tenant_config`;
CREATE TABLE `sparkit_tenant_config` (
  `config_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `tenant_id` bigint(20) NOT NULL COMMENT '租户ID',
  `config_key` varchar(128) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值',
  `config_type` varchar(20) DEFAULT 'string' COMMENT '配置类型：string/number/boolean/json/image',
  `description` varchar(255) DEFAULT NULL COMMENT '配置说明',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_tenant_key` (`tenant_id`, `config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户个性化配置表';

-- 10.3 系统版本管理表
DROP TABLE IF EXISTS `sparkit_sys_version`;
CREATE TABLE `sparkit_sys_version` (
  `version_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `platform` varchar(20) NOT NULL COMMENT '平台：admin_web/web/android/ios/mp_wechat/mp_alipay',
  `version_code` int(11) NOT NULL COMMENT '版本号（数值，用于比较）',
  `version_name` varchar(32) NOT NULL COMMENT '版本名称（如 1.0.0）',
  `title` varchar(255) DEFAULT NULL COMMENT '更新标题',
  `content` text COMMENT '更新内容',
  `download_url` varchar(500) DEFAULT NULL COMMENT '下载地址',
  `app_store_url` varchar(500) DEFAULT NULL COMMENT '应用商店地址',
  `min_version_code` int(11) DEFAULT NULL COMMENT '最低兼容版本号',
  `force_update` tinyint(1) DEFAULT '0' COMMENT '是否强制更新：0=否, 1=是',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=未发布, 1=已发布',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`version_id`),
  UNIQUE KEY `uk_platform_code` (`platform`, `version_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统版本管理表';

-- 10.4 用户反馈表
DROP TABLE IF EXISTS `sparkit_sys_feedback`;
CREATE TABLE `sparkit_sys_feedback` (
  `feedback_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `user_type` varchar(10) NOT NULL COMMENT '用户类型：admin/user',
  `category` varchar(32) DEFAULT NULL COMMENT '反馈分类：bug/suggestion/question/complaint/other',
  `title` varchar(255) DEFAULT NULL COMMENT '反馈标题',
  `content` text NOT NULL COMMENT '反馈内容',
  `images` text COMMENT '截图JSON数组',
  `contact` varchar(128) DEFAULT NULL COMMENT '联系方式',
  `platform` varchar(20) DEFAULT NULL COMMENT '来源平台：web/android/ios/mp',
  `app_version` varchar(32) DEFAULT NULL COMMENT 'App版本号',
  `device_info` varchar(255) DEFAULT NULL COMMENT '设备信息',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0=未处理, 1=处理中, 2=已完成, 3=已关闭',
  `remark` varchar(500) DEFAULT NULL COMMENT '处理备注',
  `handle_by` varchar(64) DEFAULT NULL COMMENT '处理人',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`feedback_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

-- =====================================================
-- 十一、代码生成器表
-- =====================================================

-- 11.1 代码生成-表信息
DROP TABLE IF EXISTS `sparkit_gen_table`;
CREATE TABLE `sparkit_gen_table` (
  `table_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `table_name` varchar(200) NOT NULL COMMENT '表名称',
  `table_comment` varchar(500) DEFAULT NULL COMMENT '表描述',
  `class_name` varchar(200) DEFAULT NULL COMMENT '实体类名称',
  `package_name` varchar(200) DEFAULT NULL COMMENT '包路径',
  `module_name` varchar(100) DEFAULT NULL COMMENT '模块名',
  `business_name` varchar(100) DEFAULT NULL COMMENT '业务名',
  `function_name` varchar(200) DEFAULT NULL COMMENT '功能名称',
  `function_author` varchar(100) DEFAULT NULL COMMENT '功能作者',
  `gen_type` varchar(20) DEFAULT 'basic' COMMENT '生成方式：basic/basic-crud/force-crud/tree/table',
  `gen_path` varchar(200) DEFAULT NULL COMMENT '代码生成路径',
  `options` text COMMENT '额外选项JSON',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除：0=正常, 1=已删除',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成-表信息';

-- 11.2 代码生成-列信息
DROP TABLE IF EXISTS `sparkit_gen_table_column`;
CREATE TABLE `sparkit_gen_table_column` (
  `column_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键自增',
  `table_id` bigint(20) NOT NULL COMMENT '所属表ID',
  `column_name` varchar(200) NOT NULL COMMENT '列名称',
  `column_comment` varchar(500) DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) NOT NULL COMMENT '列类型',
  `java_type` varchar(100) DEFAULT NULL COMMENT 'Java类型',
  `java_field` varchar(200) DEFAULT NULL COMMENT 'Java字段名',
  `is_pk` tinyint(1) DEFAULT '0' COMMENT '是否主键：0=否, 1=是',
  `is_increment` tinyint(1) DEFAULT '0' COMMENT '是否自增：0=否, 1=是',
  `is_required` tinyint(1) DEFAULT '0' COMMENT '是否必填：0=否, 1=是',
  `is_insert` tinyint(1) DEFAULT '1' COMMENT '是否插入字段：0=否, 1=是',
  `is_edit` tinyint(1) DEFAULT '1' COMMENT '是否编辑字段：0=否, 1=是',
  `is_list` tinyint(1) DEFAULT '1' COMMENT '是否列表字段：0=否, 1=是',
  `is_query` tinyint(1) DEFAULT '1' COMMENT '是否查询字段：0=否, 1=是',
  `query_type` varchar(20) DEFAULT 'EQ' COMMENT '查询方式：EQ/LIKE/BETWEEN/NE/GT/LT/GE/LE',
  `html_type` varchar(20) DEFAULT 'input' COMMENT '表单类型：input/textarea/select/radio/checkbox/datetime/image/editor',
  `dict_type` varchar(100) DEFAULT NULL COMMENT '字典类型',
  `sort` int(11) DEFAULT '0' COMMENT '排序号',
  PRIMARY KEY (`column_id`),
  KEY `idx_table_id` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成-列信息';

-- =====================================================
-- 初始化数据：默认管理员账号 admin/123456
-- 密码 BCrypt 加密：$2a$10$rLkX3JLq5eZ5Z5Z5Z5Z5ZO
-- =====================================================
INSERT INTO `sparkit_admin_user` (`username`, `password`, `nickname`, `status`) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 1);

-- 默认角色
INSERT INTO `sparkit_role` (`role_name`, `role_key`, `role_sort`, `status`) VALUES
('超级管理员', 'admin', 1, 1),
('普通管理员', 'common', 2, 1);

-- 绑定管理员角色
INSERT INTO `sparkit_admin_user_role` (`admin_id`, `role_id`) VALUES (1, 1);

-- 默认配置
INSERT INTO `sparkit_config` (`config_group`, `config_key`, `config_name`, `config_value`, `config_type`, `built_in`) VALUES
('system', 'sys.app.name', '应用名称', 'Sparkit', 'text', 1),
('system', 'sys.app.logo', '应用Logo', '/static/logo.png', 'image', 1),
('security', 'security.login.retry_max', '登录最大重试次数', '5', 'number', 1),
('security', 'security.login.lock_minutes', '登录锁定分钟数', '30', 'number', 1),
('storage', 'storage.image_compress_enabled', '图片压缩开关', 'false', 'switch', 1),
('storage', 'storage.image_compress_quality', '图片压缩质量', '0.8', 'number', 1),
('storage', 'storage.video_transcode_enabled', '视频转码开关', 'false', 'switch', 1);
