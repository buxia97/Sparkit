# AGENTS.md - Sparkit 开发指南

本文档为 AI 编码助手提供 Sparkit 项目上下文，确保后续开发工作的一致性。

---

## 一、项目概况

Sparkit 是一套多端通用开发框架，采用前后端分离架构：
- **后端** `sparkit-server/`：Spring Boot 3.5 + Java 17 + MyBatis-Plus
- **Web 前端** `sparkit-admin-web/`：Nuxt 3 (SPA) + Vue 3 + Element Plus + Pinia
- **App 端** `sparkit-app/`：UniApp + Vue 3
- **辅助** `sparkit-shell/`：运维脚本，`docs/`：设计文档

---

## 二、技术栈与关键版本

| 组件 | 版本 | 备注 |
|------|------|------|
| Java | **17** (Temurin) | 当前运行时，不要升级到 21 |
| Spring Boot | 3.5.14 | |
| MyBatis-Plus | 3.5.9 | 含 jsqlparser |
| MySQL Connector | 8.0.33 | |
| JJWT | 0.12.6 | api / impl / jackson 三个模块 |
| Hutool | 5.8.34 | hutool-all |
| Knife4j | **4.4.0** | OpenAPI3 Jakarta 版，**必须用官方 `com.github.xiaoymin`** |
| SpringDoc | 2.8.16 | |
| Fastjson2 | 2.0.53 | |
| EasyExcel | 4.0.3 | |
| Redisson | 3.41.0 | |
| Lombok | 1.18.36 | |
| MapStruct | 1.6.3 | |
| Nuxt | ^3.12.0 | SPA 模式 (`ssr: false`) |
| Vue | ^3.4.0 | |
| Element Plus | ^2.8.0 | |
| Pinia | ^2.1.0 | |

## 三、后端项目结构 (`sparkit-server/`)

### 3.1 模块依赖层次

```
sparkit-start (启动入口，聚合所有模块)
  ├── sparkit-system    (系统管理)
  ├── sparkit-user      (用户管理)
  ├── sparkit-storage   (存储管理)
  ├── sparkit-payment   (支付管理)
  ├── sparkit-notification (通知管理)
  ├── sparkit-ai        (AI管理)
  ├── sparkit-news      (新闻管理)
  ├── sparkit-job       (定时任务)
  ├── sparkit-generator (代码生成)
  ├── sparkit-backup    (数据备份)
  └── sparkit-search    (搜索引擎)

sparkit-framework (框架核心，被上述模块依赖)
  └── sparkit-common (公共模块)
```

### 3.2 各模块职责

| 模块 | 职责 |
|------|------|
| `sparkit-common` | 基础类：`BaseEntity`、`R<T>`(统一响应体)、`PageQuery`/`PageResult`、`ErrorCode`枚举、`BusinessException`、工具类、安全过滤器 |
| `sparkit-framework` | 框架层：Spring Security 配置、JWT 认证、分布式锁AOP、限流AOP、全局异常处理、`ResponseAdvice`(统一响应包装)、MyBatis-Plus/Redis/Redisson 配置 |
| `sparkit-start` | 启动模块：`SparkitStartApplication`、`application.yml`。`@SpringBootApplication(scanBasePackages = "com.sparkit")` |
| `sparkit-system` | 管理员、角色、菜单、部门、岗位、配置、字典、地区、i18n、导入导出、主题 |
| `sparkit-user` | C端用户注册登录、第三方登录、验证码、黑名单 |
| `sparkit-storage` | 文件上传/下载、多存储源（本地/OSS/COS/Kodo/S3/FTP）、分片上传 |
| `sparkit-payment` | 支付订单、多支付渠道（微信/支付宝/PayPal/Apple/Google）、退款、对账 |
| `sparkit-notification` | 短信/邮件/公众号/UniPush/站内信 多通道通知 |
| `sparkit-ai` | 多模型供应商（DeepSeek/百炼/小米）、策略模式切换、流式输出 |
| `sparkit-news` | 新闻分类/文章/评论、AI采集、SEO |
| `sparkit-job` | Quartz 定时任务管理 |
| `sparkit-generator` | 代码生成器 |
| `sparkit-backup` | 数据库备份 |
| `sparkit-search` | Elasticsearch 全文搜索 |

### 3.3 包结构约定

每个业务模块内部统一采用以下包结构：

```
com.sparkit.<module>/
  ├── controller/     # REST 控制器
  ├── mapper/         # MyBatis-Plus Mapper 接口
  ├── model/
  │   └── entity/     # 数据库实体
  ├── service/        # 业务服务接口与实现
  └── strategy/       # 策略模式（如有多实现）
```

