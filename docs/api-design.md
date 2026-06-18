# Sparkit API 接口设计文档

> 版本：v1.0 | 基础路径：`/api/v1` | 数据格式：JSON | 字符编码：UTF-8

---

## 一、通用设计规范

### 1.1 设计原则

| 原则 | 说明 |
|------|------|
| RESTful 风格 | 资源 URL 采用名词复数，HTTP 方法语义化（GET/POST/PUT/DELETE） |
| 统一响应体 | 所有接口返回 `{ code, msg, data, timestamp }` 结构 |
| 版本控制 | URL 路径版本 `/api/v1`，便于平滑升级 |
| 无状态认证 | JWT Token（AccessToken + RefreshToken），不依赖 Session |
| 幂等设计 | 支付、退款等敏感操作使用幂等键（idempotent_key）防重复提交 |
| 批量操作 | 配置类接口统一使用 JSON 批量提交，避免循环调用；后端批量处理，单事务提交 |
| 统计接口 | 所有 count/statistics 接口使用 SQL 聚合函数（COUNT/SUM/AVG），禁止拉全量列表后内存统计 |
| 分页查询 | 列表查询强制分页，默认 `page=1&pageSize=10`，上限 `pageSize=100` |

### 1.2 高性能设计原则

| 原则 | 说明 |
|------|------|
| 配置批量更新 | `sparkit_config` 等配置表，前端提交完整 JSON 对象，后端解析后批量写入，**禁止逐条调用接口** |
| 统计走 SQL | 任何 count/total/summary 类接口，必须在 XML Mapper 中写聚合 SQL，禁止拉取全量列表后 count |
| 关联查询 | 多表关联优先用 JOIN，避免 N+1 查询；列表查询中禁止循环查子表 |
| 分页必须 | 所有列表查询必须分页，后端强制 `pageSize` 上限 100 |
| 字段裁剪 | 列表接口只返回必要字段，详情接口返回全部字段 |
| 缓存策略 | 字典、配置、地区等低频变更数据使用 Redis 缓存 + 版本号淘汰 |

### 1.3 安全设计原则

| 原则 | 说明 |
|------|------|
| JWT 双令牌 | AccessToken 短期有效（15min），RefreshToken 长期有效（7d），AccessToken 过期后用 RefreshToken 刷新 |
| 接口鉴权 | 细粒度 RBAC：每个 API 接口对应 `sparkit_menu` 一条记录（menu_type=B），通过角色-菜单绑定鉴权 |
| 数据脱敏 | 手机号、邮箱、身份证在列表接口中返回脱敏值（如 `138****1234`） |
| 敏感字段加密 | 身份证、API Key、Secret Key 等敏感数据数据库加密存储（AES-256） |
| 请求签名 | 关键操作（支付/退款/提现）增加请求签名验证，防篡改 |
| 防重放 | 关键接口增加 timestamp + nonce 校验，防重放攻击 |
| XSS 防护 | 所有输入参数经 XSS 过滤器转义 |
| SQL 注入防护 | 全部使用 MyBatis `#{}` 参数绑定，禁止 `${}` 拼接 |
| CORS 白名单 | 跨域仅允许配置的域名列表 |
| 限流 | 登录/验证码发送等接口增加 IP 级别限流（令牌桶算法） |
| 敏感操作日志 | 密码修改、权限变更、配置修改等操作记录到 `sparkit_sensitive_log` |

---

### 1.4 统一响应格式

```json
// 成功
{
  "code": 200,
  "msg": "操作成功",
  "data": { ... },
  "timestamp": 1718700000000
}

// 分页成功
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "records": [ ... ],
    "total": 100,
    "page": 1,
    "pageSize": 10
  },
  "timestamp": 1718700000000
}

// 失败
{
  "code": 500,
  "msg": "用户名已存在",
  "data": null,
  "timestamp": 1718700000000
}
```

### 1.5 错误码规范

| 区间 | 说明 |
|------|------|
| 200 | 操作成功 |
| 400 | 参数校验失败 |
| 401 | 未登录或 Token 过期 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 409 | 数据冲突（如唯一键重复） |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |
| 自定义 | 业务错误码 `10001+` 如 `10001=验证码错误`, `10002=账号已锁定` |

### 1.6 鉴权方式

```
Header:
  Authorization: Bearer <access_token>
  X-Refresh-Token: <refresh_token>    # 刷新时传递
  X-Request-Id: <uuid>                # 链路追踪ID
```

### 1.7 通用查询参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 10，上限 100 |
| sortField | string | 否 | 排序字段 |
| sortOrder | string | 否 | 排序方向：asc/desc |
| keyword | string | 否 | 模糊搜索关键词 |
| startTime | string | 否 | 开始时间 (yyyy-MM-dd HH:mm:ss) |
| endTime | string | 否 | 结束时间 (yyyy-MM-dd HH:mm:ss) |

---

## 二、系统管理模块 API

> 基础路径：`/api/v1/admin`

### 2.1 管理员管理

#### 2.1.1 管理员登录

