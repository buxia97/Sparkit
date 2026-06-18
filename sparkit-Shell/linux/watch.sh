#!/bin/bash
# ============================================
# Sparkit - 守护监听 (Linux)
# 作用：自动检测服务状态，崩溃后自动重启
#       配合 systemd 可实现开机自启
# 用法: ./watch.sh [interval] [max_retry]
# 示例: ./watch.sh           # 默认间隔5秒，最大重试10次
#       ./watch.sh 10       # 每10秒检查一次
#       ./watch.sh 5 20     # 每5秒检查，最多重试20次
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"
load_config

# 参数
INTERVAL="${1:-${WATCH_INTERVAL}}"
MAX_RETRY="${2:-${WATCH_MAX_RETRY}}"

print_title "守护监听"
print_var "轮询间隔" "${INTERVAL} 秒"
print_var "最大重试" "${MAX_RETRY} 次"
print_var "检测端口" "${APP_PORT}"
echo -e "${SEP_LINE}"

echo -e "  ${YELLOW}💡 守护进程已启动，按 Ctrl+C 退出${NC}"
echo -e "  ${YELLOW}💡 配合 systemd 可实现开机自启${NC}"
echo ""

RETRY_COUNT=0
TOTAL_RESTARTS=0
START_TIME=$(date +%s)

# 信号处理：优雅退出
cleanup() {
    END_TIME=$(date +%s)
    RUNTIME=$((END_TIME - START_TIME))
    echo ""
    echo -e "  ${SEP_LINE}"
    echo -e "  ${PURPLE}守护进程运行统计${NC}"
    print_var "运行时间" "$(date -u -d @${RUNTIME} +'%H:%M:%S')"
    print_var "自动重启次数" "${TOTAL_RESTARTS}"
    echo -e "  ${SEP_LINE}"
    echo ""
    log_info "守护进程退出"
    exit 0
}
trap cleanup SIGINT SIGTERM

# 主循环
while true; do
    CURRENT_TIME=$(date '+%Y-%m-%d %H:%M:%S')

    if is_running; then
        # 服务运行正常，重置重试计数
        if [ ${RETRY_COUNT} -gt 0 ]; then
            echo -e "[${CURRENT_TIME}] ${GREEN}服务已恢复运行${NC}"
        fi
        RETRY_COUNT=0
        sleep "${INTERVAL}"
        continue
    fi

    # 服务未运行，尝试重启
    ((RETRY_COUNT++))
    echo -e "[${CURRENT_TIME}] ${YELLOW}[WARN] 服务未运行！正在尝试自动重启 (第 ${RETRY_COUNT}/${MAX_RETRY} 次)${NC}"

    if [ ${RETRY_COUNT} -gt ${MAX_RETRY} ]; then
        echo -e "[${CURRENT_TIME}] ${RED}[ERROR] 已达到最大重试次数 ${MAX_RETRY}，守护进程退出${NC}"
        echo -e "[${CURRENT_TIME}] ${RED}[ALERT] 请检查服务状态并手动排查问题${NC}"
        echo ""
        log_info "查看日志: tail -100 ${LOG_DIR}/sparkit.log"
        log_info "手动启动: ./start.sh"
        exit 1
    fi

    # 执行启动
    echo -e "[${CURRENT_TIME}] ${CYAN}[INFO] 正在执行自动重启...${NC}"
    if bash "${SCRIPT_DIR}/start.sh" 2>/dev/null; then
        ((TOTAL_RESTARTS++))
        echo -e "[${CURRENT_TIME}] ${GREEN}[SUCCESS] 服务已自动拉起 (累计重启 ${TOTAL_RESTARTS} 次)${NC}"
        RETRY_COUNT=0
    else
        echo -e "[${CURRENT_TIME}] ${RED}[ERROR] 自动重启失败，将在 ${INTERVAL} 秒后重试${NC}"
    fi

    sleep "${INTERVAL}"
done
