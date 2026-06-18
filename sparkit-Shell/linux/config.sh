#!/bin/bash
# ============================================
# Sparkit 全局配置中心
# 所有脚本从这里读取配置，只需修改此文件即可
# ============================================

# ---------- 项目路径 ----------
# 项目根目录（sparkit-server 的位置）
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../sparkit-server" && pwd 2>/dev/null)"
# 如果自动获取失败，可以手动设置（删除下方 # 并填写实际路径）：
# PROJECT_DIR="/home/yourname/sparkit-server"

# ---------- JAR 包配置 ----------
JAR_NAME="sparkit-start-1.0.0-SNAPSHOT.jar"
JAR_DIR="${PROJECT_DIR}/sparkit-start/target"
JAR_PATH="${JAR_DIR}/${JAR_NAME}"

# ---------- 服务端口 ----------
APP_PORT=8083

# ---------- 日志目录 ----------
LOG_DIR="${PROJECT_DIR}/logs"
PID_FILE="${LOG_DIR}/sparkit.pid"

# ---------- 备份目录 ----------
BACKUP_DIR="${PROJECT_DIR}/backup"

# ---------- 默认环境 ----------
# 可选值: dev / test / prod
DEFAULT_PROFILE="dev"

# ---------- JVM 参数 ----------
# 可根据服务器配置调整
DEFAULT_JVM_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_DIR}/"

# ---------- Git 配置（部署用） ----------
# 支持配置多个 Git 仓库地址，部署时会按顺序尝试，直到成功拉取代码
# 主要解决国内 GitHub 可能无法访问的问题，配置 Gitee/GitLab 备用
GIT_REPOS=(
    # 格式: "仓库地址 分支名"
    # 如果分支名留空，则使用 deploy.sh 参数指定的分支

    # 示例（请修改为你的实际仓库地址）：
    # "https://gitee.com/yourname/sparkit-server.git"
    # "https://github.com/yourname/sparkit-server.git"
    # "git@gitlab.com:yourname/sparkit-server.git"
    ""
)
# 说明：数组中的每个元素都是一个仓库地址。
# 部署时脚本会从上到下依次尝试，哪个能用就用哪个。

# ---------- 数据库配置（备份/恢复用） ----------
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-sparkit}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-root}"

# ---------- 备份保留天数 ----------
BACKUP_RETENTION_DAYS=30

# ---------- 健康检查 URL ----------
HEALTH_URL="http://localhost:${APP_PORT}/actuator/health"

# ---------- 守护进程配置 ----------
WATCH_INTERVAL=5     # 轮询间隔（秒）
WATCH_MAX_RETRY=10   # 最大重试次数

# ---------- 服务停止等待超时（秒） ----------
STOP_TIMEOUT=15