```
POST /api/v1/public/admin/login
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码（前端 RSA 加密传输） |
| captchaKey | string | 是 | 验证码 Key |
| captchaCode | string | 是 | 验证码 |

**响应**：
```json
{
  "code": 200,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 900
  }
}
```

**安全设计**：
- 登录失败 5 次锁定账号 30 分钟
- 记录登录日志（IP、设备、状态）
- 验证码 5 分钟有效期，单次使用

#### 2.1.2 刷新 Token

```
POST /api/v1/public/admin/refresh-token
Header: X-Refresh-Token: <refresh_token>
```

#### 2.1.3 管理员退出

```
POST /api/v1/admin/logout
```

#### 2.1.4 获取当前管理员信息

```
GET /api/v1/admin/info
```

**响应**：返回管理员信息 + 角色列表 + 权限标识列表（perms）

#### 2.1.5 管理员列表（分页）

```
GET /api/v1/admin/users?page=1&pageSize=10&keyword=&status=&deptId=
```

**性能说明**：分页查询使用 MyBatis PageHelper，count 查询走 SQL 聚合。

#### 2.1.6 管理员详情

```
GET /api/v1/admin/users/{adminId}
```

#### 2.1.7 新增管理员

```
POST /api/v1/admin/users
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |
| nickname | string | 否 | 昵称 |
| email | string | 否 | 邮箱 |
| phone | string | 否 | 手机号 |
| deptId | long | 否 | 部门ID |
| roleIds | long[] | 是 | 角色ID列表 |
| postIds | long[] | 否 | 岗位ID列表 |
| status | int | 否 | 状态 |

#### 2.1.8 编辑管理员

```
PUT /api/v1/admin/users/{adminId}
```

#### 2.1.9 删除管理员（逻辑删除）

```
DELETE /api/v1/admin/users/{adminId}
```

#### 2.1.10 重置密码

```
PUT /api/v1/admin/users/{adminId}/reset-password
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| newPassword | string | 是 | 新密码 |

**安全设计**：记录到 `sparkit_sensitive_log`，事件类型 `password_change`。

#### 2.1.11 修改个人信息

```
PUT /api/v1/admin/profile
```

#### 2.1.12 修改个人密码

```
PUT /api/v1/admin/profile/password
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| oldPassword | string | 是 | 旧密码 |
| newPassword | string | 是 | 新密码 |

---

### 2.2 角色管理

#### 2.2.1 角色列表（分页）

```
GET /api/v1/admin/roles?page=1&pageSize=10&keyword=&status=
```

#### 2.2.2 角色详情

```
GET /api/v1/admin/roles/{roleId}
```

#### 2.2.3 新增角色

```
POST /api/v1/admin/roles
```

#### 2.2.4 编辑角色

```
PUT /api/v1/admin/roles/{roleId}
```

#### 2.2.5 删除角色

```
DELETE /api/v1/admin/roles/{roleId}
```

**安全设计**：内置角色（`built_in=1`）禁止删除。

#### 2.2.6 分配权限

```
PUT /api/v1/admin/roles/{roleId}/permissions
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| menuIds | long[] | 是 | 菜单/权限ID列表 |

**安全设计**：变更前后数据记录到 `sparkit_sensitive_log`。

#### 2.2.7 获取角色权限树

```
GET /api/v1/admin/roles/{roleId}/permissions
```

**响应**：返回所有菜单树 + 该角色已选中的 menuId 列表。

---

### 2.3 菜单管理

#### 2.3.1 菜单树

```
GET /api/v1/admin/menus/tree
```

**性能说明**：一次性查询所有菜单，后端构建树结构返回。

#### 2.3.2 菜单详情

```
GET /api/v1/admin/menus/{menuId}
```

#### 2.3.3 新增菜单

```
POST /api/v1/admin/menus
```

#### 2.3.4 编辑菜单

```
PUT /api/v1/admin/menus/{menuId}
```

#### 2.3.5 删除菜单

```
DELETE /api/v1/admin/menus/{menuId}
```

**安全设计**：有子菜单的禁止删除，已有角色绑定的禁止删除。

---

### 2.4 部门管理

#### 2.4.1 部门树

```
GET /api/v1/admin/depts/tree
```

#### 2.4.2 部门详情

```
GET /api/v1/admin/depts/{deptId}
```

#### 2.4.3 新增部门

```
POST /api/v1/admin/depts
```

#### 2.4.4 编辑部门

```
PUT /api/v1/admin/depts/{deptId}
```

#### 2.4.5 删除部门

```
DELETE /api/v1/admin/depts/{deptId}
```

**安全设计**：有子部门或有关联用户的禁止删除。

---

### 2.5 岗位管理

#### 2.5.1 岗位列表（分页）

```
GET /api/v1/admin/posts?page=1&pageSize=10
```

#### 2.5.2 岗位详情

```
GET /api/v1/admin/posts/{postId}
```

#### 2.5.3 新增/编辑/删除岗位

```
POST   /api/v1/admin/posts
PUT    /api/v1/admin/posts/{postId}
DELETE /api/v1/admin/posts/{postId}
```

---

### 2.6 系统配置管理

#### 2.6.1 批量获取配置（按分组）

```
GET /api/v1/admin/configs?group={configGroup}
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "siteName": "Sparkit",
    "siteLogo": "https://...",
    "siteDesc": "企业级开发框架",
    "captchaEnabled": "true"
  }
}
```

**性能说明**：一次查询同分组下所有配置，返回 key-value 对象，前端直接绑定。

#### 2.6.2 批量保存配置

```
PUT /api/v1/admin/configs/batch
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| group | string | 是 | 配置分组 |
| configs | object | 是 | 完整 key-value JSON 对象 |

