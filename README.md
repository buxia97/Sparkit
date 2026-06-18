# Sparkit 通用开发框架

## 一、项目简介

Sparkit 是一套多端通用开发框架，旨在快速构建企业级 Web 应用与移动端 App。框架采用前后端分离架构，后端基于 Spring Boot 3.5 + Java，Web 端基于 NuxtJS + Vue 3，App 端基于 UniApp + Vue 3，一套代码同时覆盖 H5、iOS、Android 及各大主流小程序平台。

---

## 二、技术栈

| 层级 | 技术选型 | 版本要求 |
|------|----------|----------|
| 后端框架 | Spring Boot 3.5 | 3.5.x |
| 开发语言 | Java | JDK 17+（兼容所有 LTS 版本） |
| 数据库 | MySQL | 5.5 ~ 8.0 兼容 |
| ORM | MyBatis-Plus + JPA | 最新稳定版 |
| 缓存 | Redis | 6.0+ |
| Web 前端 | NuxtJS + Vue 3 + Element Plus | Nuxt 3.x / Vue 3.x |
| App 端 | UniApp + Vue 3 | UniApp 3.x |
| 构建工具 | Vite / Webpack | 按端适配 |
| 包管理 | pnpm / npm | 最新稳定版 |

---

## 三、项目结构

```
Sparkit/
├── sparkit-server/                 # 后端 Spring Boot 工程
│   ├── sparkit-common/             # 公共模块（工具类、异常、枚举、常量）
│   ├── sparkit-framework/          # 框架核心（拦截器、配置、安全、缓存）
│   ├── sparkit-system/             # 系统管理模块
│   ├── sparkit-user/               # 用户模块
│   ├── sparkit-storage/            # 存储模块
│   ├── sparkit-payment/            # 支付模块
│   ├── sparkit-notification/       # 通知模块
│   ├── sparkit-ai/                 # AI 模块
│   ├── sparkit-news/               # 新闻模块
│   ├── sparkit-generator/          # 代码生成器
│   ├── sparkit-job/                # 定时任务模块
│   └── sparkit-start/              # 管理后台管理后台
├── sparkit-admin-web/              # 管理中心前端 NuxtJS 3 + Vue 3 工程
├── sparkit-web/                    # Web 前端 NuxtJS 3 工程
├── sparkit-app/                    # App 端 UniApp 工程
├── sparkit-shell/                  # 运维脚本（Windows .bat / Linux .sh）
└── docs/                           # 项目文档
```

---

## 四、功能模块

### 4.1 用户模块 (User)

| 功能 | 说明 |
|------|------|
| 账号注册/登录 | 用户名 + 密码 + 验证码注册与登录 |
| 手机号验证 | 阿里云短信验证 / 阿里云号码认证（一键登录） / 飞鸽云短信验证 |
| 邮箱验证 | 任意邮箱验证码 / 阿里云企业邮箱验证 |
| 微信登录 | 小程序登录 / App 微信授权 / 微信扫码关注公众号登录 |
| 企业微信登录 | 企业微信 OAuth2.0 授权登录 |
| QQ 登录 | Web 网页登录 / App 授权 / 小程序登录 |
| 微博登录 | Web 网页登录 / App 授权登录 |
| GitHub 登录 | OAuth 授权登录 |
| 钉钉登录 | 钉钉扫码登录 / 企业内部应用登录 |
| 用户信息管理 | 头像、昵称、密码修改、手机/邮箱绑定与解绑 |
| 实名认证 | 身份证实名认证（阿里云/腾讯云实名认证接口） |
| 用户等级 | 等级体系、经验值、成长值 |
| 黑名单管理 | 用户封禁 / 解封，IP 黑名单 |

### 4.2 权限管理 (RBAC)

