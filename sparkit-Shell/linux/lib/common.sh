#!/bin/bash
# ============================================
# Sparkit 公共函数库
# 提供颜色输出、日志打印、通用检查等函数
# ============================================

# ---------- 颜色定义 ----------
# 使用方式: echo -e "${GREEN}成功信息${NC}"
RED='\033[0;31m'          # 红色 - 错误
GREEN='\033[0;32m'        # 绿色 - 成功
YELLOW='\033[1;33m'       # 黄色 - 警告
BLUE='\033[0;34m'         # 蓝色 - 信息
PURPLE='\033[0;35m'       # 紫色 - 步骤标题
CYAN='\033[0;36m'         # 青色 - 提示
WHITE='\033[1;37m'        # 白色 - 强调
BOLD='\033[1m'            # 加粗
NC='\033[0m'              # 重置颜色

# ---------- 分隔线 ----------
SEP_LINE="${CYAN}============================================${NC}"
SEP_DASH="${CYAN}--------------------------------------------${NC}"

# ---------- 日志函数 ----------

# 打印信息消息
# 用法: log_info "消息内容"
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

# 打印成功消息
# 用法: log_success "消息内容"
log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# 打印警告消息
# 用法: log_warn "消息内容"
log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# 打印错误消息
# 用法: log_error "消息内容"
log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 打印步骤标题
# 用法: log_step "步骤描述"
log_step() {
    echo ""
    echo -e "${PURPLE}[STEP]${NC} $1"
    echo -e "${SEP_DASH}"
}

# 打印标题（带边框的大标题）
# 用法: print_title "标题内容"
print_title() {
    echo ""
    echo -e "${SEP_LINE}"
    echo -e "${CYAN} Sparkit - $1${NC}"
    echo -e "${SEP_LINE}"
}

# 打印变量值
# 用法: print_var "变量名" "变量值"
print_var() {
    echo -e "  ${BOLD}$1${NC}: $2"
}

# ---------- 通用检查函数 ----------

# 检查命令是否存在
# 用法: check_command "mvn" "Maven 3.8+"
check_command() {
    local cmd=$1
    local name=$2
    if ! command -v "${cmd}" &>/dev/null; then
        log_error "${name} 未安装，请先安装 ${name}"
        echo -e "  ${YELLOW}安装命令示例：${NC}"
        case "${cmd}" in
            mvn)    echo "    Ubuntu/Debian: sudo apt install maven" ;;
            java)   echo "    请先安装 JDK 17+" ;;
            mysql)  echo "    Ubuntu/Debian: sudo apt install mysql-client" ;;
            mysqldump) echo "    Ubuntu/Debian: sudo apt install mysql-client" ;;
            curl)   echo "    Ubuntu/Debian: sudo apt install curl" ;;
            lsof)   echo "    Ubuntu/Debian: sudo apt install lsof" ;;
            *)      echo "    请使用系统包管理器安装" ;;
        esac
        return 1
    fi
    return 0
}

# 检查目录是否存在，不存在则创建
# 用法: ensure_dir "/path/to/dir"
ensure_dir() {
    local dir=$1
    if [ ! -d "${dir}" ]; then
        mkdir -p "${dir}"
        log_info "已创建目录: ${dir}"
    fi
}

# 检查文件是否存在
# 用法: check_file "\$JAR_PATH" "JAR 包"
check_file() {
    local file=$1
    local name=$2
    if [ ! -f "${file}" ]; then
        log_error "${name} 文件不存在: ${file}"
        return 1
    fi
    return 0
}

