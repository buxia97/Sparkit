# Sparkit 部署与管理脚本

> 📌 **如果你不懂 Linux 命令也没关系，照着下面的步骤做就行了。**

## 📋 目录结构

```
Sparkit-Shell/
├── README.md                 ← 📖 使用说明（就是本文件）
├── linux/                    ← 🐧 Linux 系统用的脚本
│   ├── config.sh             ← ⚙️ 配置文件（只需改这里）
│   ├── lib/
│   │   └── common.sh         ← 📦 公共函数库（不用管它）
│   ├── deploy.sh             ← 🚀 一键部署（最常用）
│   ├── start.sh              -- ▶️ 启动服务
│   ├── stop.sh               -- ⏹️ 停止服务
│   ├── restart.sh            -- 🔄 重启服务
│   ├── status.sh             -- 📊 查看服务状态
│   ├── health-check.sh       -- 🏥 健康检查
│   ├── log-monitor.sh        -- 📝 实时查看日志
│   ├── jvm-monitor.sh        -- 💻 JVM 性能监控
│   ├── backup-db.sh          -- 💾 备份数据库
│   ├── restore-db.sh         -- ↩️ 恢复数据库
│   └── watch.sh              -- 👀 守护进程（自动重启）
└── windows/                  -- 🪟 Windows 系统用的脚本
    ├── start.bat
    ├── stop.bat
    ├── restart.bat
    ├── status.bat
    ├── backup-db.bat
    ├── restore-db.bat
    ├── health-check.bat
    ├── log-monitor.bat
    ├── jvm-monitor.bat
    └── watch.bat
```

---

## 🚀 快速开始

### 第一步：下载项目代码

```bash
# 如果还没有下载项目代码，先克隆到服务器
# 如果已经下载了，跳过这一步
git clone https://github.com/你的用户名/你的项目.git

# 进入项目目录
cd sparkit-server

# 确认你的项目路径，后面会用到
# 比如：/home/ubuntu/sparkit-server
pwd
```

### 第二步：进入脚本目录

```bash
cd Sparkit-Shell/linux
```

### 第三步：一键部署（最简单的方式）

```bash
# 啥都不用想，直接运行就行
./deploy.sh
```

> **💡 第一次运行会自动进入配置向导**，按提示输入项目路径和 Git 仓库地址即可。

**更高级的用法：**

```bash
./deploy.sh                            # 默认 dev 环境 + main 分支
./deploy.sh prod                       # 部署生产环境
./deploy.sh prod main                  # 部署生产环境 main 分支
./deploy.sh test dev                   # 部署测试环境 dev 分支
```

---

## 📖 各脚本详细说明

### 1️⃣ 一键部署 `deploy.sh`（最常用）

**作用**：自动完成以下 7 个步骤：
1. 检查环境（Java、Maven、Git 是否安装）
2. 拉取最新代码（支持多备用 Git 地址）
3. 停止旧服务
4. Maven 编译打包
5. 备份旧版本
6. 备份数据库
7. 启动新服务

**示例：**

```bash
# 新手最爱——什么都不加，全自动
./deploy.sh

# 老手专用——指定环境
./deploy.sh prod main
```

**关于 Git 多备用地址（重点！）：**

考虑到国内访问 GitHub 可能不稳定，脚本支持配置多个 Git 仓库地址：

```bash
# 编辑 config.sh，找到 GIT_REPOS 部分
# 可以配置 GitHub、Gitee、GitLab 等多个地址
GIT_REPOS=(
    "https://gitee.com/你的用户名/sparkit-server.git"    # 码云（国内快）
    "https://github.com/你的用户名/sparkit-server.git"    # GitHub（国外）
    "git@gitlab.com:你的用户名/sparkit-server.git"        # GitLab（备用）
)
```

部署时脚本会**从上到下依次尝试**，哪个能用就用哪个。

---

### 2️⃣ 启动服务 `start.sh`

```bash
# 启动（默认 dev 环境）
./start.sh

# 启动生产环境，并分配更多内存
./start.sh prod "-Xms512m -Xmx1024m"
```

---

### 3️⃣ 停止服务 `stop.sh`

```bash
# 优雅停止服务
./stop.sh
```

脚本会先发 SIGTERM 信号让程序自己退出，等待 15 秒如果没反应，再强制终止。

---

### 4️⃣ 重启服务 `restart.sh`