| 功能 | 说明 |
|------|------|
| 用户管理 | 用户增删改查、状态管理、重置密码 |
| 角色管理 | 角色分配、角色权限绑定 |
| 菜单管理 | 多级菜单、按钮权限、接口权限 |
| 部门管理 | 组织架构树、部门负责人 |
| 岗位管理 | 岗位与部门关联 |
| 数据权限 | 按部门/角色/自定义规则进行数据隔离 |
| 登录策略 | 单设备登录 / 多设备登录 / 登录失败锁定 |

### 4.3 系统管理 (System)

| 功能 | 说明 |
|------|------|
| 系统配置 | 动态参数配置（文本、图片、开关、JSON） |
| 字典管理 | 数据字典维护与缓存 |
| 地区管理 | 中国省/市/区三级联动数据 |
| 国际化 (i18n) | 前后端多语言支持（中/英/日/韩等） |
| 操作日志 | 记录用户关键操作，支持审计追溯 |
| 登录日志 | 登录时间、IP、设备、状态记录 |
| 文件管理 | 文件上传/下载/预览/在线编辑（Office/PDF） |
| 导入导出 | Excel / Word / PDF 数据导入导出（EasyExcel） |
| 主题皮肤 | Web 端多主题切换、暗黑模式 |

### 4.4 存储模块 (Storage)

| 功能 | 说明 |
|------|------|
| 本地存储 | 服务器本地磁盘存储 |
| FTP 远程存储 | FTP/SFTP 协议远程文件存储 |
| 阿里云 OSS | 阿里云对象存储服务 |
| 腾讯云 COS | 腾讯云对象存储服务 |
| 七牛云 Kodo | 七牛云对象存储 |
| S3 通用协议 | 兼容 AWS S3 协议（MinIO / 华为云 OBS 等） |
| 存储策略 | 多存储源动态切换、自动灾备 |
| 分片上传 | 大文件分片上传、断点续传、秒传 |
| MD5 校验 | 文件上传完整性校验、秒传识别 |
| 文件预览 | 图片/视频/Office 文件在线预览 |

### 4.5 支付模块 (Payment)

| 功能 | 说明 |
|------|------|
| 微信支付-Native | PC 网站扫码支付（NATIVE） |
| 微信支付-JSAPI | 微信内网页/公众号支付 |
| 微信支付-APP | App 内调起微信支付 |
| 微信支付-小程序 | 小程序内支付 |
| 微信支付-虚拟支付 | 小程序虚拟商品支付（会员/课程） |
| 支付宝-当面付 | 扫码支付（PC/Native） |
| 支付宝-APP | App 内调起支付宝支付 |
| PayPal | 国际 PayPal 支付 |
| Apple Pay | iOS App 内 Apple Pay |
| Google Pay | Android App 内 Google Pay |
| 支付回调 | 统一支付回调处理、幂等性保证 |
| 退款管理 | 原路退款、部分退款 |
| 账单对账 | 定时对账、差异告警 |

### 4.6 通知模块 (Notification)

| 功能 | 说明 |
|------|------|
| 微信公众号通知 | 公众号模板消息推送 |
| 邮件通知 | SMTP 邮件发送（支持多模板） |
| 手机短信通知 | 阿里云/飞鸽云短信发送 |
| UniPush 推送 | DCloud UniPush App 消息推送 |
| 站内信 | 系统内消息通知、已读/未读管理 |
| 通知模板 | 自定义通知模板、变量替换 |
| 通知策略 | 多通道组合通知、优先级路由 |

### 4.7 AI 模块 (AI)

| 功能 | 说明 |
|------|------|
| DeepSeek | 文字生成/对话/代码生成 |
| 小米大模型 | 文字生成/对话 |
| 阿里百炼 | 文字生成/图片生成/视频生成 |
| 模型切换 | 统一 API 接口，动态切换模型供应商 |
| 流式输出 | SSE / WebSocket 流式返回 |
| 对话管理 | 多轮对话上下文管理、会话历史 |
| 内容审核 | AI 生成内容合规审核 |

### 4.8 新闻模块 (News)

