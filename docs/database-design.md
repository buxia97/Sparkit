# Sparkit 数据库设计文档

> 数据库：MySQL 5.5 ~ 8.0 | 字符集：utf8mb4 | 排序规则：utf8mb4_general_ci | 所有表前缀：`sparkit`

---

## 一、设计原则

### 1.1 管理员与用户分离

- **`sparkit_admin_user`**：管理后台管理员，拥有细粒度权限控制
- **`sparkit_user`**：前端普通用户（C 端），不参与后台权限体系

### 1.2 细粒度权限模型（RBAC）

管理员权限通过 **角色 -> 菜单** 绑定实现。菜单表 `sparkit_menu` 同时承载目录、菜单、按钮/API 接口三种类型：

- **D（目录）**：仅用于前端菜单分组，无实际路由
- **M（菜单）**：对应前端页面路由
- **B（按钮/API）**：对应每个接口的精确权限标识，支持 HTTP Method + API Path 匹配

管理员通过角色获得菜单权限，每个接口可独立授权给角色。

### 1.3 统一配置管理

所有系统参数集中在 `sparkit_config` 表中，通过 `config_group` 分组、`config_key` 唯一标识，支持文本、图片、开关、JSON、数字等多种值类型。

### 1.4 通用字段约定

以下字段在多数表中复用，不再逐表重复解释：

| 字段 | 类型 | 说明 |
|------|------|------|
| create_by | varchar(64) | 创建人 |
| create_time | datetime | 创建时间 |
| update_by | varchar(64) | 更新人 |
| update_time | datetime | 更新时间 |
| remark | varchar(500) | 备注 |
| status | tinyint | 状态（0=停用/禁用, 1=启用/正常） |
| deleted | tinyint | 逻辑删除（0=未删除, 1=已删除） |
| sort | int | 排序号 |

---

## 二、ER 关系总览

```
                              ┌──────────────────────┐
                              │   sparkit_tenant      │
                              │   (多租户)             │
                              └──────────┬───────────┘
                                         │
┌──────────────────────┐     ┌───────────┴───────────┐
│  sparkit_admin_user  │────>│  sparkit_admin_user_   │
│  (管理员)             │     │  role (管理-角色)       │
└──────────┬───────────┘     └───────────┬───────────┘
           │                             │
           │                     ┌───────┴───────┐
           │                     │  sparkit_role │────> sparkit_role_menu <─── sparkit_menu
           │                     │  (角色)        │                            (菜单/API权限)
           │                     └───────────────┘
           │
           ├──> sparkit_dept (部门) <── sparkit_post (岗位)
           │
           └──> sparkit_login_log / sparkit_oper_log (日志)

┌──────────────────────┐
│    sparkit_config    │  <── 统一配置表 (所有参数集中管理)
└──────────────────────┘

┌──────────────────────┐     ┌──────────────────────┐
│    sparkit_user      │────>│ sparkit_user_third_   │
│    (C端用户)          │     │ party (第三方绑定)     │
└──────────┬───────────┘     └──────────────────────┘
           │
           ├──> sparkit_user_verify_code (验证码)
           └──> sparkit_user_blacklist (黑名单)

┌──────────────────────┐     ┌──────────────────────┐
│  sparkit_storage_    │────>│    sparkit_file       │
│  config (存储源)      │     │    (文件记录)          │
└──────────────────────┘     └──────────┬───────────┘
                                        │
                                        └──> sparkit_file_chunk (分片)

┌──────────────────────┐
│  sparkit_payment_    │────> sparkit_payment_order ──> sparkit_payment_refund
│  config (支付配置)     │
└──────────────────────┘     └──> sparkit_payment_reconciliation (对账)

┌──────────────────────┐
│  sparkit_notify_     │────> sparkit_notify_record
│  template (通知模板)   │
└──────────────────────┘     └──> sparkit_notify_message (站内信)

┌──────────────────────┐
│  sparkit_ai_config   │────> sparkit_ai_session ──> sparkit_ai_message
└──────────────────────┘

┌──────────────────────┐
│  sparkit_news_       │────> sparkit_news_article ──> sparkit_news_article_tag <── sparkit_news_tag
│  category (新闻分类)   │           │
└──────────────────────┘           └──> sparkit_news_comment

┌──────────────────────┐
│  sparkit_job (任务)   │────> sparkit_job_log
└──────────────────────┘
```

---

## 三、系统管理核心表

### 3.1 sparkit_admin_user（管理员用户表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| admin_id | bigint(20) | 是 | 主键，自增 |
| username | varchar(64) | 是 | 登录用户名，唯一 |
| password | varchar(128) | 是 | 加密密码（BCrypt） |
| nickname | varchar(64) | 否 | 显示名称 |
| avatar | varchar(500) | 否 | 头像 URL |
| email | varchar(128) | 否 | 邮箱 |
| phone | varchar(20) | 否 | 手机号 |
| gender | tinyint(1) | 否 | 性别：0=未知, 1=男, 2=女 |
| dept_id | bigint(20) | 否 | 所属部门 ID |
| login_ip | varchar(128) | 否 | 最后登录 IP |
| login_date | datetime | 否 | 最后登录时间 |
| login_count | int(11) | 否 | 累计登录次数 |
| pwd_reset_time | datetime | 否 | 密码最后修改时间 |
| status | tinyint(1) | 是 | 0=停用, 1=正常 |
| deleted | tinyint(1) | 是 | 逻辑删除：0=否, 1=是 |
| remark | varchar(500) | 否 | 备注 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`uk_username` (username), `idx_dept_id` (dept_id)

---

### 3.2 sparkit_role（角色表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| role_id | bigint(20) | 是 | 主键，自增 |
| role_name | varchar(64) | 是 | 角色名称 |
| role_key | varchar(64) | 是 | 角色标识（唯一，如 admin/common） |
| role_sort | int(11) | 否 | 排序号 |
| data_scope | tinyint(1) | 否 | 数据权限范围：1=全部, 2=自定义, 3=本部门, 4=本部门及子部门, 5=仅本人 |
| status | tinyint(1) | 是 | 0=停用, 1=正常 |
| deleted | tinyint(1) | 是 | 逻辑删除 |
| remark | varchar(500) | 否 | 备注 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`uk_role_key` (role_key)

---

### 3.3 sparkit_menu（菜单/权限表 -- 细粒度权限核心）

> 本表同时承载前端菜单和后端 API 接口权限，通过 `menu_type` 区分。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| menu_id | bigint(20) | 是 | 主键，自增 |
| parent_id | bigint(20) | 是 | 父菜单 ID，顶级为 0 |
| menu_name | varchar(64) | 是 | 菜单/权限名称 |
| menu_type | char(1) | 是 | **D**=目录, **M**=菜单, **B**=按钮/API 接口 |
| path | varchar(200) | 否 | 路由地址（菜单）或 API 路径（按钮） |
| component | varchar(255) | 否 | 前端组件路径（仅菜单类型使用） |
| perms | varchar(100) | 否 | 权限标识（如 `sys:user:list`） |
| icon | varchar(100) | 否 | 菜单图标 |
| sort | int(11) | 否 | 排序号 |
| status | tinyint(1) | 是 | 0=隐藏, 1=显示 |
| visible | tinyint(1) | 是 | 是否在菜单中显示：0=隐藏, 1=显示 |
| is_frame | tinyint(1) | 否 | 是否外链：0=否, 1=是 |
| method | varchar(10) | 否 | HTTP 方法（仅 API 按钮类型）：GET/POST/PUT/DELETE/PATCH |
| api_path | varchar(200) | 否 | 完整 API 路径（仅 API 按钮类型，用于精确匹配） |
| query | varchar(255) | 否 | 路由参数 |
| remark | varchar(500) | 否 | 备注 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**设计说明**：