```bash
# 重启（相当于 stop + start）
./restart.sh

# 重启到生产环境
./restart.sh prod
```

---

### 5️⃣ 查看服务状态 `status.sh`

```bash
./status.sh
```

会显示：
- 服务是否运行
- PID、CPU 占用、内存占用
- 端口监听状态
- 日志文件信息
- 最近几条日志

---

### 6️⃣ 健康检查 `health-check.sh`

```bash
# 检查服务是否正常
./health-check.sh
```

---

### 7️⃣ 实时查看日志 `log-monitor.sh`

```bash
# 查看全部实时日志
./log-monitor.sh

# 只看错误日志
./log-monitor.sh ERROR

# 搜索关键词
./log-monitor.sh "登录失败"
./log-monitor.sh Exception
```

> 按 `Ctrl+C` 退出日志查看。

---

### 8️⃣ JVM 性能监控 `jvm-monitor.sh`

```bash
# 查看 JVM 内存、GC、线程等信息
./jvm-monitor.sh
```

---

### 9️⃣ 数据库备份 `backup-db.sh`

```bash
# 备份当前数据库
./backup-db.sh
```

自动压缩保存，保留 30 天，超期自动清理。

---

### 🔟 数据库恢复 `restore-db.sh`

```bash
# 交互式选择备份文件恢复
./restore-db.sh

# 直接指定文件恢复
./restore-db.sh /path/to/backup.sql.gz
```

> ⚠️ 恢复会覆盖现有数据，操作前会再次确认。

---

### 1️⃣1️⃣ 守护进程 `watch.sh`

```bash
# 启动守护进程，服务挂了自动重启
./watch.sh

# 每10秒检查一次，最多重试20次
./watch.sh 10 20
```

配合 systemd 可以实现开机自启。

---

## ⚙️ 配置文件说明

所有配置都集中在 `config.sh` 中，你只需要修改这一个文件：

```bash
# ---------- 必须修改的配置 ----------
GIT_REPOS=(
    "https://gitee.com/你的项目.git"       # 改成你的仓库地址
)

# ---------- 按需修改的配置 ----------
PROJECT_DIR="/home/ubuntu/sparkit-server"  # 项目路径（一般不用改）
APP_PORT=8083                               # 服务端口
DEFAULT_PROFILE="dev"                       # 默认环境

# ---------- 数据库配置（备份/恢复用） ----------
DB_HOST="localhost"
DB_PORT=3306
DB_NAME="sparkit"
DB_USER="root"
DB_PASS="root"                               # 改成你的数据库密码
```

---

## 🛠️ 常见问题

### Q: 提示 "Permission denied"
```bash
# 给脚本添加执行权限
chmod +x *.sh lib/*.sh
```

### Q: 提示 "mvn: command not found"
```bash
# 安装 Maven
sudo apt update
sudo apt install maven
```

### Q: 提示 "JAVA_HOME is not set"
```bash
# 找到 Java 安装路径
which java
# 输出类似 /usr/bin/java

# 查看实际路径
ls -la /usr/bin/java
# 可能指向 /usr/lib/jvm/java-17-openjdk-amd64/bin/java

# 设置 JAVA_HOME（添加到 ~/.bashrc 中永久生效）
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### Q: 提示 "端口已被占用"
```bash
# 查看谁占用了端口
sudo lsof -i :8083

# 或者改个端口（编辑 config.sh 中的 APP_PORT）
```

### Q: GitHub 连接不上，拉不了代码
```bash
# 在 config.sh 中添加 Gitee（码云）的备用地址
GIT_REPOS=(
    "https://gitee.com/你的项目.git"       # 国内用 Gitee
    "https://github.com/你的项目.git"       # 国外用 GitHub
)
```

### Q: 数据库备份/恢复报错
```bash
# 检查 config.sh 中的数据库配置是否正确
# 确认数据库服务是否在运行
systemctl status mysql
```

---

## 📝 使用小贴士

- **💡 第一次使用**直接运行 `./deploy.sh`，会进入中文配置向导
- **💡 所有脚本**都带颜色提示：绿色=成功，红色=错误，黄色=警告，蓝色=信息
- **💡 如果遇到问题**先看日志：`./log-monitor.sh ERROR`
- **💡 升级部署**直接再跑一次 `./deploy.sh` 就行，会自动拉取最新代码