### 3.4 配置文件位置

- 主配置：`sparkit-start/src/main/resources/application.yml`
- 数据库连接：`localhost:3306/sparkit`，用户 `root/root`
- Redis：`localhost:6379`，无密码
- 服务端口：`8083`
- 上下文路径：`/`
- API 文档路径：`/doc.html`（Knife4j）
- API 基础路径：`/api/v1`

### 3.5 架构关键约定

**统一响应体：** 所有 Controller 返回 `R<T>` 对象（`com.sparkit.common.model.R`）。`ResponseAdvice` 会自动包装返回值。需要跳过包装的方法加 `@IgnoreResponseAdvice`。

**响应格式：**
```json
{ "code": 200, "msg": "操作成功", "data": {...}, "timestamp": 1718700000000 }
```

**分页：** 使用 `PageQuery` 作为入参，返回 `PageResult<T>`。前端分页参数为 `page` + `pageSize`，上限 100。

**实体基类：** 所有数据库实体继承 `BaseEntity`，包含 `createBy`、`createTime`、`updateBy`、`updateTime`、`remark` 公共字段。逻辑删除字段 `deleted` 由 MyBatis-Plus 自动处理。

**认证鉴权：** 
- JWT 双令牌：AccessToken(15min) + RefreshToken(7d)
- Spring Security + `JwtAuthenticationFilter`（`OncePerRequestFilter`）
- 白名单配置在 `sparkit.security.ignore-urls` 中
- 通过 `IgnoreSecurityConfig` 匹配白名单

**安全配置：** `SecurityConfig` 中 JWT Filter 用 `FilterRegistrationBean` 禁用自动注册，避免重复执行。

### 3.6 启动方式

1. 确保 MySQL 和 Redis 已启动
2. 运行 `sparkit-start` 模块的 `SparkitStartApplication.main()`
3. 访问 `http://localhost:8083/doc.html` 查看 API 文档

---

## 四、前端项目结构 (`sparkit-admin-web/`)

### 4.1 技术选型

- **框架：** Nuxt 3，**SPA 模式** (`ssr: false`)
- **UI 库：** Element Plus
- **状态管理：** Pinia
- **HTTP 客户端：** useFetch（Nuxt 内置）+ 自定义 `useApi` composable
- **图表：** ECharts + vue-echarts
- **CSS 预处理：** Sass

### 4.2 目录结构

```
sparkit-admin-web/
  ├── assets/css/          # 全局样式
  ├── components/
  │   ├── common/          # 通用组件（如 IconSelect）
  │   └── layout/          # 布局组件（Sidebar, HeaderBar, PageTabs, GlobalSearch）
  ├── composables/         # 组合式函数
  │   ├── menuConfig.js    # 侧边栏菜单配置
  │   ├── useApi.js        # API 请求封装
  │   ├── useAuth.js       # 认证状态 (Pinia Store)
  │   └── useTabs.js       # 标签页状态 (Pinia Store)
  ├── layouts/
  │   └── default.vue      # 默认布局（侧边栏 + 顶栏 + 标签页 + 内容区）
  ├── middleware/
  │   └── auth.js          # 认证中间件（非登录页检查 Token）
  ├── pages/               # 页面组件（Nuxt 文件路由）
  │   ├── index.vue        # 仪表盘
  │   ├── login.vue        # 登录页
  │   ├── system/          # 系统管理页面
  │   │   ├── admin/
  │   │   ├── role/
  │   │   ├── menu/
  │   │   ├── dept/
  │   │   ├── post/
  │   │   ├── config/
  │   │   ├── dict/
  │   │   ├── region/
  │   │   ├── i18n/
  │   │   ├── import-export/
  │   │   └── theme/
  │   ├── user/
  │   ├── storage/
  │   ├── payment/
  │   ├── notification/
  │   ├── ai/
  │   ├── news/
  │   ├── job/
  │   ├── generator/
  │   ├── tenant/
  │   ├── backup/
  │   └── monitor/
  │       ├── loginlog/
  │       └── operlog/
  └── app.vue              # 根组件
```

### 4.3 前端架构约定

**API 请求：** 使用 `useApi()` composable，自动处理 Token 注入、401 过期跳转、错误提示。
```js
const api = useApi()
const result = await api.get('/admin/users', { page: 1, pageSize: 10 })
```