- 当 `menu_type = 'B'` 时，记录的是 API 接口权限。例如：
  - `perms = "sys:user:list"`, `method = "GET"`, `api_path = "/api/admin/user/list"`
  - `perms = "sys:user:add"`, `method = "POST"`, `api_path = "/api/admin/user"`
- 管理员通过角色获得这些 B 类型权限，即可访问对应 API，实现每个接口独立授权。

**索引**：`idx_parent_id` (parent_id)

---

### 3.4 sparkit_role_menu（角色-菜单关联表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| role_id | bigint(20) | 是 | 角色 ID |
| menu_id | bigint(20) | 是 | 菜单 ID |

**联合主键**：(role_id, menu_id)

---

### 3.5 sparkit_admin_user_role（管理员-角色关联表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| admin_id | bigint(20) | 是 | 管理员 ID |
| role_id | bigint(20) | 是 | 角色 ID |

**联合主键**：(admin_id, role_id)

---

### 3.6 sparkit_dept（部门表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| dept_id | bigint(20) | 是 | 主键，自增 |
| parent_id | bigint(20) | 是 | 父部门 ID，顶级为 0 |
| dept_name | varchar(64) | 是 | 部门名称 |
| leader | varchar(64) | 否 | 负责人 |
| phone | varchar(20) | 否 | 联系电话 |
| email | varchar(128) | 否 | 邮箱 |
| sort | int(11) | 否 | 排序号 |
| status | tinyint(1) | 是 | 0=停用, 1=正常 |
| deleted | tinyint(1) | 是 | 逻辑删除 |
| remark | varchar(500) | 否 | 备注 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`idx_parent_id` (parent_id)

---

### 3.7 sparkit_post（岗位表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| post_id | bigint(20) | 是 | 主键，自增 |
| post_code | varchar(64) | 是 | 岗位编码（唯一） |
| post_name | varchar(64) | 是 | 岗位名称 |
| sort | int(11) | 否 | 排序号 |
| status | tinyint(1) | 是 | 0=停用, 1=正常 |
| remark | varchar(500) | 否 | 备注 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`uk_post_code` (post_code)

---

### 3.8 sparkit_admin_user_post（管理员-岗位关联表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| admin_id | bigint(20) | 是 | 管理员 ID |
| post_id | bigint(20) | 是 | 岗位 ID |

**联合主键**：(admin_id, post_id)

---

### 3.9 sparkit_config（统一系统配置表）

> 所有系统参数集中存储于此表，通过 `config_group` 分组，`config_key` 唯一标识。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| config_id | bigint(20) | 是 | 主键，自增 |
| config_group | varchar(64) | 是 | 配置分组（如：system/upload/sms/payment/ai/security） |
| config_key | varchar(128) | 是 | 配置键（唯一） |
| config_name | varchar(128) | 是 | 配置名称/说明 |
| config_value | text | 否 | 配置值 |
| config_type | varchar(20) | 是 | 值类型：text / image / switch / json / number / textarea |
| sort | int(11) | 否 | 排序号 |
| built_in | tinyint(1) | 否 | 是否内置：0=否(可删), 1=是(不可删) |
| remark | varchar(500) | 否 | 备注 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`uk_config_key` (config_key), `idx_config_group` (config_group)

**典型配置示例**：

| config_group | config_key | config_name | config_value | config_type |
|--------------|------------|-------------|--------------|-------------|
| system | sys.app.name | 应用名称 | Sparkit | text |
| system | sys.app.logo | 应用Logo | /static/logo.png | image |
| upload | upload.max_size | 上传文件最大大小 | 100MB | text |
| upload | upload.allowed_ext | 允许上传的文件类型 | jpg,png,pdf,docx | text |
| sms | sms.aliyun.access_key | 阿里云短信 AccessKey | xxxxx | text |
| payment | pay.wechat.mch_id | 微信商户号 | 1234567890 | text |
| security | security.login.retry_max | 登录最大重试次数 | 5 | number |
| security | security.login.lock_minutes | 登录锁定分钟数 | 30 | number |

---

### 3.10 sparkit_dict_type（字典类型表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| dict_id | bigint(20) | 是 | 主键，自增 |
| dict_type | varchar(100) | 是 | 字典类型（唯一，如 `sys_user_sex`） |
| dict_name | varchar(100) | 是 | 字典名称 |
| status | tinyint(1) | 是 | 0=停用, 1=正常 |
| remark | varchar(500) | 否 | 备注 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`uk_dict_type` (dict_type)

---

### 3.11 sparkit_dict_data（字典数据表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| data_id | bigint(20) | 是 | 主键，自增 |
| dict_type | varchar(100) | 是 | 字典类型（关联 sparkit_dict_type.dict_type） |
| dict_label | varchar(100) | 是 | 字典标签 |
| dict_value | varchar(100) | 是 | 字典值 |
| css_class | varchar(100) | 否 | 样式类名 |
| list_class | varchar(100) | 否 | 列表样式 |
| sort | int(11) | 否 | 排序号 |
| status | tinyint(1) | 是 | 0=停用, 1=正常 |
| is_default | tinyint(1) | 否 | 是否默认：0=否, 1=是 |
| remark | varchar(500) | 否 | 备注 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`idx_dict_type` (dict_type)

---

### 3.12 sparkit_region（地区表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| region_id | bigint(20) | 是 | 主键，自增 |
| parent_id | bigint(20) | 是 | 父级 ID，根为 0 |
| region_code | varchar(20) | 是 | 行政区划代码 |
| region_name | varchar(64) | 是 | 地区名称 |
| region_level | tinyint(1) | 是 | 层级：1=省, 2=市, 3=区/县 |
| sort | int(11) | 否 | 排序号 |
| status | tinyint(1) | 是 | 0=停用, 1=正常 |

**索引**：`idx_parent_id` (parent_id), `idx_region_code` (region_code)

---

### 3.13 sparkit_i18n（国际化表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| i18n_id | bigint(20) | 是 | 主键，自增 |
| lang_key | varchar(200) | 是 | 语言 Key |
| lang_code | varchar(20) | 是 | 语言编码：zh_CN / en_US / ja_JP / ko_KR |
| lang_value | varchar(500) | 是 | 翻译值 |
| module | varchar(64) | 否 | 所属模块 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**联合唯一索引**：`uk_lang_key_code` (lang_key, lang_code)

---

## 四、用户模块表（C 端）

