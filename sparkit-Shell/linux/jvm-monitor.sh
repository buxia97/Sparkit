#!/bin/bash
# ============================================
# Sparkit - JVM 性能监控 (Linux)
# 作用：查看 JVM 堆内存、GC 情况、线程状态等指标
# 用法: ./jvm-monitor.sh
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"
load_config

print_title "JVM 性能监控"

# Step 1: 获取 PID
PID=$(get_pid)

if [ -z "${PID}" ]; then
    log_error "未找到运行中的 Sparkit 服务"
    echo -e "  ${YELLOW}请先启动服务: ./start.sh${NC}"
    exit 1
fi

log_info "服务 PID: ${PID}"
echo ""

# Step 2: 检查 JDK 工具
JPS="${JAVA_HOME}/bin/jps"
JSTAT="${JAVA_HOME}/bin/jstat"
JINFO="${JAVA_HOME}/bin/jinfo"
JSTACK="${JAVA_HOME}/bin/jstack"

JDK_TOOLS_MISSING=false
for tool in "${JSTAT}" "${JSTACK}"; do
    if [ ! -x "${tool}" ]; then
        JDK_TOOLS_MISSING=true
    fi
done

if [ "${JDK_TOOLS_MISSING}" = true ]; then
    log_warn "部分 JDK 工具不可用（jstat/jstack 等）"
    echo -e "  ${YELLOW}原因可能是使用的 JRE 而非完整 JDK${NC}"
    echo -e "  ${YELLOW}需安装完整 JDK 才能查看 JVM 详细信息${NC}"
    echo ""
fi

# [1] JVM 进程基本信息
echo -e "${PURPLE}━━━ [1] JVM 进程信息 ━━━${NC}"
if [ -x "${JPS}" ]; then
    "${JPS}" -l -v 2>/dev/null | grep -E "(^${PID}|sparkit)" || echo "  jps 未找到 sparkit 进程"
else
    echo "  jps 工具不可用"
fi
echo ""

# [2] 堆内存使用情况
echo -e "${PURPLE}━━━ [2] 堆内存使用情况 ━━━${NC}"
if [ -x "${JSTAT}" ]; then
    echo -e "  ${CYAN}堆内存概览 (S0/S1/Eden/Old/Metaspace):${NC}"
    "${JSTAT}" -gc "${PID}" 2>/dev/null | head -2
    echo ""
    echo -e "  ${CYAN}GC 统计:${NC}"
    "${JSTAT}" -gcutil "${PID}" 2>/dev/null | head -2
    echo ""
    echo -e "  ${CYAN}内存池容量 (MB):${NC}"
    "${JSTAT}" -gccapacity "${PID}" 2>/dev/null | head -2 | awk '{
        if(NR>1){
            printf "    NGCMN=%.1f NGCMX=%.1f NGC=%.1f\n", $1/1024, $2/1024, $3/1024
            printf "    OGCMN=%.1f OGCMX=%.1f OGC=%.1f\n", $7/1024, $8/1024, $9/1024
            printf "    MCMN=%.1f MCMX=%.1f MC=%.1f\n", $13/1024, $14/1024, $15/1024
        }
    }'
else
    echo "  jstat 工具不可用"
fi
echo ""

# [3] JVM 配置信息
echo -e "${PURPLE}━━━ [3] JVM 配置信息 ━━━${NC}"
if [ -x "${JINFO}" ]; then
    "${JINFO}" -sysprops "${PID}" 2>/dev/null | grep -E "java\.(vm\.name|version|runtime\.name|specification\.version)" | while read -r line; do
        echo "  ${line}"
    done
else
    echo "  jinfo 工具不可用"
fi
echo ""

# [4] 线程统计
echo -e "${PURPLE}━━━ [4] 线程统计 ━━━${NC}"
if [ -x "${JSTACK}" ]; then
    THREAD_COUNT=$("${JSTACK}" "${PID}" 2>/dev/null | grep -c "^Thread " || echo 0)
    echo -e "  线程总数: ${BOLD}${THREAD_COUNT}${NC}"
    echo ""
    echo -e "  ${CYAN}线程状态分布:${NC}"
    "${JSTACK}" "${PID}" 2>/dev/null | grep "java.lang.Thread.State:" | awk '{print $2}' | sort | uniq -c | sort -rn | while read -r count state; do
        case "${state}" in
            RUNNABLE)    echo -e "    ${GREEN}${count}${NC} ${state}" ;;
            BLOCKED)     echo -e "    ${RED}${count}${NC} ${state}" ;;
            WAITING)     echo -e "    ${YELLOW}${count}${NC} ${state}" ;;
            TIMED_WAITING) echo -e "    ${YELLOW}${count}${NC} ${state}" ;;
            *)           echo -e "    ${count} ${state}" ;;
        esac
    done
else
    echo "  jstack 工具不可用"
fi
echo ""

# [5] 系统资源占用
echo -e "${PURPLE}━━━ [5] 系统资源占用 ━━━${NC}"
if command -v ps &>/dev/null; then
    ps -p "${PID}" -o pid,ppid,user,%cpu,%mem,rss,vsz,etime,args --no-headers 2>/dev/null | while read -r line; do
        rss_kb=$(echo "${line}" | awk '{print $5}')
        rss_mb=$(echo "scale=1; ${rss_kb} / 1024" | bc 2>/dev/null || echo "${rss_kb}KB")
        vsz_kb=$(echo "${line}" | awk '{print $6}')
        vsz_mb=$(echo "scale=1; ${vsz_kb} / 1024" | bc 2>/dev/null || echo "${vsz_kb}KB")
        echo -e "    CPU 使用率: $(echo "${line}" | awk '{print $4}')%"
        echo -e "    物理内存: ${rss_mb} MB"
        echo -e "    虚拟内存: ${vsz_mb} MB"
        echo -e "    运行时间: $(echo "${line}" | awk '{print $8}')"
    done
fi
echo ""

# 其他监控端点
echo -e "  ${CYAN}也可通过 Actuator 端点查看：${NC}"
echo "    http://localhost:${APP_PORT}/actuator/health"
echo "    http://localhost:${APP_PORT}/actuator/metrics"
echo "    http://localhost:${APP_PORT}/actuator/metrics/jvm.memory.used"
echo "    http://localhost:${APP_PORT}/actuator/metrics/jvm.gc.pause"
echo ""
log_info "监控完成"
