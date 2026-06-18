#!/bin/bash
# ============================================
# Sparkit - 数据库恢复 (Linux)
# 作用：从备份文件恢复数据库
# 用法: ./restore-db.sh [backup_file]
# 示例: ./restore-db.sh                             # 交互式选择
#       ./restore-db.sh sparkit_backup_20260101.sql.gz  # 指定文件
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"
load_config

print_title "数据库恢复"
print_var "数据库" "${DB_NAME}"
print_var "备份目录" "${BACKUP_DIR}"
echo -e "${SEP_LINE}"

# Step 1: 检查 mysql 客户端
log_step "1/4 检查环境"
if ! check_command "mysql" "MySQL 客户端 (mysql)"; then
    exit 1
fi
log_success "mysql 客户端可用"

# Step 2: 确定备份文件
log_step "2/4 选择备份文件"

BACKUP_FILE="$1"

if [ -z "${BACKUP_FILE}" ]; then
    echo -e "  ${CYAN}可用的备份文件：${NC}"
    echo ""

    # 查找备份文件
    FILES=()
    while IFS= read -r -d '' f; do
        FILES+=("$f")
    done < <(find "${BACKUP_DIR}" -name "sparkit_backup_*.sql*" -type f -print0 2>/dev/null | sort -r)

    if [ ${#FILES[@]} -eq 0 ]; then
        log_error "未找到任何备份文件！"
        echo -e "  ${YELLOW}请先执行 ./backup-db.sh 创建备份${NC}"
        exit 1
    fi

    for i in "${!FILES[@]}"; do
        FILENAME=$(basename "${FILES[$i]}")
        SIZE=$(du -h "${FILES[$i]}" 2>/dev/null | cut -f1)
        MTIME=$(stat -c '%y' "${FILES[$i]}" 2>/dev/null | cut -d. -f1)
        echo -e "  ${BOLD}[$((i+1))]${NC} ${FILENAME}"
        echo -e "     大小: ${SIZE}, 备份时间: ${MTIME}"
    done

    echo ""
    read -r -p "$(echo -e "${YELLOW}请选择要恢复的备份文件编号 [1-${#FILES[@]}]${NC}: ")" CHOICE

    if ! [[ "${CHOICE}" =~ ^[0-9]+$ ]] || [ "${CHOICE}" -lt 1 ] || [ "${CHOICE}" -gt "${#FILES[@]}" ]; then
        log_error "无效选择，请输入 1-${#FILES[@]} 之间的数字"
        exit 1
    fi

    BACKUP_FILE="${FILES[$((CHOICE-1))]}"
fi

if [ ! -f "${BACKUP_FILE}" ]; then
    log_error "备份文件不存在: ${BACKUP_FILE}"
    exit 1
fi

FILENAME=$(basename "${BACKUP_FILE}")
FILESIZE=$(du -h "${BACKUP_FILE}" 2>/dev/null | cut -f1)
log_info "选择的备份文件: ${FILENAME} (${FILESIZE})"

# Step 3: 危险操作确认
log_step "3/4 确认恢复操作"

echo ""
echo -e "  ${RED}┌──────────────────────────────────────────┐${NC}"
echo -e "  ${RED}│  ⚠ 危险操作：恢复将覆盖当前数据库数据！    │${NC}"
echo -e "  ${RED}└──────────────────────────────────────────┘${NC}"
echo ""
echo -e "  ${YELLOW}即将执行以下操作：${NC}"
echo -e "    · 数据库: ${BOLD}${DB_NAME}${NC} @ ${DB_HOST}:${DB_PORT}"
echo -e "    · 备份文件: ${BOLD}${FILENAME}${NC}"
echo -e "    · 操作: ${RED}覆盖全部数据${NC}"
echo ""

if ! confirm_yesno "确认恢复？请输入 yes 继续" ""; then
    log_info "已取消恢复操作"
    exit 0
fi

# Step 4: 执行恢复
log_step "4/4 正在恢复数据库"

# 解压 gz 文件
SQL_FILE="${BACKUP_FILE}"
IS_GZIPPED=false
if [[ "${BACKUP_FILE}" == *.gz ]]; then
    IS_GZIPPED=true
    SQL_FILE="${BACKUP_FILE%.gz}"
    log_info "正在解压备份文件..."
    gunzip -c "${BACKUP_FILE}" > "${SQL_FILE}"
    if [ $? -ne 0 ]; then
        log_error "解压失败！备份文件可能已损坏"
        exit 1
    fi
    log_success "解压完成: $(basename "${SQL_FILE}")"
fi

# 执行恢复
log_info "正在恢复数据库，请稍候..."
echo -e "  ${CYAN}恢复命令: mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p****** ${DB_NAME} < ${SQL_FILE}${NC}"

if [ -n "${DB_PASS}" ] && [ "${DB_PASS}" != "root" ]; then
    MYSQL_PWD="${DB_PASS}" mysql \
        -h"${DB_HOST}" \
        -P"${DB_PORT}" \
        -u"${DB_USER}" \
        --default-character-set=utf8mb4 \
        "${DB_NAME}" < "${SQL_FILE}" 2>"${LOG_DIR}/restore_error.log"
else
    log_info "请输入数据库密码..."
    mysql \
        -h"${DB_HOST}" \
        -P"${DB_PORT}" \
        -u"${DB_USER}" \
        -p \
        --default-character-set=utf8mb4 \
        "${DB_NAME}" < "${SQL_FILE}" 2>"${LOG_DIR}/restore_error.log"
fi

if [ $? -ne 0 ]; then
    log_error "数据库恢复失败！"
    echo -e "  ${YELLOW}错误详情：${NC}"
    cat "${LOG_DIR}/restore_error.log"
    exit 1
fi

# 清理临时文件
if [ "${IS_GZIPPED}" = true ]; then
    rm -f "${SQL_FILE}"
    log_info "已清理临时解压文件"
fi

echo ""
log_success "数据库恢复成功！"
echo ""
echo -e "  ${YELLOW}💡 建议：${NC}"
echo -e "    1. 重启服务以刷新缓存：./restart.sh"
echo -e "    2. 验证数据是否正常：./health-check.sh"
echo -e "    3. 如恢复有误，可选择其他备份文件重新恢复"