### 4.1 sparkit_user（C 端用户表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| user_id | bigint(20) | 是 | 主键，自增 |
| username | varchar(64) | 是 | 用户名（唯一） |
| password | varchar(128) | 否 | 加密密码（第三方登录可为空） |
| nickname | varchar(64) | 否 | 昵称 |
| avatar | varchar(500) | 否 | 头像 URL |
| real_name | varchar(32) | 否 | 真实姓名 |
| id_card | varchar(32) | 否 | 身份证号（加密存储） |
| email | varchar(128) | 否 | 邮箱 |
| phone | varchar(20) | 否 | 手机号 |
| gender | tinyint(1) | 否 | 0=未知, 1=男, 2=女 |
| birthday | date | 否 | 生日 |
| level | int(11) | 否 | 用户等级 |
| exp | bigint(20) | 否 | 经验值 |
| growth | bigint(20) | 否 | 成长值 |
| real_name_status | tinyint(1) | 否 | 实名状态：0=未认证, 1=已认证, 2=审核中, 3=认证失败 |
| is_blacklisted | tinyint(1) | 否 | 是否黑名单：0=否, 1=是 |
| source | varchar(20) | 是 | 注册来源：register / wechat / qq / weibo / github / dingtalk / wecom |
| last_login_ip | varchar(128) | 否 | 最后登录 IP |
| last_login_time | datetime | 否 | 最后登录时间 |
| register_ip | varchar(128) | 否 | 注册 IP |
| register_time | datetime | 是 | 注册时间 |
| verified | tinyint(1) | 否 | 实名认证：0=未认证, 1=已认证 |
| status | tinyint(1) | 是 | 0=禁用, 1=正常 |
| deleted | tinyint(1) | 是 | 逻辑删除 |
| remark | varchar(500) | 否 | 备注 |
| create_time | datetime | 是 | 创建时间 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`uk_username` (username), `uk_phone` (phone), `uk_email` (email)

---

### 4.2 sparkit_user_third_party（第三方登录绑定表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| bind_id | bigint(20) | 是 | 主键，自增 |
| user_id | bigint(20) | 是 | 用户 ID |
| platform | varchar(20) | 是 | 平台：wechat / wecom / qq / weibo / github / dingtalk |
| open_id | varchar(128) | 是 | 平台 OpenID |
| union_id | varchar(128) | 否 | 微信 UnionID |
| app_type | varchar(20) | 否 | 来源类型：mp / app / web / h5 |
| nickname | varchar(64) | 否 | 平台昵称 |
| avatar | varchar(500) | 否 | 平台头像 |
| access_token | varchar(500) | 否 | 访问令牌 |
| refresh_token | varchar(500) | 否 | 刷新令牌 |
| token_expire | datetime | 否 | 令牌过期时间 |
| bind_time | datetime | 是 | 绑定时间 |
| create_time | datetime | 是 | 创建时间 |

**联合唯一索引**：`uk_platform_openid` (platform, open_id)

---

### 4.3 sparkit_user_verify_code（验证码表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code_id | bigint(20) | 是 | 主键，自增 |
| target | varchar(128) | 是 | 手机号或邮箱地址 |
| target_type | varchar(10) | 是 | 目标类型：phone / email |
| code | varchar(10) | 是 | 验证码 |
| scene | varchar(32) | 是 | 场景：register / login / reset_password / bind / change / unbind |
| ip_address | varchar(128) | 否 | 请求 IP |
| status | tinyint(1) | 是 | 0=未使用, 1=已使用, 2=已过期 |
| expire_time | datetime | 是 | 过期时间 |
| create_time | datetime | 是 | 创建时间 |

**索引**：`idx_target_scene` (target, scene), `idx_expire_time` (expire_time)

---

### 4.4 sparkit_user_blacklist（黑名单表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| blacklist_id | bigint(20) | 是 | 主键，自增 |
| target_type | varchar(10) | 是 | 类型：user / ip |
| target_value | varchar(128) | 是 | 用户 ID 或 IP 地址 |
| reason | varchar(500) | 否 | 拉黑原因 |
| status | tinyint(1) | 是 | 0=已解除, 1=生效中 |
| create_by | varchar(64) | 否 | 操作人 |
| create_time | datetime | 是 | 操作时间 |
| unlock_time | datetime | 否 | 解封时间 |
| unlock_by | varchar(64) | 否 | 解封操作人 |

**索引**：`idx_target` (target_type, target_value)

---

## 五、存储模块表

### 5.1 sparkit_storage_config（存储源配置表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| storage_id | bigint(20) | 是 | 主键，自增 |
| storage_name | varchar(64) | 是 | 存储源名称 |
| storage_type | varchar(20) | 是 | 类型：local / ftp / oss / cos / qiniu / s3 |
| source_code | varchar(50) | 是 | 存储源编码（唯一标识） |
| access_key | varchar(255) | 否 | AccessKey |
| secret_key | varchar(255) | 否 | SecretKey（加密存储） |
| endpoint | varchar(255) | 否 | 终端节点 |
| bucket | varchar(128) | 否 | 桶 / 容器名称 |
| region | varchar(64) | 否 | 地域 |
| domain | varchar(255) | 否 | 自定义访问域名 |
| base_path | varchar(255) | 否 | 基础存储路径 |
| is_default | tinyint(1) | 否 | 是否默认存储源：0=否, 1=是 |
| status | tinyint(1) | 是 | 0=停用, 1=启用 |
| sort | int(11) | 否 | 排序号 |
| extra_config | text | 否 | 额外配置（JSON格式，存储各类型特有的配置项） |
| deleted | tinyint(1) | 是 | 逻辑删除：0=未删除, 1=已删除 |
| remark | varchar(500) | 否 | 备注 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`uk_source_code` (source_code)

---

### 5.2 sparkit_file（文件记录表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file_id | bigint(20) | 是 | 主键，自增 |
| storage_id | bigint(20) | 是 | 存储源 ID |
| file_name | varchar(255) | 是 | 原始文件名 |
| file_key | varchar(500) | 是 | 存储 Key / 路径 |
| file_url | varchar(500) | 是 | 访问 URL |
| file_size | bigint(20) | 是 | 文件大小（字节） |
| file_type | varchar(50) | 否 | 文件分类：image / video / audio / document / other |
| file_ext | varchar(20) | 否 | 扩展名 |
| mime_type | varchar(128) | 否 | MIME 类型 |
| md5 | varchar(64) | 否 | 文件 MD5（用于秒传和完整性校验） |
| sha256 | varchar(128) | 否 | 文件 SHA256 |
| upload_status | tinyint(1) | 是 | 上传状态：0=上传中, 1=完成, 2=失败 |
| chunk_count | int(11) | 否 | 分片总数 |
| chunk_size | bigint(20) | 否 | 分片大小（字节） |
| upload_id | varchar(128) | 否 | 分片上传任务 ID（云存储返回） |
| upload_by | varchar(64) | 否 | 上传人 |
| create_time | datetime | 是 | 创建时间 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`idx_md5` (md5), `idx_storage_id` (storage_id), `idx_file_key` (file_key)

---

### 5.3 sparkit_file_chunk（文件分片记录表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chunk_id | bigint(20) | 是 | 主键，自增 |
| file_id | bigint(20) | 是 | 文件 ID |
| chunk_index | int(11) | 是 | 分片序号（从 0 开始） |
| chunk_md5 | varchar(64) | 否 | 分片 MD5 |
| chunk_size | bigint(20) | 是 | 分片大小（字节） |
| chunk_key | varchar(500) | 否 | 分片存储 Key |
| chunk_status | tinyint(1) | 是 | 0=待上传, 1=已上传 |
| create_time | datetime | 是 | 创建时间 |

