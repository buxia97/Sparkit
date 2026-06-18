#!/bin/bash
# ============================================
# Sparkit - 重启服务 (Linux)
# 用法: ./restart.sh [profile]
# 示例: ./restart.sh          # 重启（默认环境）
#       ./restart.sh prod     # 重启生产环境
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"
load_config

print_title "重启服务"
print_var "环境" "${1:-${DEFAULT_PROFILE}}"
echo -e "${SEP_LINE}"

# Step 1: 停止服务
log_step "1/2 停止旧服务"
if is_running; then
    if bash "${SCRIPT_DIR}/stop.sh"; then
        log_success "旧服务已停止"
    else
        log_warn "停止服务时出现异常，但继续启动"
    fi
else
    log_info "服务未运行，跳过停止步骤"
fi

# 等待 2 秒确保端口释放
echo ""
log_info "等待端口释放..."
sleep 2

# Step 2: 启动服务
log_step "2/2 启动新服务"
if bash "${SCRIPT_DIR}/start.sh" "$@"; then
    log_success "重启完成！"
else
    log_error "重启失败，请检查日志"
    exit 1
fi