# 安全获取 PID（读取 PID 文件或通过端口查找）
# 用法: get_pid
# 返回: PID 值，如果找不到则返回空
get_pid() {
    local pid=""

    # 方式1：读取 PID 文件
    if [ -f "${PID_FILE}" ]; then
        pid=$(cat "${PID_FILE}")
        if [ -n "${pid}" ] && kill -0 "${pid}" 2>/dev/null; then
            echo "${pid}"
            return 0
        fi
    fi

    # 方式2：通过端口查找
    if command -v lsof &>/dev/null; then
        pid=$(lsof -Pi :"${APP_PORT}" -sTCP:LISTEN -t 2>/dev/null)
        if [ -n "${pid}" ]; then
            echo "${pid}"
            return 0
        fi
    fi

    # 方式3：通过进程名查找
    pid=$(ps aux | grep "${JAR_NAME}" | grep -v grep | awk '{print $2}' 2>/dev/null)
    if [ -n "${pid}" ]; then
        echo "${pid}"
        return 0
    fi

    return 1
}

# 检查服务是否运行
# 用法: is_running
# 返回: 0=运行中, 1=未运行
is_running() {
    local pid
    pid=$(get_pid)
    if [ -n "${pid}" ]; then
        return 0
    fi
    return 1
}

# 检查端口是否被占用
# 用法: check_port
# 返回: 0=端口可用, 1=端口被占用
check_port() {
    if command -v lsof &>/dev/null; then
        if lsof -Pi :"${APP_PORT}" -sTCP:LISTEN -t >/dev/null 2>&1; then
            return 1
        fi
    fi
    return 0
}

# ---------- 用户交互函数 ----------

# 询问用户确认
# 用法: confirm_yesno "是否继续？"
# 返回: 0=是, 1=否
confirm_yesno() {
    local prompt=$1
    local default=${2:-n}
    local hint=""
    if [ "${default}" = "y" ]; then
        hint="[Y/n]"
    else
        hint="[y/N]"
    fi
    read -r -p "$(echo -e "${YELLOW}${prompt} ${hint}${NC} ")" answer
    answer=${answer:-${default}}
    case "${answer}" in
        [yY][eE][sS]|[yY]) return 0 ;;
        *) return 1 ;;
    esac
}

# 读取用户输入（带默认值）
# 用法: read_input "请输入内容" "默认值" variable_name
read_input() {
    local prompt=$1
    local default=$2
    local var_name=$3
    read -r -p "$(echo -e "${CYAN}${prompt}${NC} (默认: ${default}): ")" input
    input=${input:-${default}}
    eval "${var_name}='${input}'"
}

# ---------- 加载配置 ----------

# 加载 config.sh（如果存在）
load_config() {
    local script_dir
    script_dir="$(cd "$(dirname "${BASH_SOURCE[1]}")" && pwd)"
    local config_file="${script_dir}/config.sh"

    if [ -f "${config_file}" ]; then
        # shellcheck source=/dev/null
        source "${config_file}"
    else
        # 如果 config.sh 不存在，使用默认值
        log_warn "未找到 config.sh 配置文件，使用默认值"
        PROJECT_DIR="$(cd "${script_dir}/../../sparkit-server" && pwd 2>/dev/null || echo "")"
        JAR_NAME="sparkit-start-1.0.0-SNAPSHOT.jar"
        JAR_DIR="${PROJECT_DIR}/sparkit-start/target"
        JAR_PATH="${JAR_DIR}/${JAR_NAME}"
        APP_PORT=8083
        LOG_DIR="${PROJECT_DIR}/logs"
        PID_FILE="${LOG_DIR}/sparkit.pid"
        BACKUP_DIR="${PROJECT_DIR}/backup"
        DEFAULT_PROFILE="dev"
        DEFAULT_JVM_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
        DB_HOST="${DB_HOST:-localhost}"
        DB_PORT="${DB_PORT:-3306}"
        DB_NAME="${DB_NAME:-sparkit}"
        DB_USER="${DB_USER:-root}"
        DB_PASS="${DB_PASS:-root}"
        HEALTH_URL="http://localhost:${APP_PORT}/actuator/health"
        STOP_TIMEOUT=15
        WATCH_INTERVAL=5
        WATCH_MAX_RETRY=10
    fi
}
