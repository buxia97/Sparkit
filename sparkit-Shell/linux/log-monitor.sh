#!/bin/bash
# ============================================
# Sparkit - 日志实时监控 (Linux)
# 作用：实时查看应用日志，支持关键字过滤
# 用法: ./log-monitor.sh [keyword]
# 示例: ./log-monitor.sh              # 查看全部日志
#       ./log-monitor.sh ERROR        # 只显示错误日志
#       ./log-monitor.sh "登录失败"   # 搜索指定关键词
#       ./log-monitor.sh "Exception"  # 查看异常信息
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"
load_config

print_title "日志实时监控"
print_var "日志文件" "${LOG_DIR}/sparkit.log"

KEYWORD="$1"

if [ -n "${KEYWORD}" ]; then
    print_var "过滤关键字" "${KEYWORD}"
fi
echo -e "${SEP_LINE}"

# 检查日志文件是否存在
if [ ! -f "${LOG_DIR}/sparkit.log" ]; then
    log_error "日志文件不存在: ${LOG_DIR}/sparkit.log"
    echo -e "  ${YELLOW}可能的原因：${NC}"
    echo "    1. 服务尚未启动（请先执行 ./start.sh）"
    echo "    2. 日志路径不正确（请检查 config.sh 中的 LOG_DIR）"
    exit 1
fi

# 显示日志文件基本信息
LOG_SIZE=$(du -h "${LOG_DIR}/sparkit.log" 2>/dev/null | cut -f1)
LOG_LINES=$(wc -l < "${LOG_DIR}/sparkit.log" 2>/dev/null)
log_info "日志大小: ${LOG_SIZE}, 总行数: ${LOG_LINES}"
echo ""

echo -e "  ${YELLOW}💡 按 Ctrl+C 退出监控${NC}"
echo ""

# 进入实时监控
if [ -n "${KEYWORD}" ]; then
    # 带关键字过滤
    echo -e "${CYAN}━━━ 仅显示包含 \"${KEYWORD}\" 的日志行 ━━━${NC}"
    echo ""
    tail -f "${LOG_DIR}/sparkit.log" | grep --color=always -i "${KEYWORD}"
else
    # 全部日志
    echo -e "${CYAN}━━━ 实时日志输出 ━━━${NC}"
    echo ""
    tail -f "${LOG_DIR}/sparkit.log"
fi