| 功能 | 说明 |
|------|------|
| 新闻分类 | 多级分类管理、排序 |
| 新闻文章 | 文章发布、编辑、草稿、定时发布 |
| 富文本编辑 | 所见即所得编辑器 |
| AI 采集 | 基于 AI 的新闻自动采集与摘要 |
| AI 生成 | 基于大模型的新闻内容自动生成 |
| 标签管理 | 文章标签/关键词 |
| 评论管理 | 文章评论、审核、举报 |
| SEO 优化 | 静态生成(SSG)、Meta 标签、Sitemap |

### 4.9 定时任务 (Job)

| 功能 | 说明 |
|------|------|
| 任务调度 | 基于 Quartz 的定时任务管理 |
| 分布式调度 | XXL-JOB 集成（可选） |
| 任务日志 | 执行记录、耗时统计、失败告警 |
| 动态管理 | 在线创建/暂停/恢复/删除任务 |

### 4.10 安全防护 (Security)

| 功能 | 说明 |
|------|------|
| 验证码 | 图片验证码 / 滑块验证码 / 腾讯云验证码 |
| XSS 防护 | 输入过滤、输出编码 |
| CSRF 防护 | Token 校验 |
| SQL 注入防护 | 参数化查询 + 过滤器 |
| 限流 | 接口级别限流（令牌桶/漏桶算法） |
| 敏感数据加密 | 密码 BCrypt / 手机号脱敏 / 身份证脱敏 |
| API 签名 | 接口签名校验，防篡改/防重放 |
| HTTPS | 强制 HTTPS、证书管理 |

### 4.11 监控运维 (Monitor)

| 功能 | 说明 |
|------|------|
| 健康检查 | Spring Boot Actuator 端点 |
| 性能监控 | Prometheus + Grafana 集成 |
| 链路追踪 | SkyWalking / Micrometer Tracing |
| 在线用户 | 实时查看在线用户、强制下线 |
| 服务监控 | CPU / 内存 / 磁盘 / JVM 监控 |
| 数据库监控 | 慢 SQL 记录、连接池监控 |
| 配置中心 | Nacos 配置管理与热更新 |
| 服务注册发现 | Nacos 服务注册与发现 |
| 灰度发布 | Feature Flag 功能开关、AB 测试 |

### 4.12 分布式能力 (Distributed)

| 功能 | 说明 |
|------|------|
| 分布式锁 | Redisson 分布式锁（可重入/公平/联锁） |
| 分布式事务 | Seata AT/TCC 模式 |
| 分布式 ID | 雪花算法 Snowflake |
| 分布式 Session | Redis Session 共享 |
| 消息队列 | RabbitMQ / RocketMQ / Kafka 集成 |

### 4.13 多租户 (Multi-Tenancy)

| 功能 | 说明 |
|------|------|
| 租户模式 | 独立数据库 / 共享数据库独立 Schema / 共享数据表 |
| 租户管理 | 租户套餐、租户域名、租户状态 |
| 数据隔离 | 自动 SQL 拦截注入租户 ID |
| 租户权限 | 租户级管理员、租户内角色权限 |

### 4.14 代码生成器 (Generator)

| 功能 | 说明 |
|------|------|
| 表结构导入 | 从数据库表自动生成代码 |
| 模板引擎 | Velocity / Freemarker 可自定义模板 |
| 生成内容 | Entity / Mapper / Service / Controller / Vue 页面 / API |
| 预览下载 | 在线预览生成结果、一键下载 |

### 4.15 搜索引擎 (Search)

| 功能 | 说明 |
|------|------|
| 全文搜索 | Elasticsearch 集成 |
| 索引管理 | 自动索引同步、增量更新 |
| 搜索建议 | 搜索补全、热门搜索 |
| 高亮 | 搜索结果关键词高亮 |

### 4.16 数据备份 (Backup)