**请求示例**：
```json
{
  "group": "site",
  "configs": {
    "siteName": "Sparkit",
    "siteLogo": "https://example.com/logo.png",
    "siteDesc": "企业级开发框架",
    "captchaEnabled": "true"
  }
}
```

**性能设计**：后端循环 `configs` 写入数据库，单事务内批量执行，**前端只需一次请求**。禁止逐条调用 PUT。

#### 2.6.3 获取单个配置

```
GET /api/v1/admin/configs/{configKey}
```

#### 2.6.4 配置管理列表（分页，用于后台表格展示）

```
GET /api/v1/admin/configs/list?page=1&pageSize=10&group=&keyword=
```

---

### 2.7 字典管理

#### 2.7.1 字典类型列表（分页）

```
GET /api/v1/admin/dict/types?page=1&pageSize=10
```

#### 2.7.2 根据类型获取字典数据

```
GET /api/v1/admin/dict/datas/{dictType}
```

**性能说明**：字典数据缓存到 Redis，设置版本号，数据变更时刷新缓存。

#### 2.7.3 字典类型 CRUD

```
POST   /api/v1/admin/dict/types
PUT    /api/v1/admin/dict/types/{dictId}
DELETE /api/v1/admin/dict/types/{dictId}
```

#### 2.7.4 字典数据 CRUD

```
POST   /api/v1/admin/dict/datas
PUT    /api/v1/admin/dict/datas/{dataId}
DELETE /api/v1/admin/dict/datas/{dataId}
```

#### 2.7.5 刷新字典缓存

```
POST /api/v1/admin/dict/cache/refresh
```

---

### 2.8 地区管理

#### 2.8.1 获取地区树（懒加载）

```
GET /api/v1/admin/regions/tree?parentId=0
```

#### 2.8.2 根据编码获取下级

```
GET /api/v1/admin/regions/children/{regionCode}
```

**性能说明**：地区数据全量缓存 Redis，前端逐级加载时直接从缓存读取。

---

### 2.9 国际化

#### 2.9.1 按模块获取语言包

```
GET /api/v1/admin/i18n/package?langCode=zh_CN&module=
```

**性能说明**：语言包按模块 + 语言缓存 Redis，前端按需加载，避免一次性加载全部语言包。

#### 2.9.2 i18n 列表（分页）

```
GET /api/v1/admin/i18n/list?page=1&pageSize=10&langCode=&module=&keyword=
```

#### 2.9.3 i18n 批量保存

```
POST /api/v1/admin/i18n/batch
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| items | object[] | 是 | 翻译条目数组 |

**设计说明**：批量保存，前端一次提交多个翻译条目，后端循环处理。

#### 2.9.4 刷新语言包缓存

```
POST /api/v1/admin/i18n/cache/refresh
```

---

## 三、用户模块 API（C端）

> 基础路径：`/api/v1/public/user`（公开接口）、`/api/v1/user`（需登录）

### 3.1 注册与登录

#### 3.1.1 发送验证码

```
POST /api/v1/public/user/send-code
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| target | string | 是 | 手机号或邮箱 |
| targetType | string | 是 | phone/email |
| scene | string | 是 | register/login/resetPassword/bind/change |

**安全设计**：
- 同一 target 同一 scene 60 秒内只能发送一次
- 同一 IP 每小时最多发送 10 次
- 验证码 5 分钟有效

#### 3.1.2 账号注册

```
POST /api/v1/public/user/register
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码（前端 RSA 加密） |
| phone | string | 否 | 手机号 |
| email | string | 否 | 邮箱 |
| code | string | 是 | 验证码 |
| codeKey | string | 是 | 验证码关联 Key |

#### 3.1.3 账号密码登录

```
POST /api/v1/public/user/login
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| account | string | 是 | 用户名/手机号/邮箱 |
| password | string | 是 | 密码 |
| loginType | string | 否 | 登录方式：account/phone/email |

**安全设计**：登录失败 5 次锁定账号 30 分钟，记录登录日志。

#### 3.1.4 手机号验证码登录

```
POST /api/v1/public/user/login-by-code
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | string | 是 | 手机号 |
| code | string | 是 | 短信验证码 |

#### 3.1.5 一键登录（阿里云号码认证）

```
POST /api/v1/public/user/login-by-one-click
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| accessToken | string | 是 | 运营商返回的 accessToken |

#### 3.1.6 第三方登录

```
POST /api/v1/public/user/login-by-third-party
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| platform | string | 是 | wechat/wecom/qq/weibo/github/dingtalk |
| code | string | 是 | 第三方授权 code |
| appType | string | 否 | mp/app/web/h5 |
| state | string | 否 | 防 CSRF 参数 |

#### 3.1.7 刷新 Token

```
POST /api/v1/public/user/refresh-token
```

#### 3.1.8 退出登录

```
POST /api/v1/user/logout
```

---

### 3.2 用户信息

#### 3.2.1 获取当前用户信息

```
GET /api/v1/user/info
```

#### 3.2.2 修改个人信息

```
PUT /api/v1/user/profile
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| nickname | string | 否 | 昵称 |
| avatar | string | 否 | 头像URL |
| realName | string | 否 | 真实姓名 |
| idCard | string | 否 | 身份证号 |
| gender | int | 否 | 0=未知, 1=男, 2=女 |
| birthday | string | 否 | 生日 (yyyy-MM-dd) |

