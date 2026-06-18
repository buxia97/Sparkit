#!/bin/bash
# ============================================
# Sparkit - 启动服务 (Linux)
# 用法: ./start.sh [profile] [jvm_opts]
# 示例: ./start.sh              # 默认 dev 环境
#       ./start.sh prod         # 生产环境
#       ./start.sh prod "-Xms512m -Xmx1024m"  # 自定义 JVM 参数
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"
load_config

# 参数
PROFILE="${1:-${DEFAULT_PROFILE}}"
JVM_OPTS="${2:-${DEFAULT_JVM_OPTS}}"

print_title "启动服务"
print_var "项目目录" "${PROJECT_DIR}"
print_var "启动环境" "${PROFILE}"
print_var "JVM 参数" "${JVM_OPTS}"
echo -e "${SEP_LINE}"

# Step 1: 检查是否已运行
log_step "检查服务状态"
if is_running; then
    PID=$(get_pid)
    log_warn "服务已在运行中 (PID: ${PID})"
    echo -e "  ${YELLOW}如需重启请执行: ./restart.sh${NC}"
    echo -e "  ${YELLOW}如需查看状态请执行: ./status.sh${NC}"
    exit 0
fi
log_info "当前无运行中的服务"

# Step 2: 检查端口
log_step "检查端口 ${APP_PORT}"
if ! check_port; then
    log_error "端口 ${APP_PORT} 已被占用！"
    echo -e "  ${YELLOW}占用端口的进程信息：${NC}"
    lsof -i :"${APP_PORT}" 2>/dev/null
    echo ""
    echo -e "  ${YELLOW}解决方法：${NC}"
    echo "    1. 停止占用端口的程序"
    echo "    2. 或修改 config.sh 中的 APP_PORT 为其他端口"
    exit 1
fi
log_success "端口 ${APP_PORT} 可用"

# Step 3: 检查 JAR 包
log_step "检查 JAR 包"
if [ ! -f "${JAR_PATH}" ]; then
    log_error "JAR 包不存在: ${JAR_PATH}"
    echo ""
    echo -e "  ${YELLOW}解决方法：${NC}"
    echo "    1. 先编译项目：cd ${PROJECT_DIR} && mvn clean install -DskipTests"
    echo "    2. 或者执行一键部署：./deploy.sh"
    echo "    3. 如果 JAR 包在其他路径，请修改 config.sh 中的 JAR_NAME"
    exit 1
fi
log_success "JAR 包找到: ${JAR_PATH}"

# Step 4: 检查 Java
log_step "检查 Java 环境"
if [ -z "${JAVA_HOME}" ]; then
    log_error "JAVA_HOME 环境变量未设置"
    echo -e "  ${YELLOW}解决方法：${NC}"
    echo "    echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc"
    echo "    echo 'export PATH=\$JAVA_HOME/bin:\$PATH' >> ~/.bashrc"
    echo "    source ~/.bashrc"
    exit 1
fi

JAVA_EXE="${JAVA_HOME}/bin/java"
if [ ! -x "${JAVA_EXE}" ]; then
    log_error "Java 可执行文件未找到: ${JAVA_EXE}"
    exit 1
fi

JAVA_VERSION=$("${JAVA_EXE}" -version 2>&1 | head -1)
log_info "Java 版本: ${JAVA_VERSION}"

# Step 5: 创建日志目录
ensure_dir "${LOG_DIR}"

# Step 6: 启动服务
log_step "启动服务"

echo -e "  ${CYAN}启动命令:${NC}"
echo "    nohup ${JAVA_EXE} ${JVM_OPTS} \\"
echo "      -Dspring.profiles.active=${PROFILE} \\"
echo "      -jar ${JAR_PATH} \\"
echo "      > ${LOG_DIR}/sparkit.log 2>&1 &"
echo ""

nohup "${JAVA_EXE}" ${JVM_OPTS} \
    -Dspring.profiles.active="${PROFILE}" \
    -jar "${JAR_PATH}" \
    > "${LOG_DIR}/sparkit.log" 2>&1 &

PID=$!
echo "${PID}" > "${PID_FILE}"
log_info "进程已启动，PID: ${PID}"

# Step 7: 等待启动完成
log_step "等待服务就绪（最长等待 60 秒）"

RETRY=0
MAX_RETRY=30
while [ ${RETRY} -lt ${MAX_RETRY} ]; do
    sleep 2
    if lsof -Pi :"${APP_PORT}" -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo ""
        log_success "服务启动成功！"
        echo ""
        echo -e "  ${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        echo -e "  ${BOLD}  访问地址:${NC}  http://localhost:${APP_PORT}"
        echo -e "  ${BOLD}  API 文档:${NC}  http://localhost:${APP_PORT}/doc.html"
        echo -e "  ${BOLD}  日志文件:${NC}  ${LOG_DIR}/sparkit.log"
        echo -e "  ${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
        exit 0
    fi
    ((RETRY++))
done

# 超时
log_error "服务启动超时（已等待 60 秒）"
log_info "请查看日志排查问题："
echo -e "  ${CYAN}tail -50 ${LOG_DIR}/sparkit.log${NC}"
echo -e "  ${CYAN}./log-monitor.sh ERROR${NC}"
echo ""
echo -e "${YELLOW}最后 20 行日志：${NC}"
tail -20 "${LOG_DIR}/sparkit.log"
exit 1
