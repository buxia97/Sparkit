#!/bin/bash
# ============================================
# Sparkit - 查看服务状态 (Linux)
# 用法: ./status.sh
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"
load_config

print_title "服务状态"

# Step 1: 获取 PID
PID=$(get_pid)

if [ -z "${PID}" ]; then
    echo ""
    echo -e "  ${YELLOW}┌──────────────────────────────────────────┐${NC}"
    echo -e "  ${YELLOW}│  服务状态: 未运行                          │${NC}"
    echo -e "  ${YELLOW}└──────────────────────────────────────────┘${NC}"
    echo ""
    echo -e "  ${CYAN}启动服务:${NC} ./start.sh"
    echo -e "  ${CYAN}部署项目:${NC} ./deploy.sh"
    exit 0
fi

# Step 2: 服务运行中，显示详细信息
echo ""
echo -e "  ${GREEN}┌──────────────────────────────────────────┐${NC}"
echo -e "  ${GREEN}│  服务状态: 运行中 ✓                        │${NC}"
echo -e "  ${GREEN}└──────────────────────────────────────────┘${NC}"
echo ""

print_var "进程 PID" "${PID}"

# 进程基本信息
if command -v ps &>/dev/null; then
    echo ""
    echo -e "${BOLD}  进程信息:${NC}"
    ps -p "${PID}" -o pid,ppid,user,%cpu,%mem,rss,vsz,etime,args --no-headers 2>/dev/null | while read -r line; do
        # 将 RSS 从 KB 转换为 MB
        rss_kb=$(echo "${line}" | awk '{print $4}')
        rss_mb=$(echo "scale=1; ${rss_kb} / 1024" | bc 2>/dev/null || echo "${rss_kb}KB")
        echo -e "    PID: $(echo "${line}" | awk '{print $1}')"
        echo -e "    父PID: $(echo "${line}" | awk '{print $2}')"
        echo -e "    运行用户: $(echo "${line}" | awk '{print $3}')"
        echo -e "    CPU占用: $(echo "${line}" | awk '{print $4}')%"
        echo -e "    内存占用: ${rss_mb} MB"
        echo -e "    已运行: $(echo "${line}" | awk '{print $8}')"
    done
fi

# PID 文件信息
echo ""
echo -e "${BOLD}  PID 文件:${NC}"
if [ -f "${PID_FILE}" ]; then
    echo -e "    ${GREEN}✓${NC} ${PID_FILE}"
else
    echo -e "    ${YELLOW}✗${NC} ${PID_FILE}（不存在，但不影响运行）"
fi

# 端口信息
echo ""
echo -e "${BOLD}  端口信息:${NC}"
if command -v lsof &>/dev/null && lsof -Pi :"${APP_PORT}" -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "    ${GREEN}✓${NC} 端口 ${APP_PORT} 正在监听"
    echo ""
    echo -e "    ${CYAN}访问地址:${NC}  http://localhost:${APP_PORT}"
    echo -e "    ${CYAN}API 文档:${NC}  http://localhost:${APP_PORT}/doc.html"
else
    echo -e "    ${YELLOW}✗${NC} 端口 ${APP_PORT} 未监听"
fi

# 日志文件信息
echo ""
echo -e "${BOLD}  日志文件:${NC}"
if [ -f "${LOG_DIR}/sparkit.log" ]; then
    LOG_SIZE=$(du -h "${LOG_DIR}/sparkit.log" 2>/dev/null | cut -f1)
    LOG_LINES=$(wc -l < "${LOG_DIR}/sparkit.log" 2>/dev/null)
    echo -e "    ${GREEN}✓${NC} ${LOG_DIR}/sparkit.log"
    echo -e "    大小: ${LOG_SIZE}, 行数: ${LOG_LINES}"
else
    echo -e "    ${YELLOW}✗${NC} 日志文件不存在"
fi

# 最近日志
echo ""
echo -e "${BOLD}  最近日志（最后 5 行）:${NC}"
if [ -f "${LOG_DIR}/sparkit.log" ]; then
    echo -e "  ${SEP_DASH}"
    tail -5 "${LOG_DIR}/sparkit.log" | while read -r line; do
        echo -e "  ${line}"
    done
    echo -e "  ${SEP_DASH}"
fi

echo ""
echo -e "  ${CYAN}常用操作:${NC}"
echo -e "    ./restart.sh    重启服务"
echo -e "    ./stop.sh       停止服务"
echo -e "    ./log-monitor.sh 实时查看日志"
echo -e "    ./health-check.sh 健康检查"