| 功能 | 说明 |
|------|------|
| 数据库备份 | 定时全量/增量备份 |
| 备份恢复 | 一键恢复、回滚 |
| 远程备份 | 备份文件同步至 OSS/S3/FTP |

---

## 五、管理中心 (Sparkit-Admin)

基于 NuxtJS 3 + Vue 3 构建的后台管理中心，提供可视化的系统管理界面。

| 功能 | 说明 |
|------|------|
| 仪表盘 | 系统概览、数据统计、图表展示 |
| 用户管理 | 用户列表、角色分配、状态管理 |
| 权限管理 | 菜单/按钮/接口权限配置 |
| 系统配置 | 参数设置、字典维护、地区管理 |
| 文件管理 | 文件上传、预览、存储源切换 |
| 日志审计 | 操作日志、登录日志查询 |
| 定时任务 | 任务调度管理与监控 |
| 代码生成 | 在线代码生成器 |
| 多租户 | 租户管理与套餐配置 |
| 主题切换 | 多主题/暗黑模式支持 |

---

## 六、运维脚本 (Sparkit-Shell)

提供 Windows / Linux 环境下项目后端的启动、管控、监控脚本，核心覆盖四大能力：**启动、停止、重启、守护监听**。

### 6.1 核心四件套

| 脚本 | 功能 | 说明 |
|------|------|------|
| **start** | 启动 | 启动 Spring Boot 后端服务，支持指定 profile、JVM 参数 |
| **stop** | 停止 | 优雅停止服务（先 SIGTERM，超时后 SIGKILL 强杀） |
| **restart** | 重启 | 先停止再启动，实现无缝重启 |
| **watch** | 守护监听 | 循环检测服务存活状态，发现未启动或异常退出时自动拉起，支持间隔轮询 (默认 5s) 与告警 |

### 6.2 目录结构

```
sparkit-shell/
├── windows/                        # Windows 批处理脚本
│   ├── start.bat                   # 启动服务
│   ├── stop.bat                    # 停止服务
│   ├── restart.bat                 # 重启服务
│   ├── watch.bat                   # 守护监听（未启动自动拉起）
│   ├── status.bat                  # 查看服务状态
│   ├── backup-db.bat               # 数据库备份
│   ├── restore-db.bat              # 数据库恢复
│   ├── log-monitor.bat             # 日志实时监控
│   ├── health-check.bat            # 健康检查
│   └── jvm-monitor.bat             # JVM 性能监控
└── linux/                          # Linux Shell 脚本
    ├── start.sh                    # 启动服务
    ├── stop.sh                     # 停止服务
    ├── restart.sh                  # 重启服务
    ├── watch.sh                    # 守护监听（未启动自动拉起，可配合 systemd 使用）
    ├── status.sh                   # 查看服务状态
    ├── backup-db.sh                # 数据库备份
    ├── restore-db.sh               # 数据库恢复
    ├── log-monitor.sh              # 日志实时监控
    ├── health-check.sh             # 健康检查
    ├── jvm-monitor.sh              # JVM 性能监控
    └── deploy.sh                   # 一键部署脚本
```

### 6.3 脚本功能说明

| 脚本 | Windows | Linux | 功能描述 |
|------|---------|-------|----------|
| 启动服务 | start.bat | start.sh | 启动 Spring Boot 后端服务，支持指定 profile 与 JVM 参数 |
| 停止服务 | stop.bat | stop.sh | 优雅停止服务（先 soft kill，超时后 force kill） |
| 重启服务 | restart.bat | restart.sh | 先 stop 再 start，实现服务重启 |
| 守护监听 | watch.bat | watch.sh | 循环检测进程/端口存活状态，未启动时自动拉起；支持轮询间隔、最大重试次数、钉钉/邮件告警通知 |
| 服务状态 | status.bat | status.sh | 查看进程 PID、端口监听、运行时长、内存占用 |
| 数据库备份 | backup-db.bat | backup-db.sh | 执行 MySQL 全量备份并压缩归档 |
| 数据库恢复 | restore-db.bat | restore-db.sh | 从备份文件恢复数据库 |
| 日志监控 | log-monitor.bat | log-monitor.sh | 实时 tail 日志，支持关键字过滤与告警 |
| 健康检查 | health-check.bat | health-check.sh | 调用 Actuator /health 端点，返回服务健康状态 |
| JVM 监控 | jvm-monitor.bat | jvm-monitor.sh | 查看 JVM 堆内存、GC、线程数等指标 |
| 一键部署 | - | deploy.sh | 自动拉取代码、编译、备份、发布 |