**索引**：`idx_file_id` (file_id), `uk_file_chunk` (file_id, chunk_index)

---

## 六、支付模块表

### 6.1 sparkit_payment_config（支付渠道配置表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| config_id | bigint(20) | 是 | 主键，自增 |
| channel | varchar(32) | 是 | 渠道编码：wechat_native / wechat_jsapi / wechat_app / wechat_mp / wechat_virtual / alipay_f2f / alipay_app / paypal / apple_pay / google_pay |
| channel_name | varchar(64) | 是 | 渠道名称 |
| app_id | varchar(128) | 否 | AppID |
| mch_id | varchar(128) | 否 | 商户号 |
| api_key | varchar(255) | 否 | API Key（加密存储） |
| private_key | text | 否 | 私钥（加密存储） |
| public_key | text | 否 | 公钥 |
| notify_url | varchar(500) | 否 | 回调通知地址 |
| return_url | varchar(500) | 否 | 支付完成跳转地址 |
| sandbox | tinyint(1) | 否 | 沙箱模式：0=生产, 1=沙箱 |
| status | tinyint(1) | 是 | 0=停用, 1=启用 |
| sort | int(11) | 否 | 排序号 |
| remark | varchar(500) | 否 | 备注 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`uk_channel` (channel)

---

### 6.2 sparkit_payment_order（支付订单表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| order_id | bigint(20) | 是 | 主键，自增 |
| order_no | varchar(64) | 是 | 业务订单号（唯一） |
| payment_no | varchar(64) | 是 | 支付流水号（唯一） |
| channel | varchar(32) | 是 | 支付渠道 |
| user_id | bigint(20) | 是 | 用户 ID |
| user_type | varchar(10) | 是 | 用户类型：admin / user |
| amount | decimal(12,2) | 是 | 订单金额（元） |
| currency | varchar(10) | 否 | 货币：CNY / USD |
| subject | varchar(255) | 是 | 商品标题 |
| body | varchar(500) | 否 | 商品描述 |
| trade_type | varchar(20) | 否 | 交易类型：NATIVE / JSAPI / APP / MP |
| open_id | varchar(128) | 否 | 用户 OpenID |
| status | tinyint(1) | 是 | 0=待支付, 1=支付成功, 2=支付失败, 3=已关闭, 4=已退款 |
| paid_amount | decimal(12,2) | 否 | 实付金额 |
| paid_time | datetime | 否 | 支付时间 |
| expire_time | datetime | 否 | 过期时间 |
| notify_data | text | 否 | 回调原始数据 |
| callback_url | varchar(500) | 否 | 业务回调地址 |
| remark | varchar(500) | 否 | 备注 |
| create_time | datetime | 是 | 创建时间 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`uk_order_no` (order_no), `uk_payment_no` (payment_no), `idx_user_id` (user_id), `idx_status` (status)

---

### 6.3 sparkit_payment_refund（退款表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refund_id | bigint(20) | 是 | 主键，自增 |
| order_id | bigint(20) | 是 | 关联订单 ID |
| refund_no | varchar(64) | 是 | 退款单号（唯一） |
| payment_no | varchar(64) | 是 | 原支付流水号 |
| refund_amount | decimal(12,2) | 是 | 退款金额（元） |
| refund_reason | varchar(500) | 否 | 退款原因 |
| status | tinyint(1) | 是 | 0=退款中, 1=退款成功, 2=退款失败 |
| refund_time | datetime | 否 | 退款完成时间 |
| notify_data | text | 否 | 回调原始数据 |
| create_by | varchar(64) | 否 | 操作人 |
| create_time | datetime | 是 | 创建时间 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`uk_refund_no` (refund_no), `idx_order_id` (order_id)

---

### 6.4 sparkit_payment_reconciliation（对账表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| recon_id | bigint(20) | 是 | 主键，自增 |
| channel | varchar(32) | 是 | 支付渠道 |
| recon_date | date | 是 | 对账日期 |
| total_count | int(11) | 否 | 总笔数 |
| success_count | int(11) | 否 | 一致笔数 |
| diff_count | int(11) | 否 | 差异笔数 |
| diff_amount | decimal(12,2) | 否 | 差异金额 |
| status | tinyint(1) | 是 | 0=对账中, 1=完成, 2=异常 |
| diff_detail | text | 否 | 差异明细 JSON |
| recon_time | datetime | 否 | 对账完成时间 |
| create_time | datetime | 是 | 创建时间 |

**索引**：`uk_channel_date` (channel, recon_date)

---

## 七、通知模块表

### 7.1 sparkit_notify_template（通知模板表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| template_id | bigint(20) | 是 | 主键，自增 |
| template_code | varchar(64) | 是 | 模板编码（唯一） |
| template_name | varchar(128) | 是 | 模板名称 |
| channel | varchar(20) | 是 | 渠道：sms / email / wechat / unipush / site |
| title | varchar(255) | 否 | 模板标题 |
| content | text | 是 | 模板内容（支持变量占位符 `${var}`） |
| variables | varchar(500) | 否 | 变量列表 JSON（如 `["code","expire"]`） |
| third_party_id | varchar(128) | 否 | 第三方平台模板 ID |
| status | tinyint(1) | 是 | 0=停用, 1=启用 |
| remark | varchar(500) | 否 | 备注 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`uk_template_code` (template_code)

---

### 7.2 sparkit_notify_record（通知发送记录表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| record_id | bigint(20) | 是 | 主键，自增 |
| template_id | bigint(20) | 否 | 模板 ID |
| channel | varchar(20) | 是 | 发送渠道 |
| sender | varchar(128) | 否 | 发送方标识 |
| receiver | varchar(128) | 是 | 接收方（手机号/邮箱/OpenID/设备Token） |
| title | varchar(255) | 否 | 发送标题 |
| content | text | 是 | 发送内容 |
| params | text | 否 | 实际参数 JSON |
| status | tinyint(1) | 是 | 0=发送中, 1=成功, 2=失败 |
| error_msg | varchar(500) | 否 | 失败原因 |
| send_time | datetime | 否 | 发送时间 |
| create_time | datetime | 是 | 创建时间 |

**索引**：`idx_receiver` (receiver), `idx_channel` (channel), `idx_send_time` (send_time)

---

### 7.3 sparkit_notify_message（站内信表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message_id | bigint(20) | 是 | 主键，自增 |
| user_id | bigint(20) | 是 | 接收用户 ID |
| user_type | varchar(10) | 是 | 用户类型：admin / user |
| title | varchar(255) | 是 | 消息标题 |
| content | text | 是 | 消息内容 |
| message_type | varchar(20) | 是 | 消息类型：system / notice / remind |
| is_read | tinyint(1) | 是 | 是否已读：0=未读, 1=已读 |
| read_time | datetime | 否 | 阅读时间 |
| sender_id | bigint(20) | 否 | 发送者 ID |
| sender_name | varchar(64) | 否 | 发送者名称 |
| create_time | datetime | 是 | 创建时间 |