**认证流程：**
1. 登录 → `useAuthStore.login()` → 存储 `accessToken` + `refreshToken` 到 Cookie
2. 路由守卫 → `middleware/auth.js` → 无 Token 跳转 `/login`
3. API 请求 → `useApi` 自动携带 `Authorization: Bearer <token>`
4. 401 响应 → 清除 Token，跳转登录页

**菜单与权限：** `menuConfig.js` 定义侧边栏菜单树，每个菜单项有 `perm` 字段用于权限控制。

**标签页：** `useTabsStore` 管理多标签页状态，支持关闭、关闭其他、关闭全部。

**布局：** `layouts/default.vue` 提供经典后台布局（侧边栏 + 顶栏 + 多标签 + 内容）。

### 4.4 开发代理

`nuxt.config.js` 中配置了 `/api` 代理到 `http://localhost:8083`，开发时前端请求自动转发到后端。

### 4.5 构建与部署

```bash
cd sparkit-admin-web
pnpm install
pnpm dev          # 开发模式（默认端口 3000）
pnpm build        # SPA 构建
pnpm generate     # 静态生成（nitro static preset）
```

构建产物输出到 `.output/public/`，可部署到任意静态服务器。

---

## 五、数据库设计要点

- 所有表前缀 `sparkit_`
- 字符集 `utf8mb4`，排序规则 `utf8mb4_general_ci`
- 标准公共字段：`create_by`, `create_time`, `update_by`, `update_time`, `remark`, `deleted`
- 管理员表（`sparkit_admin_user`）与 C 端用户表（`sparkit_user`）分离
- RBAC 权限模型：角色 → 角色菜单关联 → 菜单（D=目录, M=菜单, B=按钮/API）
- 统一配置表 `sparkit_config` 管理所有系统参数
- 完整建表脚本在 `docs/sparkit.sql`

---

## 六、开发规范

### 6.1 Java 后端

| 规范 | 说明 |
|------|------|
| 包名 | `com.sparkit.<模块名>` |
| 实体类 | 继承 `BaseEntity`，使用 Lombok `@Data`，MyBatis-Plus 注解 |
| Mapper | 继承 `BaseMapper<Entity>` |
| Service | 接口 + Impl 实现，继承 `IService<Entity>` / `ServiceImpl` |
| Controller | 返回 `R<T>`，路径 `/api/v1/<模块>/<资源>` |
| 异常 | 业务异常抛 `BusinessException(ErrorCode)` |
| 验证 | 使用 Jakarta Validation 注解，异常由 `GlobalExceptionHandler` 统一处理 |
| 日志 | 使用 `@Slf4j`，关键操作记录到 `sparkit_oper_log` |
| 依赖注入 | 推荐 `@RequiredArgsConstructor` + `final` 字段 |

### 6.2 Vue 前端

| 规范 | 说明 |
|------|------|
| 文件名 | 小写 kebab-case |
| 组件 | `<script setup>` 语法 |
| 状态管理 | Pinia `defineStore` + Composition API |
| 样式 | 优先使用 Element Plus 组件样式，全局样式放 `assets/css/global.css` |
| API 调用 | 始终通过 `useApi()` composable，不要直接使用 `useFetch` |
| 路由 | 利用 Nuxt 文件路由，无需手动配置 |

### 6.3 通用

- 所有新依赖版本统一在父 POM 的 `<properties>` 中定义
- 不要在子模块 POM 中硬编码版本号
- 添加新功能模块时，参考现有模块结构创建

---

## 七、常见问题与注意事项

1. **Knife4j 版本陷阱：** 4.5.0+ 用 Java 21 编译，与当前 Java 17 运行时冲突。只能使用 4.4.0 官方版。
2. **JWT Filter 重复执行：** `JwtAuthenticationFilter` 通过 `FilterRegistrationBean.setEnabled(false)` 禁用自动注册，避免同时作为 Spring Bean 和 Security Filter 生效。
3. **静态资源访问：** `sparkit-start/src/main/resources/static/index.html` 为默认首页，白名单中已放开 `/` 和 `/index.html`。
4. **MyBatis-Plus 逻辑删除：** `deleted` 字段，值 0=未删除, 1=已删除。XML Mapper 中不需要手写 `WHERE deleted = 0`。
5. **跨域：** 前端开发代理已处理跨域，后端仅需在 `SecurityConfig` 中配置 CORS 即可。
6. **Maven 编译：** `maven-compiler-plugin` 统一配置 `source/target=17`，含 Lombok + MapStruct 注解处理器路径。