**安全设计**：身份证号加密存储，列表接口返回脱敏值。

#### 3.2.3 修改密码

```
PUT /api/v1/user/profile/password
```

#### 3.2.4 绑定/解绑手机号

```
POST /api/v1/user/profile/bind-phone
POST /api/v1/user/profile/unbind-phone
```

#### 3.2.5 绑定/解绑邮箱

```
POST /api/v1/user/profile/bind-email
POST /api/v1/user/profile/unbind-email
```

#### 3.2.6 绑定第三方账号

```
POST /api/v1/user/profile/bind-third-party
```

---

### 3.3 用户管理（后台）

#### 3.3.1 用户列表（分页）

```
GET /api/v1/admin/c-users?page=1&pageSize=10&keyword=&status=&startTime=&endTime=
```

**性能说明**：count 走 SQL 聚合，列表字段脱敏处理。

#### 3.3.2 用户详情

```
GET /api/v1/admin/c-users/{userId}
```

#### 3.3.3 禁用/启用用户

```
PUT /api/v1/admin/c-users/{userId}/status
```

#### 3.3.4 用户统计

```
GET /api/v1/admin/c-users/statistics
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "totalUsers": 12800,
    "todayNew": 156,
    "weekNew": 1205,
    "monthNew": 4520,
    "activeUsers": 3890
  }
}
```

**性能设计**：使用 SQL 聚合函数 `COUNT()` + `WHERE` 条件分别统计，**禁止拉取全量列表后 count**。

---

### 3.4 黑名单管理

#### 3.4.1 黑名单列表（分页）

```
GET /api/v1/admin/blacklist?page=1&pageSize=10&targetType=&keyword=
```

#### 3.4.2 拉黑/解封

```
POST /api/v1/admin/blacklist
DELETE /api/v1/admin/blacklist/{blacklistId}
```

---

## 四、存储模块 API

> 基础路径：`/api/v1/storage`

### 4.1 存储源配置（后台管理）

#### 4.1.1 存储源列表

```
GET /api/v1/admin/storages?page=1&pageSize=10
```

#### 4.1.2 存储源 CRUD

```
POST   /api/v1/admin/storages
PUT    /api/v1/admin/storages/{storageId}
DELETE /api/v1/admin/storages/{storageId}
```

**安全设计**：`accessKey`/`secretKey` 加密存储，接口返回时脱敏。

---

### 4.2 文件上传

#### 4.2.1 普通上传

```
POST /api/v1/storage/upload
Content-Type: multipart/form-data
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | 文件 |
| storageId | long | 否 | 指定存储源（不传则使用默认） |
| fileType | string | 否 | 文件分类 |

**响应**：
```json
{
  "code": 200,
  "data": {
    "fileId": 1001,
    "fileName": "example.jpg",
    "fileUrl": "https://cdn.example.com/2024/06/example.jpg",
    "fileSize": 204800,
    "md5": "d41d8cd98f00b204e9800998ecf8427e"
  }
}
```

#### 4.2.2 分片上传 - 初始化

```
POST /api/v1/storage/chunk/init
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| fileName | string | 是 | 文件名 |
| fileSize | long | 是 | 文件总大小（字节） |
| chunkSize | long | 是 | 分片大小（字节） |
| md5 | string | 是 | 文件完整 MD5 |
| storageId | long | 否 | 指定存储源 |

**响应**：返回 `uploadId`，用于后续分片上传。

**MD5 秒传设计**：后端先根据 MD5 查询 `sparkit_file` 表，若已存在相同 MD5 且上传完成的文件，直接返回已有文件信息，跳过上传。

#### 4.2.3 分片上传 - 上传分片

```
POST /api/v1/storage/chunk/upload
Content-Type: multipart/form-data
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| uploadId | string | 是 | 上传任务ID |
| chunkIndex | int | 是 | 分片序号（从0开始） |
| chunkMd5 | string | 是 | 分片 MD5 |
| file | file | 是 | 分片文件 |

**响应**：返回当前分片上传状态。

#### 4.2.4 分片上传 - 合并

```
POST /api/v1/storage/chunk/merge
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| uploadId | string | 是 | 上传任务ID |

**响应**：合并完成后返回完整文件信息。

#### 4.2.5 分片上传 - 取消

```
POST /api/v1/storage/chunk/cancel
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| uploadId | string | 是 | 上传任务ID |

**设计说明**：取消后清理已上传的分片数据。

#### 4.2.6 检查文件 MD5（秒传判断）

```
POST /api/v1/storage/check-md5
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| md5 | string | 是 | 文件 MD5 |

**性能设计**：仅查询 `sparkit_file` 索引，不产生 IO。

---

### 4.3 文件管理

#### 4.3.1 文件列表（分页）

```
GET /api/v1/admin/files?page=1&pageSize=10&fileType=&storageId=&keyword=&startTime=&endTime=
```

#### 4.3.2 文件详情

```
GET /api/v1/admin/files/{fileId}
```

