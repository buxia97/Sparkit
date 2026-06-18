#!/bin/bash
# ============================================
# Sparkit - 停止服务 (Linux)
# 作用：先尝试优雅停止 (SIGTERM)，超时后强制终止 (SIGKILL)
# 用法: ./stop.sh
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"
load_config

print_title "停止服务"

# Step 1: 查找进程 PID
PID=$(get_pid)

if [ -z "${PID}" ]; then
    log_info "未找到运行中的 Sparkit 服务"
    echo -e "  ${YELLOW}可能的原因：${NC}"
    echo "    1. 服务尚未启动"
    echo "    2. 服务已经停止"
    echo "    3. PID 文件过期（可执行 ./status.sh 确认）"
    exit 0
fi

log_info "找到服务进程，PID: ${PID}"
print_var "进程详情" "$(ps -p "${PID}" -o pid,ppid,user,etime,args --no-headers 2>/dev/null)"

# Step 2: 优雅停止
echo ""
log_step "正在优雅停止服务（发送 SIGTERM 信号）"

kill "${PID}" 2>/dev/null

# 等待进程退出（最多 STOP_TIMEOUT 秒）
WAIT=0
while kill -0 "${PID}" 2>/dev/null && [ ${WAIT} -lt ${STOP_TIMEOUT} ]; do
    sleep 1
    echo -n "."
    ((WAIT++))
done
echo ""

if kill -0 "${PID}" 2>/dev/null; then
    # Step 3: 如果优雅停止超时，强制终止
    log_warn "优雅停止超时（已等待 ${STOP_TIMEOUT} 秒），执行强制终止..."
    kill -9 "${PID}" 2>/dev/null
    sleep 2

    if kill -0 "${PID}" 2>/dev/null; then
        log_error "强制终止失败！PID: ${PID}"
        echo -e "  ${YELLOW}请手动处理：kill -9 ${PID}${NC}"
        exit 1
    else
        log_success "服务已强制终止 (PID: ${PID})"
    fi
else
    log_success "服务已优雅停止 (PID: ${PID})"
fi

# Step 4: 清理 PID 文件
rm -f "${PID_FILE}"
log_info "已清理 PID 文件: ${PID_FILE}"

echo ""
echo -e "  ${GREEN}┌──────────────────────────────────────────┐${NC}"
echo -e "  ${GREEN}│  服务已停止                               │${NC}"
echo -e "  ${GREEN}└──────────────────────────────────────────┘${NC}"
