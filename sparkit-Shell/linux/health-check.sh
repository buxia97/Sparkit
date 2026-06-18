#!/bin/bash
# ============================================
# Sparkit - 健康检查 (Linux)
# 作用：调用 Actuator 健康检查端点，确认服务是否正常
# 用法: ./health-check.sh
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"
load_config

print_title "健康检查"
print_var "检查地址" "${HEALTH_URL}"
echo -e "${SEP_LINE}"

# 首先检查服务是否运行
if ! is_running; then
    log_error "服务未运行！"
    echo -e "  ${YELLOW}请先启动服务: ./start.sh${NC}"
    exit 1
fi

# 检查 curl 或 wget
HAS_CURL=false
HAS_WGET=false
command -v curl &>/dev/null && HAS_CURL=true
command -v wget &>/dev/null && HAS_WGET=true

if [ "${HAS_CURL}" = false ] && [ "${HAS_WGET}" = false ]; then
    log_error "curl 或 wget 未安装，无法执行健康检查"
    echo -e "  ${YELLOW}安装命令: sudo apt install curl${NC}"
    exit 1
fi

# 执行健康检查
echo ""
log_step "正在检测服务健康状态..."

if [ "${HAS_CURL}" = true ]; then
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 "${HEALTH_URL}" 2>/dev/null)
    RESPONSE_BODY=$(curl -s --connect-timeout 5 "${HEALTH_URL}" 2>/dev/null)

    if [ "${HTTP_CODE}" = "200" ]; then
        echo ""
        log_success "健康检查通过！"
        echo ""
        echo -e "${BOLD}  响应内容：${NC}"
        echo "${RESPONSE_BODY}" | python3 -m json.tool 2>/dev/null || echo "${RESPONSE_BODY}"
    else
        log_error "健康检查失败 (HTTP ${HTTP_CODE})"
        echo ""
        echo -e "  ${YELLOW}可能的原因：${NC}"
        echo "    1. 服务正在启动中，请稍后再试"
        echo "    2. Actuator 端点未配置"
        echo "    3. 服务异常，请查看日志：tail -50 ${LOG_DIR}/sparkit.log"
        exit 1
    fi
else
    # 使用 wget
    if wget -q --timeout=5 -O - "${HEALTH_URL}" >/dev/null 2>&1; then
        log_success "健康检查通过！"
        echo ""
        echo -e "${BOLD}  响应内容：${NC}"
        wget -q --timeout=5 -O - "${HEALTH_URL}"
    else
        log_error "健康检查失败"
        echo -e "  ${YELLOW}请查看日志: tail -50 ${LOG_DIR}/sparkit.log${NC}"
        exit 1
    fi
fi

# 额外信息
echo ""
echo -e "  ${CYAN}其他端点：${NC}"
echo "    http://localhost:${APP_PORT}/actuator/info"
echo "    http://localhost:${APP_PORT}/actuator/metrics"
echo "    http://localhost:${APP_PORT}/actuator/env"