#### 4.3.3 删除文件

```
DELETE /api/v1/admin/files/{fileId}
```

#### 4.3.4 文件下载

```
GET /api/v1/storage/download/{fileId}
```

**安全设计**：校验下载权限，生成临时下载链接（签名 URL，有效期 5 分钟），302 跳转。

#### 4.3.5 文件统计

```
GET /api/v1/admin/files/statistics
```

**性能设计**：使用 SQL 聚合：
```sql
SELECT 
  COUNT(*) AS totalCount,
  SUM(file_size) AS totalSize,
  COUNT(CASE WHEN file_type='image' THEN 1 END) AS imageCount,
  COUNT(CASE WHEN file_type='video' THEN 1 END) AS videoCount
FROM sparkit_file
WHERE deleted = 0
```

---

## 五、支付模块 API

> 基础路径：`/api/v1/payment`

### 5.1 支付渠道配置（后台管理）

#### 5.1.1 渠道列表

```
GET /api/v1/admin/payment/configs?page=1&pageSize=10
```

#### 5.1.2 渠道配置 CRUD

```
POST   /api/v1/admin/payment/configs
PUT    /api/v1/admin/payment/configs/{configId}
DELETE /api/v1/admin/payment/configs/{configId}
```

**安全设计**：`apiKey`/`privateKey` 加密存储，返回时脱敏。

---

### 5.2 支付订单

#### 5.2.1 创建支付订单

```
POST /api/v1/payment/orders
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderNo | string | 是 | 业务订单号（唯一） |
| channel | string | 是 | 支付渠道 |
| amount | decimal | 是 | 金额（元） |
| subject | string | 是 | 商品标题 |
| body | string | 否 | 商品描述 |
| tradeType | string | 否 | 交易类型：NATIVE/JSAPI/APP/MP |
| openId | string | 否 | 用户 OpenID（JSAPI 必填） |
| callbackUrl | string | 否 | 业务回调地址 |
| idempotentKey | string | 是 | 幂等键（防重复提交） |

**响应**：
```json
{
  "code": 200,
  "data": {
    "paymentNo": "PAY202406180001",
    "channel": "wechat",
    "qrCode": "weixin://wxpay/...",
    "payUrl": "https://...",
    "expireTime": "2024-06-18 14:00:00"
  }
}
```

**安全设计**：
- `idempotentKey` 防重复创建，同一 key 重复请求返回已有订单
- 请求签名验证

#### 5.2.2 查询订单状态

```
GET /api/v1/payment/orders/{paymentNo}
```

#### 5.2.3 主动查询（同步第三方状态）

```
POST /api/v1/payment/orders/{paymentNo}/sync
```

#### 5.2.4 关闭订单

```
POST /api/v1/payment/orders/{paymentNo}/close
```

#### 5.2.5 支付回调通知

```
POST /api/v1/public/payment/notify/{channel}
```

**安全设计**：
- 验签（各渠道 SDK 提供的验签方法）
- 幂等处理（同一 paymentNo 回调多次不影响）
- 回调数据原样存储到 `notify_data` 字段

---

### 5.3 退款

#### 5.3.1 申请退款

```
POST /api/v1/payment/refunds
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderId | long | 是 | 订单ID |
| refundAmount | decimal | 是 | 退款金额（元） |
| refundReason | string | 否 | 退款原因 |
| idempotentKey | string | 是 | 幂等键 |

#### 5.3.2 退款查询

```
GET /api/v1/payment/refunds/{refundNo}
```

#### 5.3.3 退款回调

```
POST /api/v1/public/payment/refund/notify/{channel}
```

---

### 5.4 支付管理（后台）

#### 5.4.1 订单列表（分页）

```
GET /api/v1/admin/payment/orders?page=1&pageSize=10&channel=&status=&orderNo=&startTime=&endTime=
```

#### 5.4.2 订单详情

```
GET /api/v1/admin/payment/orders/{orderId}
```

#### 5.4.3 退款列表（分页）

```
GET /api/v1/admin/payment/refunds?page=1&pageSize=10&status=&startTime=&endTime=
```

#### 5.4.4 支付统计

```
GET /api/v1/admin/payment/statistics?startTime=&endTime=&channel=
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "totalAmount": 125800.50,
    "totalCount": 3200,
    "successCount": 3050,
    "refundAmount": 5800.00,
    "refundCount": 150
  }
}
```

**性能设计**：使用 SQL 聚合：
```sql
SELECT 
  COUNT(*) AS totalCount,
  SUM(amount) AS totalAmount,
  COUNT(CASE WHEN status=1 THEN 1 END) AS successCount,
  SUM(CASE WHEN status=4 THEN amount ELSE 0 END) AS refundAmount
FROM sparkit_payment_order
WHERE create_time BETWEEN #{startTime} AND #{endTime}
```

#### 5.4.5 对账列表

```
GET /api/v1/admin/payment/reconciliation?page=1&pageSize=10&channel=&reconDate=
```

#### 5.4.6 触发对账