**索引**：`idx_user_id_type` (user_id, user_type), `idx_is_read` (is_read)

---

## 八、AI 模块表

### 8.1 sparkit_ai_config（AI 模型配置表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| config_id | bigint(20) | 是 | 主键，自增 |
| config_name | varchar(64) | 是 | 配置名称 |
| provider | varchar(32) | 是 | 服务商：deepseek / xiaomi / aliyun_bailian / openai / azure / qwen / zhipu / moonshot / baidu / custom |
| provider_name | varchar(64) | 否 | 服务商显示名称 |
| model_name | varchar(64) | 是 | 模型名称，如：deepseek-chat / deepseek-reasoner / mixtral-8x7b |
| model_type | varchar(20) | 是 | 模型类型：text / image / video / audio / multimodal |
| api_key | varchar(500) | 是 | API Key（加密存储） |
| api_secret | varchar(500) | 否 | API Secret（加密存储） |
| api_base_url | varchar(500) | 是 | API 基础地址 |
| max_tokens | int(11) | 否 | 最大输出 Token 数 |
| temperature | decimal(3,2) | 否 | 温度参数（0.00 ~ 2.00） |
| top_p | decimal(3,2) | 否 | Top-P 采样参数 |
| context_length | int(11) | 否 | 上下文窗口大小 |
| support_function_call | tinyint(1) | 否 | 是否支持 Function Call：0=否, 1=是 |
| support_vision | tinyint(1) | 否 | 是否支持视觉：0=否, 1=是 |
| priority | int(11) | 否 | 优先级（数字越小越优先） |
| status | tinyint(1) | 是 | 0=停用, 1=启用 |
| remark | varchar(500) | 否 | 备注 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`idx_provider` (provider), `idx_model_type` (model_type), `idx_status` (status)

---

### 8.2 sparkit_ai_session（AI 会话表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| session_id | bigint(20) | 是 | 主键，自增 |
| session_uuid | varchar(64) | 是 | 会话唯一标识（对外暴露） |
| user_id | bigint(20) | 是 | 用户 ID |
| user_type | varchar(10) | 是 | 用户类型：admin / user |
| config_id | bigint(20) | 否 | 使用的 AI 配置 ID |
| title | varchar(255) | 否 | 会话标题（自动生成或用户设置） |
| session_type | varchar(20) | 是 | 会话类型：chat / image_gen / video_gen / audio / code |
| system_prompt | text | 否 | 系统提示词 |
| model_name | varchar(64) | 否 | 实际使用的模型名称 |
| total_tokens | bigint(20) | 否 | 累计消耗 Token 数 |
| message_count | int(11) | 否 | 消息总数 |
| status | tinyint(1) | 是 | 0=已归档, 1=活跃中 |
| pinned | tinyint(1) | 否 | 是否置顶：0=否, 1=是 |
| create_time | datetime | 是 | 创建时间 |
| update_time | datetime | 否 | 最后活跃时间 |

**索引**：`uk_session_uuid` (session_uuid), `idx_user_id_type` (user_id, user_type), `idx_status` (status)

---

### 8.3 sparkit_ai_message（AI 消息表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message_id | bigint(20) | 是 | 主键，自增 |
| session_id | bigint(20) | 是 | 所属会话 ID |
| role | varchar(20) | 是 | 角色：system / user / assistant / tool |
| content | longtext | 是 | 消息内容 |
| content_type | varchar(20) | 否 | 内容类型：text / image_url / file / audio |
| reasoning_content | longtext | 否 | 推理过程内容（如 deepseek-reasoner 的思考过程） |
| token_count | int(11) | 否 | 本条消息消耗 Token 数 |
| model_name | varchar(64) | 否 | 生成时使用的模型名称 |
| finish_reason | varchar(20) | 否 | 结束原因：stop / length / content_filter / tool_calls |
| metadata | text | 否 | 附加元数据 JSON |
| parent_id | bigint(20) | 否 | 父消息 ID（用于分支对话） |
| is_deleted | tinyint(1) | 否 | 逻辑删除：0=正常, 1=已删除 |
| create_time | datetime | 是 | 创建时间 |

**索引**：`idx_session_id` (session_id), `idx_role` (role), `idx_create_time` (create_time)

---

### 8.4 sparkit_ai_image_record（AI 图片生成记录表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| record_id | bigint(20) | 是 | 主键，自增 |
| session_id | bigint(20) | 否 | 关联会话 ID |
| user_id | bigint(20) | 是 | 用户 ID |
| user_type | varchar(10) | 是 | 用户类型：admin / user |
| config_id | bigint(20) | 否 | AI 配置 ID |
| model_name | varchar(64) | 否 | 使用的模型名称 |
| prompt | text | 是 | 生图提示词 |
| negative_prompt | text | 否 | 反向提示词 |
| image_url | varchar(500) | 否 | 生成图片 URL |
| image_size | varchar(20) | 否 | 图片尺寸，如：1024x1024 |
| quality | varchar(20) | 否 | 质量：standard / hd |
| style | varchar(20) | 否 | 风格 |
| seed | bigint(20) | 否 | 随机种子 |
| cost | decimal(10,4) | 否 | 消耗费用（元） |
| status | tinyint(1) | 是 | 0=生成中, 1=成功, 2=失败 |
| error_msg | varchar(500) | 否 | 失败原因 |
| create_time | datetime | 是 | 创建时间 |

**索引**：`idx_user_id_type` (user_id, user_type), `idx_status` (status), `idx_create_time` (create_time)

---

### 8.5 sparkit_ai_video_record（AI 视频生成记录表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| record_id | bigint(20) | 是 | 主键，自增 |
| session_id | bigint(20) | 否 | 关联会话 ID |
| user_id | bigint(20) | 是 | 用户 ID |
| user_type | varchar(10) | 是 | 用户类型：admin / user |
| config_id | bigint(20) | 否 | AI 配置 ID |
| model_name | varchar(64) | 否 | 使用的模型名称 |
| prompt | text | 是 | 生成视频提示词 |
| video_url | varchar(500) | 否 | 生成视频 URL |
| duration | int(11) | 否 | 视频时长（秒） |
| resolution | varchar(20) | 否 | 分辨率：1080p / 720p / 4k |
| frame_rate | int(11) | 否 | 帧率 |
| cost | decimal(10,4) | 否 | 消耗费用（元） |
| status | tinyint(1) | 是 | 0=生成中, 1=成功, 2=失败 |
| error_msg | varchar(500) | 否 | 失败原因 |
| create_time | datetime | 是 | 创建时间 |

**索引**：`idx_user_id_type` (user_id, user_type), `idx_status` (status), `idx_create_time` (create_time)

---

### 8.6 sparkit_ai_api_usage（AI API 用量统计表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| usage_id | bigint(20) | 是 | 主键，自增 |
| config_id | bigint(20) | 是 | AI 配置 ID |
| user_id | bigint(20) | 否 | 用户 ID（NULL 表示系统统计） |
| user_type | varchar(10) | 否 | 用户类型 |
| model_name | varchar(64) | 是 | 模型名称 |
| request_count | int(11) | 是 | 请求次数 |
| prompt_tokens | bigint(20) | 否 | 输入 Token 数 |
| completion_tokens | bigint(20) | 否 | 输出 Token 数 |
| total_tokens | bigint(20) | 否 | 总 Token 数 |
| cost | decimal(10,4) | 否 | 预估费用（元） |
| stat_date | date | 是 | 统计日期 |
| create_time | datetime | 是 | 创建时间 |