### 6.4 守护监听 (watch) 工作流程

```
┌──────────────────────────────────────┐
│  watch 启动                           │
│    │                                  │
│    ▼                                  │
│  检查服务是否存活（PID + 端口）        │
│    │                        │         │
│   存活                    未存活       │
│    │                        │         │
│    ▼                        ▼         │
│  等待轮询间隔(5s)       执行 start 拉起 │
│    │                        │         │
│    │              ┌─────────┘         │
│    │              ▼                   │
│    │         拉起成功？               │
│    │         │         │              │
│    │        是         否              │
│    │         │         │              │
│    │         │    重试次数+1          │
│    │         │         │              │
│    │         │    超过最大重试？       │
│    │         │    │         │         │
│    │         │   否        是          │
│    │         │    │         │          │
│    │         │    │    发送告警通知    │
│    │         │    │    退出守护       │
│    └─────────┴────┴───────────────────│
│                      │                │
│                      ▼                │
│               循环回到检查             │
└──────────────────────────────────────┘
```

---

## 七、API 文档

- Swagger / Knife4j 自动生成 API 文档
- 支持在线调试接口
- 导出 OpenAPI 3.0 规范 JSON

---

## 八、快速开始

### 8.1 环境要求

- JDK 17+
- MySQL 5.5 ~ 8.0
- Redis 6.0+
- Maven 3.8+
- Node.js 18+
- pnpm 8+

### 8.2 后端启动

```bash
# 克隆项目
git clone https://github.com/your-org/sparkit.git

# 初始化数据库（执行 sparkit-server/sql/ 下的 SQL 脚本）

# 修改配置
# sparkit-server/sparkit-start/src/main/resources/application.yml

# 启动
cd sparkit-server
mvn clean install -DskipTests
cd sparkit-start
mvn spring-boot:run
```

### 8.3 Web 前端启动

```bash
cd sparkit-web
pnpm install
pnpm dev
```

### 8.4 管理中心启动

```bash
cd sparkit-admin-web
pnpm install
pnpm dev
```

### 8.5 App 端启动

使用 HBuilderX 打开 `sparkit-app` 目录，配置 `manifest.json` 后运行到对应平台。

### 8.6 运维脚本使用

```bash
# Windows
cd sparkit-shell/windows
start.bat              # 启动服务
stop.bat               # 停止服务
restart.bat            # 重启服务
watch.bat              # 启动守护监听（后台常驻，未启动自动拉起）

# Linux
cd sparkit-shell/linux
chmod +x *.sh
./start.sh             # 启动服务
./stop.sh              # 停止服务
./restart.sh           # 重启服务
nohup ./watch.sh &     # 后台守护监听（推荐配合 systemd 注册为服务）
```

---

## 九、部署方案

| 部署方式 | 说明 |
|----------|------|
| 单机部署 | jar 包 + nginx 反向代理 |
| Docker 部署 | Docker Compose 一键编排 |
| Kubernetes | K8s 集群部署（Helm Chart） |
| CI/CD | GitHub Actions / GitLab CI / Jenkins 持续集成 |

---

## 十、贡献指南

欢迎提交 Issue 和 Pull Request 参与贡献。

---

## 十一、开源协议

本项目采用 [Apache License 2.0](LICENSE) 开源协议。