```
POST /api/v1/admin/payment/reconciliation/run
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| channel | string | 是 | 支付渠道 |
| reconDate | string | 是 | 对账日期 (yyyy-MM-dd) |

---

## 六、通知模块 API

> 基础路径：`/api/v1/notify`

### 6.1 通知模板（后台管理）

#### 6.1.1 模板列表（分页）

```
GET /api/v1/admin/notify/templates?page=1&pageSize=10&channel=&keyword=
```

#### 6.1.2 模板 CRUD

```
POST   /api/v1/admin/notify/templates
PUT    /api/v1/admin/notify/templates/{templateId}
DELETE /api/v1/admin/notify/templates/{templateId}
```

---

### 6.2 发送通知

#### 6.2.1 发送通知

```
POST /api/v1/notify/send
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| templateCode | string | 是 | 模板编码 |
| channel | string | 是 | 渠道 |
| receiver | string | 是 | 接收方 |
| params | object | 否 | 模板变量 |

**设计说明**：异步发送，写入 `sparkit_notify_record`，通过消息队列（RabbitMQ/Kafka）异步调用各渠道 SDK。

#### 6.2.2 发送记录（分页）

```
GET /api/v1/admin/notify/records?page=1&pageSize=10&channel=&status=&startTime=&endTime=
```

#### 6.2.3 发送统计

```
GET /api/v1/admin/notify/statistics?startTime=&endTime=
```

**性能设计**：SQL 聚合统计各渠道发送量、成功率。

---

### 6.3 站内信

#### 6.3.1 站内信列表（分页）

```
GET /api/v1/notify/messages?page=1&pageSize=10&isRead=&messageType=
```

#### 6.3.2 未读数量

```
GET /api/v1/notify/messages/unread-count
```

**性能设计**：SQL 聚合：
```sql
SELECT COUNT(*) FROM sparkit_notify_message 
WHERE user_id = #{userId} AND user_type = #{userType} AND is_read = 0
```

#### 6.3.3 标记已读

```
PUT /api/v1/notify/messages/{messageId}/read
PUT /api/v1/notify/messages/read-all
```

---

## 七、AI 模块 API

> 基础路径：`/api/v1/ai`

### 7.1 AI 模型配置（后台管理）

#### 7.1.1 模型列表

```
GET /api/v1/admin/ai/configs?page=1&pageSize=10&provider=&modelType=
```

#### 7.1.2 模型 CRUD

```
POST   /api/v1/admin/ai/configs
PUT    /api/v1/admin/ai/configs/{configId}
DELETE /api/v1/admin/ai/configs/{configId}
```

**安全设计**：`apiKey`/`apiSecret` 加密存储，返回时脱敏。

---

### 7.2 AI 对话

#### 7.2.1 创建会话

```
POST /api/v1/ai/sessions
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| configId | long | 是 | 模型配置ID |
| title | string | 否 | 会话标题（不传自动生成） |

#### 7.2.2 会话列表

```
GET /api/v1/ai/sessions?page=1&pageSize=10
```

#### 7.2.3 会话详情（含消息列表）

```
GET /api/v1/ai/sessions/{sessionId}
```

#### 7.2.4 删除会话

```
DELETE /api/v1/ai/sessions/{sessionId}
```

#### 7.2.5 置顶/归档会话

```
PUT /api/v1/ai/sessions/{sessionId}/pin
PUT /api/v1/ai/sessions/{sessionId}/archive
```

---

### 7.3 AI 消息

#### 7.3.1 发送消息（SSE 流式响应）

```
POST /api/v1/ai/chat
Content-Type: application/json
Accept: text/event-stream
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | long | 是 | 会话ID |
| content | string | 是 | 消息内容 |
| parentMessageId | long | 否 | 父消息ID（分支对话） |

**响应**：SSE 流式输出，事件类型：
- `data`: 增量文本内容
- `reasoning`: 推理过程（DeepSeek 等模型）
- `done`: 生成完成，返回完整消息
- `error`: 生成失败

**性能设计**：使用 SSE（Server-Sent Events）流式传输，避免长连接阻塞；后端异步调用 AI API，逐 token 推送前端。

#### 7.3.2 消息历史

```
GET /api/v1/ai/sessions/{sessionId}/messages?page=1&pageSize=20
```

---

### 7.4 AI 图片/视频生成

#### 7.4.1 图片生成

```
POST /api/v1/ai/image/generate
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| configId | long | 是 | 模型配置ID |
| prompt | string | 是 | 提示词 |
| negativePrompt | string | 否 | 反向提示词 |
| imageSize | string | 否 | 图片尺寸 |
| imageCount | int | 否 | 生成数量 |
| quality | string | 否 | 质量 |
| style | string | 否 | 风格 |

**响应**：返回任务ID，异步生成，前端轮询或 WebSocket 获取结果。

#### 7.4.2 图片生成记录

```
GET /api/v1/ai/image/records/{recordId}
GET /api/v1/ai/image/records?page=1&pageSize=10
```

#### 7.4.3 视频生成

```
POST /api/v1/ai/video/generate
```

#### 7.4.4 视频生成记录

```
GET /api/v1/ai/video/records/{recordId}
GET /api/v1/ai/video/records?page=1&pageSize=10
```

---

### 7.5 AI 知识库

#### 7.5.1 知识库列表

```
GET /api/v1/ai/knowledge-bases?page=1&pageSize=10
```

#### 7.5.2 知识库 CRUD

```
POST   /api/v1/ai/knowledge-bases
PUT    /api/v1/ai/knowledge-bases/{kbId}
DELETE /api/v1/ai/knowledge-bases/{kbId}
```

#### 7.5.3 文档上传

```
POST /api/v1/ai/knowledge-bases/{kbId}/documents
Content-Type: multipart/form-data
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | 文档文件（txt/pdf/md） |