**索引**：`idx_config_date` (config_id, stat_date), `idx_user_date` (user_id, stat_date)

---

### 8.7 sparkit_ai_knowledge_base（知识库表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| kb_id | bigint(20) | 是 | 主键，自增 |
| kb_name | varchar(128) | 是 | 知识库名称 |
| kb_desc | varchar(500) | 否 | 知识库描述 |
| embedding_model | varchar(64) | 否 | 嵌入模型名称 |
| chunk_size | int(11) | 否 | 分块大小（默认 500） |
| chunk_overlap | int(11) | 否 | 分块重叠大小（默认 50） |
| document_count | int(11) | 否 | 文档数量 |
| status | tinyint(1) | 是 | 0=停用, 1=启用 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`idx_status` (status)

---

### 8.8 sparkit_ai_kb_document（知识库文档表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| doc_id | bigint(20) | 是 | 主键，自增 |
| kb_id | bigint(20) | 是 | 所属知识库 ID |
| doc_name | varchar(255) | 是 | 文档名称 |
| doc_type | varchar(20) | 是 | 文档类型：txt / pdf / md / html / url / json |
| source_url | varchar(500) | 否 | 来源 URL |
| file_id | bigint(20) | 否 | 关联文件 ID |
| chunk_count | int(11) | 否 | 分块数量 |
| char_count | bigint(20) | 否 | 字符总数 |
| status | tinyint(1) | 是 | 0=处理中, 1=已完成, 2=失败 |
| error_msg | varchar(500) | 否 | 失败原因 |
| create_time | datetime | 是 | 创建时间 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`idx_kb_id` (kb_id), `idx_status` (status)

---

## 九、新闻模块表

### 9.1 sparkit_news_category（新闻分类表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category_id | bigint(20) | 是 | 主键，自增 |
| parent_id | bigint(20) | 否 | 父分类 ID（0 表示顶级） |
| category_name | varchar(64) | 是 | 分类名称 |
| category_code | varchar(64) | 是 | 分类编码（唯一） |
| icon | varchar(500) | 否 | 分类图标 |
| description | varchar(255) | 否 | 分类描述 |
| sort | int(11) | 否 | 排序号 |
| status | tinyint(1) | 是 | 0=停用, 1=启用 |
| is_deleted | tinyint(1) | 否 | 逻辑删除：0=正常, 1=已删除 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`uk_category_code` (category_code), `idx_parent_id` (parent_id), `idx_sort` (sort)

---

### 9.2 sparkit_news_article（新闻文章表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| article_id | bigint(20) | 是 | 主键，自增 |
| category_id | bigint(20) | 否 | 所属分类 ID |
| title | varchar(255) | 是 | 文章标题 |
| sub_title | varchar(255) | 否 | 副标题 |
| summary | varchar(500) | 否 | 文章摘要 |
| content | longtext | 是 | 文章内容（富文本） |
| content_md | longtext | 否 | Markdown 格式内容 |
| cover_image | varchar(500) | 否 | 封面图 |
| images | text | 否 | 文章配图 JSON 数组 |
| tags | varchar(500) | 否 | 标签（逗号分隔） |
| keywords | varchar(500) | 否 | SEO 关键词 |
| source | varchar(128) | 否 | 来源 |
| source_url | varchar(500) | 否 | 来源链接 |
| author | varchar(64) | 否 | 作者 |
| is_ai_generated | tinyint(1) | 否 | 是否 AI 生成：0=否, 1=是 |
| ai_model | varchar(64) | 否 | AI 生成使用的模型 |
| ai_prompt | text | 否 | AI 生成使用的提示词 |
| view_count | int(11) | 否 | 浏览次数 |
| like_count | int(11) | 否 | 点赞数 |
| comment_count | int(11) | 否 | 评论数 |
| share_count | int(11) | 否 | 分享数 |
| status | tinyint(1) | 是 | 0=草稿, 1=已发布, 2=已下架 |
| is_top | tinyint(1) | 否 | 是否置顶：0=否, 1=是 |
| is_recommend | tinyint(1) | 否 | 是否推荐：0=否, 1=是 |
| is_original | tinyint(1) | 否 | 是否原创：0=转载, 1=原创 |
| is_deleted | tinyint(1) | 否 | 逻辑删除：0=正常, 1=已删除 |
| publish_time | datetime | 否 | 发布时间 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`idx_category_id` (category_id), `idx_status` (status), `idx_publish_time` (publish_time), `idx_is_top` (is_top), `idx_is_recommend` (is_recommend)

---

### 9.3 sparkit_news_article_tag（文章标签关联表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | bigint(20) | 是 | 主键，自增 |
| article_id | bigint(20) | 是 | 文章 ID |
| tag_id | bigint(20) | 是 | 标签 ID |

**索引**：`uk_article_tag` (article_id, tag_id), `idx_tag_id` (tag_id)

---

### 9.4 sparkit_news_tag（标签表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| tag_id | bigint(20) | 是 | 主键，自增 |
| tag_name | varchar(64) | 是 | 标签名称 |
| tag_code | varchar(64) | 是 | 标签编码（唯一） |
| usage_count | int(11) | 否 | 引用次数 |
| status | tinyint(1) | 是 | 0=停用, 1=启用 |
| create_time | datetime | 是 | 创建时间 |

**索引**：`uk_tag_code` (tag_code)

---

### 9.5 sparkit_news_comment（文章评论表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| comment_id | bigint(20) | 是 | 主键，自增 |
| article_id | bigint(20) | 是 | 文章 ID |
| parent_id | bigint(20) | 否 | 父评论 ID（0 表示一级评论） |
| user_id | bigint(20) | 是 | 评论用户 ID |
| user_type | varchar(10) | 是 | 用户类型：admin / user |
| content | text | 是 | 评论内容 |
| like_count | int(11) | 否 | 点赞数 |
| status | tinyint(1) | 是 | 0=待审核, 1=已通过, 2=已拒绝, 3=已删除 |
| ip_address | varchar(128) | 否 | 评论 IP |
| user_agent | varchar(500) | 否 | 用户代理 |
| create_time | datetime | 是 | 创建时间 |
| audit_time | datetime | 否 | 审核时间 |
| audit_by | varchar(64) | 否 | 审核人 |

**索引**：`idx_article_id` (article_id), `idx_user_id` (user_id), `idx_parent_id` (parent_id), `idx_status` (status)

---

