#!/bin/bash
# ============================================
# Sparkit - 数据库备份 (Linux)
# 作用：执行 MySQL 全量备份，压缩保存，自动清理旧备份
# 用法: ./backup-db.sh
# 前置条件：需要安装 MySQL 客户端
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"
load_config

# 备份文件名
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
BACKUP_FILE="${BACKUP_DIR}/sparkit_backup_${TIMESTAMP}.sql"
ZIP_FILE="${BACKUP_FILE}.gz"

print_title "数据库备份"
print_var "数据库" "${DB_NAME}"
print_var "主机" "${DB_HOST}:${DB_PORT}"
print_var "备份目录" "${BACKUP_DIR}"
print_var "备份文件" "$(basename "${ZIP_FILE}")"
echo -e "${SEP_LINE}"

# Step 1: 检查 mysqldump
log_step "1/3 检查环境"
if ! check_command "mysqldump" "MySQL 客户端 (mysqldump)"; then
    echo ""
    echo -e "  ${YELLOW}安装 MySQL 客户端：${NC}"
    echo "    Ubuntu/Debian: sudo apt install mysql-client"
    echo "    CentOS/RHEL:   sudo yum install mysql"
    echo "    macOS:         brew install mysql-client"
    exit 1
fi
log_success "mysqldump 可用"

# Step 2: 创建备份目录
ensure_dir "${BACKUP_DIR}"

# Step 3: 执行备份
log_step "2/3 正在备份数据库"

echo -e "  ${CYAN}备份命令:${NC}"
echo "    mysqldump -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p****** \\"
echo "      --single-transaction --routines --triggers --events \\"
echo "      --hex-blob --skip-lock-tables ${DB_NAME}"
echo ""

# 执行备份（密码通过 mysql_config_editor 或环境变量传入，避免明文显示）
# 方案 A：使用 MYSQL_PWD 环境变量（简单但不够安全）
# 方案 B：使用 mysql_config_editor（推荐）
if [ -n "${DB_PASS}" ] && [ "${DB_PASS}" != "root" ]; then
    # 检查是否已经配置了 login-path
    if mysql_config_editor print --login-path=sparkit &>/dev/null; then
        mysqldump \
            --login-path=sparkit \
            --default-character-set=utf8mb4 \
            --single-transaction \
            --routines \
            --triggers \
            --events \
            --hex-blob \
            --skip-lock-tables \
            "${DB_NAME}" > "${BACKUP_FILE}" 2>"${LOG_DIR}/backup_error.log"
    else
        # 使用环境变量方式
        MYSQL_PWD="${DB_PASS}" mysqldump \
            -h"${DB_HOST}" \
            -P"${DB_PORT}" \
            -u"${DB_USER}" \
            --default-character-set=utf8mb4 \
            --single-transaction \
            --routines \
            --triggers \
            --events \
            --hex-blob \
            --skip-lock-tables \
            "${DB_NAME}" > "${BACKUP_FILE}" 2>"${LOG_DIR}/backup_error.log"
    fi
else
    # 默认方式（提示输入密码）
    log_info "正在连接数据库，请输入密码..."
    mysqldump \
        -h"${DB_HOST}" \
        -P"${DB_PORT}" \
        -u"${DB_USER}" \
        -p \
        --default-character-set=utf8mb4 \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        --hex-blob \
        --skip-lock-tables \
        "${DB_NAME}" > "${BACKUP_FILE}" 2>"${LOG_DIR}/backup_error.log"
fi

if [ $? -ne 0 ]; then
    log_error "数据库备份失败！"
    echo -e "  ${YELLOW}错误详情：${NC}"
    cat "${LOG_DIR}/backup_error.log"
    echo ""
    echo -e "  ${YELLOW}可能的原因：${NC}"
    echo "    1. 数据库连接信息错误（检查 config.sh 中的 DB_* 配置）"
    echo "    2. 数据库未启动"
    echo "    3. 用户权限不足"
    exit 1
fi

BACKUP_SIZE=$(du -h "${BACKUP_FILE}" 2>/dev/null | cut -f1)
log_success "备份完成: ${BACKUP_FILE} (${BACKUP_SIZE})"

# 压缩备份
log_step "3/3 压缩归档"
gzip -f "${BACKUP_FILE}"
ZIP_SIZE=$(du -h "${ZIP_FILE}" 2>/dev/null | cut -f1)
log_success "已压缩: ${ZIP_FILE} (${ZIP_SIZE})"

# 清理旧备份
log_info "清理 ${BACKUP_RETENTION_DAYS} 天前的旧备份..."
OLD_FILES=$(find "${BACKUP_DIR}" -name "sparkit_backup_*.sql.gz" -mtime +"${BACKUP_RETENTION_DAYS}" 2>/dev/null)
if [ -n "${OLD_FILES}" ]; then
    echo "${OLD_FILES}" | while read -r f; do
        rm -f "${f}"
        log_info "已删除旧备份: $(basename "${f}")"
    done
fi

echo ""
log_success "数据库备份全部完成！"
echo -e "  ${CYAN}备份文件:${NC} ${ZIP_FILE} (${ZIP_SIZE})"
echo -e "  ${CYAN}恢复命令:${NC} ./restore-db.sh"