**设计说明**：上传后异步向量化处理，状态通过 `vector_status` 字段反馈。

#### 7.5.4 文档列表

```
GET /api/v1/ai/knowledge-bases/{kbId}/documents?page=1&pageSize=10
```

#### 7.5.5 删除文档

```
DELETE /api/v1/ai/knowledge-bases/{kbId}/documents/{docId}
```

---

### 7.6 AI 用量统计（后台）

#### 7.6.1 用量统计

```
GET /api/v1/admin/ai/usage?startDate=&endDate=&configId=&userId=
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "totalTokens": 12500000,
    "totalCost": 125.50,
    "totalRequests": 8500,
    "dailyUsage": [
      { "date": "2024-06-01", "tokens": 450000, "cost": 4.50, "requests": 300 }
    ]
  }
}
```

**性能设计**：SQL 聚合 `SUM()` 按日期分组统计，不拉全量。

---

## 八、新闻模块 API

> 基础路径：`/api/v1/news`

### 8.1 新闻分类

#### 8.1.1 分类树

```
GET /api/v1/public/news/categories/tree
```

#### 8.1.2 分类管理列表（后台）

```
GET /api/v1/admin/news/categories?page=1&pageSize=10
```

#### 8.1.3 分类 CRUD

```
POST   /api/v1/admin/news/categories
PUT    /api/v1/admin/news/categories/{categoryId}
DELETE /api/v1/admin/news/categories/{categoryId}
```

---

### 8.2 新闻文章

#### 8.2.1 文章列表（公开，分页）

```
GET /api/v1/public/news/articles?page=1&pageSize=10&categoryId=&keyword=&isRecommend=
```

**性能说明**：列表只返回标题、摘要、封面、时间等必要字段，`content` 不返回。

#### 8.2.2 文章详情（公开）

```
GET /api/v1/public/news/articles/{articleId}
```

#### 8.2.3 文章管理列表（后台，分页）

```
GET /api/v1/admin/news/articles?page=1&pageSize=10&categoryId=&status=&keyword=&isAiGenerated=&startTime=&endTime=
```

#### 8.2.4 文章 CRUD（后台）

```
POST   /api/v1/admin/news/articles
PUT    /api/v1/admin/news/articles/{articleId}
DELETE /api/v1/admin/news/articles/{articleId}
```

#### 8.2.5 文章发布/下架

```
PUT /api/v1/admin/news/articles/{articleId}/publish
PUT /api/v1/admin/news/articles/{articleId}/withdraw
```

#### 8.2.6 文章统计

```
GET /api/v1/admin/news/statistics
```

**性能设计**：SQL 聚合：
```sql
SELECT 
  COUNT(*) AS totalArticles,
  COUNT(CASE WHEN status=1 THEN 1 END) AS publishedCount,
  COUNT(CASE WHEN is_ai_generated=1 THEN 1 END) AS aiGeneratedCount,
  SUM(view_count) AS totalViews
FROM sparkit_news_article
WHERE deleted = 0
```

---

### 8.3 评论

#### 8.3.1 文章评论列表（公开，分页）

```
GET /api/v1/public/news/articles/{articleId}/comments?page=1&pageSize=10
```

#### 8.3.2 发表评论

```
POST /api/v1/news/comments
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| articleId | long | 是 | 文章ID |
| content | string | 是 | 评论内容 |
| parentId | long | 否 | 父评论ID |

**安全设计**：内容审核（敏感词过滤），评论默认待审核状态。

#### 8.3.3 评论管理（后台，分页）

```
GET /api/v1/admin/news/comments?page=1&pageSize=10&status=&keyword=&articleId=
```

#### 8.3.4 审核评论

```
PUT /api/v1/admin/news/comments/{commentId}/audit
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | int | 是 | 1=通过, 2=驳回 |
| auditRemark | string | 否 | 审核备注 |

---

### 8.4 AI 新闻任务

#### 8.4.1 任务列表（分页）

```
GET /api/v1/admin/news/ai-tasks?page=1&pageSize=10&status=&taskType=
```

#### 8.4.2 任务 CRUD

```
POST   /api/v1/admin/news/ai-tasks
PUT    /api/v1/admin/news/ai-tasks/{taskId}
DELETE /api/v1/admin/news/ai-tasks/{taskId}
```

#### 8.4.3 手动执行任务

```
POST /api/v1/admin/news/ai-tasks/{taskId}/execute
```

#### 8.4.4 任务执行日志

```
GET /api/v1/admin/news/ai-tasks/{taskId}/logs?page=1&pageSize=10
```

---

## 九、定时任务 API

> 基础路径：`/api/v1/admin/job`

#### 9.1 任务列表（分页）

```
GET /api/v1/admin/jobs?page=1&pageSize=10&jobGroup=&status=&keyword=
```

#### 9.2 任务详情

```
GET /api/v1/admin/jobs/{jobId}
```

#### 9.3 新增任务

```
POST /api/v1/admin/jobs
```