### 9.6 sparkit_news_ai_task（AI 新闻采集/生成任务表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| task_id | bigint(20) | 是 | 主键，自增 |
| task_name | varchar(128) | 是 | 任务名称 |
| task_type | varchar(20) | 是 | 任务类型：collect（采集）/ generate（生成）/ rewrite（改写）/ translate（翻译） |
| category_id | bigint(20) | 否 | 目标分类 ID |
| ai_config_id | bigint(20) | 否 | AI 配置 ID |
| source_urls | text | 否 | 采集源 URL（JSON 数组） |
| prompt_template | text | 否 | 提示词模板 |
| keywords | varchar(500) | 否 | 关键词（逗号分隔） |
| generate_count | int(11) | 否 | 每次生成数量 |
| cron_expression | varchar(64) | 否 | Cron 表达式（定时执行） |
| last_run_time | datetime | 否 | 上次执行时间 |
| next_run_time | datetime | 否 | 下次执行时间 |
| total_generated | int(11) | 否 | 累计生成数量 |
| status | tinyint(1) | 是 | 0=停用, 1=启用, 2=运行中 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`idx_task_type` (task_type), `idx_status` (status), `idx_next_run_time` (next_run_time)

---

### 9.7 sparkit_news_ai_task_log（AI 新闻任务执行日志表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| log_id | bigint(20) | 是 | 主键，自增 |
| task_id | bigint(20) | 是 | 任务 ID |
| article_id | bigint(20) | 否 | 生成的文章 ID |
| execute_status | tinyint(1) | 是 | 0=执行中, 1=成功, 2=失败 |
| prompt_used | text | 否 | 实际使用的 Prompt |
| response_data | longtext | 否 | AI 返回原始数据 |
| error_msg | varchar(500) | 否 | 失败原因 |
| cost | decimal(10,4) | 否 | 消耗费用（元） |
| execute_time | datetime | 是 | 执行时间 |
| create_time | datetime | 是 | 创建时间 |

**索引**：`idx_task_id` (task_id), `idx_article_id` (article_id), `idx_execute_status` (execute_status)

---

## 十、定时任务表

### 10.1 sparkit_job（定时任务定义表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| job_id | bigint(20) | 是 | 主键，自增 |
| job_name | varchar(128) | 是 | 任务名称 |
| job_group | varchar(128) | 是 | 任务组 |
| job_class | varchar(255) | 是 | 执行类全限定名（如 com.sparkit.job.NewsCollectJob） |
| cron_expression | varchar(64) | 是 | Cron 表达式 |
| job_params | text | 否 | 任务参数 JSON |
| description | varchar(500) | 否 | 任务描述 |
| concurrent | tinyint(1) | 否 | 是否允许并发：0=禁止, 1=允许 |
| misfire_policy | tinyint(1) | 否 | 错过策略：0=立即执行, 1=放弃, 2=合并到下次 |
| retry_count | int(11) | 否 | 失败重试次数 |
| retry_interval | int(11) | 否 | 重试间隔（秒） |
| timeout | int(11) | 否 | 超时时间（秒，0 表示不限制） |
| status | tinyint(1) | 是 | 0=暂停, 1=运行中 |
| last_execute_time | datetime | 否 | 上次执行时间 |
| next_execute_time | datetime | 否 | 下次执行时间 |
| create_by | varchar(64) | 否 | 创建人 |
| create_time | datetime | 是 | 创建时间 |
| update_by | varchar(64) | 否 | 更新人 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`uk_job_name_group` (job_name, job_group), `idx_status` (status)

---

### 10.2 sparkit_job_log（定时任务执行日志表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| log_id | bigint(20) | 是 | 主键，自增 |
| job_id | bigint(20) | 是 | 任务 ID |
| job_name | varchar(128) | 是 | 任务名称 |
| job_group | varchar(128) | 是 | 任务组 |
| execute_ip | varchar(64) | 否 | 执行服务器 IP |
| execute_status | tinyint(1) | 是 | 0=执行中, 1=成功, 2=失败 |
| execute_params | text | 否 | 实际执行参数 |
| result_message | text | 否 | 执行结果信息 |
| error_stack | text | 否 | 异常堆栈 |
| start_time | datetime | 否 | 开始时间 |
| end_time | datetime | 否 | 结束时间 |
| duration | bigint(20) | 否 | 执行耗时（毫秒） |
| create_time | datetime | 是 | 创建时间 |

**索引**：`idx_job_id` (job_id), `idx_execute_status` (execute_status), `idx_start_time` (start_time)

---

## 十一、日志与审计表

### 11.1 sparkit_oper_log（操作日志表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| log_id | bigint(20) | 是 | 主键，自增 |
| module | varchar(64) | 否 | 操作模块 |
| business_type | varchar(32) | 否 | 业务类型：insert / update / delete / export / import / grant / other |
| method | varchar(255) | 否 | 请求方法全路径 |
| request_method | varchar(10) | 否 | HTTP 方法：GET / POST / PUT / DELETE |
| operator_type | varchar(10) | 否 | 操作人类型：admin / user |
| operator_id | bigint(20) | 否 | 操作人 ID |
| operator_name | varchar(64) | 否 | 操作人名称 |
| operator_ip | varchar(128) | 否 | 操作 IP |
| operator_location | varchar(255) | 否 | 操作地点 |
| request_url | varchar(500) | 否 | 请求 URL |
| request_params | text | 否 | 请求参数 |
| response_result | text | 否 | 响应结果（截取前 2000 字符） |
| cost_time | bigint(20) | 否 | 执行耗时（毫秒） |
| status | tinyint(1) | 否 | 0=失败, 1=成功 |
| error_msg | text | 否 | 错误信息 |
| user_agent | varchar(500) | 否 | 用户代理 |
| create_time | datetime | 是 | 创建时间 |

**索引**：`idx_operator_id` (operator_id), `idx_module` (module), `idx_business_type` (business_type), `idx_create_time` (create_time)

---

### 11.2 sparkit_login_log（登录日志表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| log_id | bigint(20) | 是 | 主键，自增 |
| login_type | varchar(10) | 是 | 登录类型：admin / user |
| user_id | bigint(20) | 是 | 用户 ID |
| username | varchar(64) | 是 | 登录用户名 |
| login_method | varchar(20) | 否 | 登录方式：password / sms / email / wechat / wecom / qq / weibo / github / dingtalk |
| ip_address | varchar(128) | 否 | 登录 IP |
| location | varchar(255) | 否 | 登录地点 |
| browser | varchar(128) | 否 | 浏览器类型 |
| os | varchar(128) | 否 | 操作系统 |
| device | varchar(20) | 否 | 设备类型：pc / mobile / tablet |
| status | tinyint(1) | 是 | 0=失败, 1=成功 |
| fail_reason | varchar(255) | 否 | 失败原因 |
| login_time | datetime | 是 | 登录时间 |

**索引**：`idx_user_id` (user_id), `idx_login_time` (login_time), `idx_status` (status)

---

### 11.3 sparkit_api_access_log（API 访问日志表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| log_id | bigint(20) | 是 | 主键，自增 |
| trace_id | varchar(64) | 否 | 链路追踪 ID |
| app_key | varchar(64) | 否 | 应用标识（对外开放 API 使用） |
| user_id | bigint(20) | 否 | 访问用户 ID |
| user_type | varchar(10) | 否 | 用户类型 |
| request_ip | varchar(128) | 否 | 请求 IP |
| request_url | varchar(500) | 否 | 请求 URL |
| http_method | varchar(10) | 否 | HTTP 方法 |
| request_params | text | 否 | 请求参数 |
| request_body | text | 否 | 请求体 |
| response_code | int(11) | 否 | 响应状态码 |
| response_body | text | 否 | 响应体（截取前 1000 字符） |
| cost_time | int(11) | 否 | 耗时（毫秒） |
| user_agent | varchar(500) | 否 | 用户代理 |
| create_time | datetime | 是 | 创建时间 |