#### 9.4 编辑任务

```
PUT /api/v1/admin/jobs/{jobId}
```

#### 9.5 删除任务

```
DELETE /api/v1/admin/jobs/{jobId}
```

#### 9.6 暂停/恢复/立即执行

```
POST /api/v1/admin/jobs/{jobId}/pause
POST /api/v1/admin/jobs/{jobId}/resume
POST /api/v1/admin/jobs/{jobId}/run
```

#### 9.7 执行日志（分页）

```
GET /api/v1/admin/job-logs?page=1&pageSize=10&jobId=&status=&startTime=&endTime=
```

#### 9.8 清空日志

```
DELETE /api/v1/admin/job-logs/clean
```

---

## 十、日志与审计 API

> 基础路径：`/api/v1/admin/log`

#### 10.1 操作日志（分页）

```
GET /api/v1/admin/logs/oper?page=1&pageSize=10&userType=&operType=&keyword=&startTime=&endTime=
```

#### 10.2 登录日志（分页）

```
GET /api/v1/admin/logs/login?page=1&pageSize=10&userType=&status=&keyword=&startTime=&endTime=
```

#### 10.3 API 访问日志（分页）

```
GET /api/v1/admin/logs/api?page=1&pageSize=10&traceId=&requestUrl=&startTime=&endTime=
```

#### 10.4 敏感操作日志（分页）

```
GET /api/v1/admin/logs/sensitive?page=1&pageSize=10&eventType=&userType=&startTime=&endTime=
```

**性能说明**：日志表数据量大，查询必须带时间范围，索引覆盖 `create_time` 字段。

#### 10.5 日志清理

```
DELETE /api/v1/admin/logs/clean?type=oper&beforeDays=90
```

---

## 十一、多租户与扩展 API

> 基础路径：`/api/v1/admin`

#### 11.1 租户列表（分页）

```
GET /api/v1/admin/tenants?page=1&pageSize=10&keyword=&status=
```

#### 11.2 租户 CRUD

```
POST   /api/v1/admin/tenants
PUT    /api/v1/admin/tenants/{tenantId}
DELETE /api/v1/admin/tenants/{tenantId}
```

**安全设计**：独立数据库配置信息加密存储。

#### 11.3 租户配置

```
GET /api/v1/admin/tenants/{tenantId}/configs
PUT /api/v1/admin/tenants/{tenantId}/configs/batch
```

**设计说明**：与 `sparkit_config` 同理，批量保存配置。

---

### 11.4 App 版本管理

#### 11.4.1 版本列表（分页）

```
GET /api/v1/admin/versions?page=1&pageSize=10&appType=
```

#### 11.4.2 版本 CRUD

```
POST   /api/v1/admin/versions
PUT    /api/v1/admin/versions/{versionId}
DELETE /api/v1/admin/versions/{versionId}
```

#### 11.4.3 检查更新（公开接口）

```
GET /api/v1/public/version/check?appType=ios&versionCode=101
```

**响应**：返回是否需要更新、更新类型、下载地址。

---

### 11.5 用户反馈

#### 11.5.1 提交反馈

```
POST /api/v1/user/feedbacks
```

#### 11.5.2 反馈列表（后台，分页）

```
GET /api/v1/admin/feedbacks?page=1&pageSize=10&feedbackType=&status=&startTime=&endTime=
```

#### 11.5.3 处理反馈

```
PUT /api/v1/admin/feedbacks/{feedbackId}/handle
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | int | 是 | 处理状态 |
| handleRemark | string | 否 | 处理备注 |

---

## 十二、通用接口

#### 12.1 验证码生成

```
GET /api/v1/public/captcha
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "captchaKey": "uuid",
    "captchaImage": "data:image/png;base64,..."
  }
}
```

#### 12.2 文件上传（通用）

```
POST /api/v1/common/upload
Content-Type: multipart/form-data
```

**设计说明**：通用上传接口，不指定存储源时使用默认存储源。

#### 12.3 导出 Excel

```
POST /api/v1/admin/export/{module}
```

**设计说明**：传入当前查询参数，后端异步生成 Excel 并返回下载链接。

---

## 附录：接口汇总

| 模块 | 接口数 | 路径前缀 |
|------|--------|----------|
| 系统管理 | 35+ | `/api/v1/admin/` |
| 用户模块（C端） | 15+ | `/api/v1/public/user/`, `/api/v1/user/` |
| 存储模块 | 12+ | `/api/v1/storage/`, `/api/v1/admin/` |
| 支付模块 | 12+ | `/api/v1/payment/`, `/api/v1/admin/payment/` |
| 通知模块 | 8+ | `/api/v1/notify/`, `/api/v1/admin/notify/` |
| AI 模块 | 18+ | `/api/v1/ai/`, `/api/v1/admin/ai/` |
| 新闻模块 | 18+ | `/api/v1/public/news/`, `/api/v1/news/`, `/api/v1/admin/news/` |
| 定时任务 | 8+ | `/api/v1/admin/job/` |
| 日志审计 | 5+ | `/api/v1/admin/logs/` |
| 多租户/扩展 | 10+ | `/api/v1/admin/` |
| 通用 | 3+ | `/api/v1/common/`, `/api/v1/public/` |

> **总计约 140+ 接口**