**索引**：`idx_trace_id` (trace_id), `idx_user_id` (user_id), `idx_create_time` (create_time)

---

### 11.4 sparkit_sensitive_log（敏感操作审计日志表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| log_id | bigint(20) | 是 | 主键，自增 |
| action | varchar(64) | 是 | 操作动作：reset_password / bind_phone / bind_email / delete_user / grant_permission / export_data / change_config |
| target_type | varchar(32) | 是 | 目标类型：user / role / config / data |
| target_id | varchar(128) | 否 | 目标 ID |
| target_name | varchar(255) | 否 | 目标名称 |
| detail | text | 否 | 操作详情 JSON |
| operator_id | bigint(20) | 是 | 操作人 ID |
| operator_type | varchar(10) | 是 | 操作人类型：admin / system |
| operator_name | varchar(64) | 否 | 操作人名称 |
| operator_ip | varchar(128) | 否 | 操作 IP |
| create_time | datetime | 是 | 创建时间 |

**索引**：`idx_action` (action), `idx_operator_id` (operator_id), `idx_create_time` (create_time)

---

## 十二、多租户与系统配置扩展表

### 12.1 sparkit_tenant（租户表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| tenant_id | bigint(20) | 是 | 主键，自增 |
| tenant_code | varchar(64) | 是 | 租户编码（唯一） |
| tenant_name | varchar(128) | 是 | 租户名称 |
| logo | varchar(500) | 否 | 租户 Logo |
| contact_name | varchar(64) | 否 | 联系人 |
| contact_phone | varchar(20) | 否 | 联系电话 |
| contact_email | varchar(128) | 否 | 联系邮箱 |
| db_type | varchar(20) | 否 | 数据库隔离方式：shared（共享）/ independent（独立） |
| db_url | varchar(500) | 否 | 独立数据库连接地址 |
| db_username | varchar(128) | 否 | 数据库用户名（加密） |
| db_password | varchar(255) | 否 | 数据库密码（加密） |
| domain | varchar(255) | 否 | 专属域名 |
| expire_time | datetime | 否 | 到期时间 |
| status | tinyint(1) | 是 | 0=停用, 1=启用, 2=已过期 |
| create_time | datetime | 是 | 创建时间 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`uk_tenant_code` (tenant_code), `idx_domain` (domain), `idx_status` (status)

---

### 12.2 sparkit_tenant_config（租户个性化配置表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| config_id | bigint(20) | 是 | 主键，自增 |
| tenant_id | bigint(20) | 是 | 租户 ID |
| config_key | varchar(128) | 是 | 配置键 |
| config_value | text | 否 | 配置值 |
| config_type | varchar(20) | 否 | 配置类型：string / number / boolean / json / image |
| description | varchar(255) | 否 | 配置说明 |
| create_time | datetime | 是 | 创建时间 |
| update_time | datetime | 否 | 更新时间 |

**索引**：`uk_tenant_key` (tenant_id, config_key)

---

### 12.3 sparkit_sys_version（系统版本管理表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| version_id | bigint(20) | 是 | 主键，自增 |
| platform | varchar(20) | 是 | 平台：admin_web / web / android / ios / mp_wechat / mp_alipay |
| version_code | int(11) | 是 | 版本号（数值，用于比较） |
| version_name | varchar(32) | 是 | 版本名称（如 1.0.0） |
| title | varchar(255) | 否 | 更新标题 |
| content | text | 否 | 更新内容 |
| download_url | varchar(500) | 否 | 下载地址 |
| app_store_url | varchar(500) | 否 | 应用商店地址 |
| min_version_code | int(11) | 否 | 最低兼容版本号（低于此版本强制更新） |
| force_update | tinyint(1) | 否 | 是否强制更新：0=否, 1=是 |
| status | tinyint(1) | 是 | 0=未发布, 1=已发布 |
| publish_time | datetime | 否 | 发布时间 |
| create_time | datetime | 是 | 创建时间 |

**索引**：`uk_platform_code` (platform, version_code), `idx_status` (status)

---

### 12.4 sparkit_sys_feedback（用户反馈表）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| feedback_id | bigint(20) | 是 | 主键，自增 |
| user_id | bigint(20) | 是 | 用户 ID |
| user_type | varchar(10) | 是 | 用户类型：admin / user |
| category | varchar(32) | 否 | 反馈分类：bug / suggestion / question / complaint / other |
| title | varchar(255) | 否 | 反馈标题 |
| content | text | 是 | 反馈内容 |
| images | text | 否 | 截图 JSON 数组 |
| contact | varchar(128) | 否 | 联系方式 |
| platform | varchar(20) | 否 | 来源平台：web / android / ios / mp |
| app_version | varchar(32) | 否 | App 版本号 |
| device_info | varchar(255) | 否 | 设备信息 |
| status | tinyint(1) | 是 | 0=未处理, 1=处理中, 2=已完成, 3=已关闭 |
| remark | varchar(500) | 否 | 处理备注 |
| handle_by | varchar(64) | 否 | 处理人 |
| handle_time | datetime | 否 | 处理时间 |
| create_time | datetime | 是 | 创建时间 |

**索引**：`idx_user_id` (user_id), `idx_category` (category), `idx_status` (status)

---

## 附录：表索引汇总

| 模块 | 表数量 | 表名列表 |
|------|--------|----------|
| 系统管理 | 13 | sparkit_admin_user, sparkit_role, sparkit_admin_user_role, sparkit_menu, sparkit_role_menu, sparkit_dept, sparkit_post, sparkit_admin_user_post, sparkit_config, sparkit_dict_type, sparkit_dict_data, sparkit_region, sparkit_i18n |
| 用户模块 | 4 | sparkit_user, sparkit_user_third_party, sparkit_user_verify_code, sparkit_user_blacklist |
| 存储模块 | 3 | sparkit_storage_config, sparkit_file, sparkit_file_chunk |
| 支付模块 | 4 | sparkit_payment_config, sparkit_payment_order, sparkit_payment_refund, sparkit_payment_reconciliation |
| 通知模块 | 3 | sparkit_notify_template, sparkit_notify_record, sparkit_notify_message |
| AI 模块 | 8 | sparkit_ai_config, sparkit_ai_session, sparkit_ai_message, sparkit_ai_image_record, sparkit_ai_video_record, sparkit_ai_api_usage, sparkit_ai_knowledge_base, sparkit_ai_kb_document |
| 新闻模块 | 7 | sparkit_news_category, sparkit_news_article, sparkit_news_article_tag, sparkit_news_tag, sparkit_news_comment, sparkit_news_ai_task, sparkit_news_ai_task_log |
| 定时任务 | 2 | sparkit_job, sparkit_job_log |
| 日志审计 | 4 | sparkit_oper_log, sparkit_login_log, sparkit_api_access_log, sparkit_sensitive_log |
| 多租户/扩展 | 4 | sparkit_tenant, sparkit_tenant_config, sparkit_sys_version, sparkit_sys_feedback |

> **总计：52 张表**