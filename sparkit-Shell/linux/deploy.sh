#!/bin/bash
# ============================================
#  ███████╗██████╗  █████╗ ██████╗ ██╗  ██╗██╗████████╗
#  ██╔════╝██╔══██╗██╔══██╗██╔══██╗██║ ██╔╝██║╚══██╔══╝
#  ███████╗██████╔╝███████║██████╔╝█████╔╝ ██║   ██║   
#  ╚════██║██╔═══╝ ██╔══██║██╔══██╗██╔═██╗ ██║   ██║   
#  ███████║██║     ██║  ██║██║  ██║██║  ██╗██║   ██║   
#  ╚══════╝╚═╝     ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝   ╚═╝   
# ============================================
#  Sparkit - 一键部署脚本 (Linux)
#  作用：自动拉取代码 → 编译打包 → 停止服务 → 备份 → 启动
#  作者：Sparkit Team
# ============================================
#
#  ██ 使用方法 ██████████████████████████████████████████
#
#  最简单（全部默认）：
#     ./deploy.sh
#
#  指定环境和分支：
#     ./deploy.sh prod main        # 部署生产环境 main 分支
#     ./deploy.sh test dev         # 部署测试环境 dev 分支
#     ./deploy.sh dev feature-x    # 部署开发环境 feature-x 分支
#
#  首次使用请先配置 Git 仓库地址：
#     第一次运行脚本时，会进入交互式配置向导
#     你也可以直接编辑 config.sh 文件来配置
#
#  ██ 前置条件 ██████████████████████████████████████████
#
#  ✓ 需要安装 Java 17+（JDK）
#  ✓ 需要安装 Maven 3.8+
#  ✓ 需要安装 Git
#  ✓ 配置好 JAVA_HOME 环境变量
#
# ============================================

# ============ 加载公共库和配置 ============
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/lib/common.sh"
load_config

# ============ 命令行参数 ============
PROFILE="${1:-${DEFAULT_PROFILE}}"
BRANCH="${2:-main}"

# ============ 交互式配置向导（首次使用） ============

# 检查是否需要运行配置向导
run_wizard=false

# 如果没有配置 Git 仓库且第一次运行，启动向导
if [ "${#GIT_REPOS[@]}" -eq 0 ] || [ -z "${GIT_REPOS[0]}" ]; then
    run_wizard=true
fi

# 如果项目目录不存在，也启动向导
if [ ! -d "${PROJECT_DIR}" ]; then
    log_warn "项目目录不存在: ${PROJECT_DIR}"
    run_wizard=true
fi

if [ "${run_wizard}" = true ]; then
    echo ""
    print_title "首次使用配置向导"
    echo -e "${YELLOW}检测到你还没有配置项目信息，让我帮你快速完成配置！${NC}"
    echo -e "${YELLOW}（如果不想使用向导，可以直接按 Ctrl+C 退出，然后手动编辑 config.sh）${NC}"
    echo ""

    # ---------- 第一步：项目路径 ----------
    echo -e "${GREEN}▶ 第一步：项目路径${NC}"
    echo -e "  ${CYAN}请确认 sparkit-server 项目的存放路径。${NC}"
    echo -e "  ${CYAN}例如：/home/ubuntu/sparkit-server${NC}"
    echo ""

    read_input "请输入项目路径" "${PROJECT_DIR}" PROJECT_DIR
    echo ""

    # 检查路径是否存在，如果不存在则询问是否创建
    if [ ! -d "${PROJECT_DIR}" ]; then
        echo -e "${YELLOW}路径 ${PROJECT_DIR} 不存在。${NC}"
        if confirm_yesno "是否创建此目录？" "y"; then
            mkdir -p "${PROJECT_DIR}"
            log_success "已创建目录: ${PROJECT_DIR}"
        else
            log_warn "跳过目录创建，路径将在后续操作中创建"
        fi
    fi

    # ---------- 第二步：Git 仓库地址 ----------
    echo ""
    echo -e "${GREEN}▶ 第二步：配置 Git 仓库地址${NC}"
    echo -e "  ${CYAN}请配置代码仓库地址。支持配置多个备用地址，${NC}"
    echo -e "  ${CYAN}比如 GitHub 无法访问时会自动尝试 Gitee（码云）。${NC}"
    echo -e "  ${CYAN}配置 3 个左右比较合适，太多了会增加等待时间。${NC}"
    echo ""

    GIT_REPOS=()
    idx=1
    while true; do
        echo -e "${BOLD}备用地址 #${idx}${NC}"
        echo -e "  ${CYAN}示例：https://github.com/yourname/sparkit-server.git${NC}"
        echo -e "  ${CYAN}      https://gitee.com/yourname/sparkit-server.git${NC}"
        echo -e "  ${CYAN}      git@gitlab.com:yourname/sparkit-server.git${NC}"
        echo ""

        read_input "Git 仓库地址" "" repo_url
        if [ -n "${repo_url}" ]; then
            GIT_REPOS+=("${repo_url}")
            echo -e "${GREEN}  ✓ 已添加: ${repo_url}${NC}"
            echo ""
            ((idx++))
            if ! confirm_yesno "还要添加备用地址吗？" "n"; then
                break
            fi
            echo ""
        else
            log_warn "地址不能为空，请重新输入"
            if [ ${idx} -gt 1 ]; then
                if confirm_yesno "放弃添加更多地址？" "y"; then
                    break
                fi
            fi
        fi
    done

    if [ ${#GIT_REPOS[@]} -eq 0 ]; then
        log_warn "未配置任何 Git 仓库地址，将跳过代码拉取步骤"
        echo -e "  ${YELLOW}你可以稍后编辑 config.sh 来配置仓库地址${NC}"
    fi

    # ---------- 第三步：确认配置 ----------
    echo ""
    echo -e "${GREEN}▶ 配置完成，请确认以下信息：${NC}"
    echo ""
    echo -e "${BOLD}  项目路径:${NC} ${PROJECT_DIR}"
    echo -e "${BOLD}  Git 仓库:${NC}"
    for url in "${GIT_REPOS[@]}"; do
        echo -e "    · ${url}"
    done
    echo ""

    if ! confirm_yesno "以上信息正确吗？" "y"; then
        log_info "请编辑 config.sh 重新配置"
        exit 0
    fi

    log_success "配置完成！现在开始部署..."
fi

# ============ 开始部署 ============

print_title "一键部署"
print_var "部署环境" "${PROFILE}"
print_var "代码分支" "${BRANCH}"
print_var "项目目录" "${PROJECT_DIR}"
echo -e "${SEP_LINE}"

# ---------- 前置检查 ----------
log_step "1/7 环境检查"

# 检查必要工具
check_command "java" "JDK 17+" || exit 1
check_command "mvn" "Maven 3.8+" || exit 1
check_command "git" "Git" || exit 1

# 检查 JAVA_HOME
if [ -z "${JAVA_HOME}" ]; then
    log_error "JAVA_HOME 环境变量未设置！"
    echo -e "  ${YELLOW}解决方法：${NC}"
    echo "    1. 找到 JDK 安装路径：which java"
    echo "    2. 编辑 /etc/profile 或 ~/.bashrc，添加："
    echo "       export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64"
    echo "       export PATH=\$JAVA_HOME/bin:\$PATH"
    echo "    3. 执行 source ~/.bashrc 刷新"
    exit 1
fi

log_success "环境检查通过"
log_info "Java 版本: $(java -version 2>&1 | head -1)"
log_info "Maven 版本: $(mvn --version 2>&1 | head -1)"
log_info "Git 版本: $(git --version)"

# ---------- 拉取代码（支持多备用地址） ----------
log_step "2/7 拉取最新代码"

if [ ${#GIT_REPOS[@]} -gt 0 ] && [ -n "${GIT_REPOS[0]}" ]; then
    # 确保项目目录存在
    ensure_dir "${PROJECT_DIR}"

    cd "${PROJECT_DIR}" || exit 1

    # 检查是否已经是 Git 仓库
    if [ -d ".git" ]; then
        log_info "已存在 Git 仓库，尝试更新远程地址..."

        # 获取当前远程仓库地址
        current_remote=$(git remote get-url origin 2>/dev/null)

        # 如果当前远程地址不在配置列表中，添加备用远程
        found=false
        for url in "${GIT_REPOS[@]}"; do
            if [ "${current_remote}" = "${url}" ]; then
                found=true
                break
            fi
        done

        if [ "${found}" = false ]; then
            # 添加多个 remote，部署时逐个尝试
            git remote remove origin 2>/dev/null
            git remote add origin "${GIT_REPOS[0]}"
            log_info "已将 origin 设置为: ${GIT_REPOS[0]}"

            # 添加多个备用 remote
            for i in "${!GIT_REPOS[@]}"; do
                if [ ${i} -gt 0 ]; then
                    remote_name="backup_$((i))"
                    git remote remove "${remote_name}" 2>/dev/null
                    git remote add "${remote_name}" "${GIT_REPOS[${i}]}"
                    log_info "已添加备用 remote ${remote_name}: ${GIT_REPOS[${i}]}"
                fi
            done
        fi

        # 按顺序尝试拉取代码
        pull_success=false
        fetch_errors=""

        # 尝试 origin
        log_info "正在从 origin 拉取代码..."
        if git fetch origin 2>/dev/null; then
            git checkout "${BRANCH}" 2>/dev/null || git checkout -b "${BRANCH}" origin/"${BRANCH}" 2>/dev/null || log_warn "切换分支失败，尝试创建分支"
            if git pull origin "${BRANCH}" 2>/dev/null; then
                pull_success=true
                log_success "从 origin 拉取代码成功！"
            fi
        else
            fetch_errors="${fetch_errors}\n    origin 失败"
        fi

        # 如果 origin 失败，尝试备用 remote
        if [ "${pull_success}" = false ]; then
            log_warn "从 origin 拉取失败，尝试备用地址..."
            for i in "${!GIT_REPOS[@]}"; do
                if [ ${i} -gt 0 ]; then
                    remote_name="backup_$((i))"
                    log_info "正在尝试 ${remote_name} (${GIT_REPOS[${i}]})..."
                    if git fetch "${remote_name}" 2>/dev/null; then
                        git checkout "${BRANCH}" 2>/dev/null || true
                        if git pull "${remote_name}" "${BRANCH}" 2>/dev/null; then
                            pull_success=true
                            log_success "从 ${remote_name} 拉取代码成功！（备用地址）"
                            break
                        fi
                    else
                        fetch_errors="${fetch_errors}\n    ${remote_name} 失败"
                    fi
                fi
            done
        fi

        if [ "${pull_success}" = false ]; then
            log_error "所有 Git 仓库地址都无法访问！"
            echo -e "${YELLOW}  请检查：${NC}"
            echo "    1. 网络是否正常：ping github.com"
            echo "    2. 仓库地址是否正确：cat config.sh | grep GIT_REPOS"
            echo "    3. 是否有访问权限：ssh -T git@github.com"
            echo ""
            echo -e "${YELLOW}  已尝试的地址：${NC}${fetch_errors}"
            echo ""
            if ! confirm_yesno "是否跳过拉取代码，使用本地已有代码继续？" "y"; then
                exit 1
            fi
            log_warn "跳过代码拉取，使用本地已有代码"
        fi
    else
        # 不是 Git 仓库，尝试克隆
        log_info "尚未初始化 Git 仓库，正在克隆..."
        clone_success=false

        for url in "${GIT_REPOS[@]}"; do
            log_info "正在尝试克隆: ${url}"
            if git clone "${url}" "${PROJECT_DIR}.tmp" 2>/dev/null; then
                # 将克隆的内容移到项目目录
                if [ -d "${PROJECT_DIR}.tmp" ]; then
                    cp -r "${PROJECT_DIR}.tmp/." "${PROJECT_DIR}/" 2>/dev/null
                    rm -rf "${PROJECT_DIR}.tmp"
                fi
                clone_success=true
                log_success "克隆成功！地址: ${url}"
                break
            else
                log_warn "克隆失败: ${url}"
                rm -rf "${PROJECT_DIR}.tmp" 2>/dev/null
            fi
        done

        if [ "${clone_success}" = false ]; then
            log_error "所有 Git 仓库地址都无法访问，克隆失败！"
            echo -e "  ${YELLOW}请检查网络连接或仓库地址配置${NC}"
            exit 1
        fi

        # 切换到指定分支
        cd "${PROJECT_DIR}" || exit 1
        git checkout "${BRANCH}" 2>/dev/null || git checkout -b "${BRANCH}" 2>/dev/null
    fi
else
    log_warn "未配置 Git 仓库地址，跳过代码拉取"
    echo -e "  ${YELLOW}如需配置，请编辑 config.sh 文件中的 GIT_REPOS 数组${NC}"
    echo -e "  ${YELLOW}或者直接运行本脚本，首次会进入配置向导${NC}"
fi

# ---------- 停止服务 ----------
log_step "3/7 停止旧服务"

if is_running; then
    log_info "检测到服务正在运行，正在停止..."
    if bash "${SCRIPT_DIR}/stop.sh"; then
        log_success "旧服务已停止"
    else
        log_warn "服务停止时出现异常，但继续下一步"
    fi
else
    log_info "服务未运行，跳过停止步骤"
fi

# ---------- 编译打包 ----------
log_step "4/7 编译打包"

cd "${PROJECT_DIR}" || exit 1

# 检查 pom.xml
if [ ! -f "pom.xml" ]; then
    log_error "未找到 pom.xml 文件，请确认项目目录是否正确"
    echo -e "  ${YELLOW}当前目录: ${PROJECT_DIR}${NC}"
    echo -e "  ${YELLOW}目录内容:${NC}"
    ls -la "${PROJECT_DIR}"
    exit 1
fi

log_info "正在编译项目，这可能需要几分钟..."
echo -e "  ${CYAN}编译命令: mvn clean install -DskipTests -P${PROFILE}${NC}"
echo ""

if mvn clean install -DskipTests -P"${PROFILE}"; then
    log_success "编译成功！"
else
    log_error "编译失败！"
    echo ""
    echo -e "${YELLOW}  ⚠ 常见编译错误及解决方法：${NC}"
    echo ""
    echo "  1. Maven 依赖下载失败"
    echo "     · 检查网络连接"
    echo "     · 尝试更换 Maven 镜像源（编辑 ~/.m2/settings.xml）"
    echo ""
    echo "  2. 编译报错"
    echo "     · 检查 Java 版本：java -version（需要 17+）"
    echo "     · 查看详细错误：mvn clean install -P${PROFILE} 2>&1 | tail -50"
    echo ""
    echo "  3. 测试失败"
    echo "     · 编译命令已添加 -DskipTests 跳过测试"
    echo "     · 如果仍然失败，可以尝试：mvn clean install -DskipTests -P${PROFILE} -o"
    exit 1
fi

# ---------- 备份当前版本 ----------
log_step "5/7 备份当前版本"

ensure_dir "${BACKUP_DIR}"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')

if [ -f "${JAR_PATH}" ]; then
    BACKUP_FILE="${BACKUP_DIR}/sparkit-start-${TIMESTAMP}.jar"
    cp "${JAR_PATH}" "${BACKUP_FILE}"
    log_success "已备份 JAR 包: ${BACKUP_FILE}"
else
    log_info "未找到旧的 JAR 包，跳过备份（首次部署正常）"
fi

# 清理过期备份
log_info "清理 ${BACKUP_RETENTION_DAYS} 天前的备份..."
find "${BACKUP_DIR}" -name "sparkit-start-*.jar" -mtime +"${BACKUP_RETENTION_DAYS}" -delete 2>/dev/null

# ---------- 数据库备份 ----------
log_step "6/7 数据库备份"

if [ -f "${SCRIPT_DIR}/backup-db.sh" ]; then
    if bash "${SCRIPT_DIR}/backup-db.sh"; then
        log_success "数据库备份完成"
    else
        log_warn "数据库备份失败（不影响部署，可稍后手动备份）"
    fi
else
    log_info "未找到 backup-db.sh 脚本，跳过数据库备份"
fi

# ---------- 启动服务 ----------
log_step "7/7 启动服务"

if bash "${SCRIPT_DIR}/start.sh" "${PROFILE}"; then
    log_success "服务启动成功！"
else
    log_error "服务启动失败，请查看日志排查问题"
    echo -e "  ${YELLOW}查看日志命令:${NC}"
    echo "    tail -100 ${LOG_DIR}/sparkit.log"
    echo "    ./log-monitor.sh ERROR"
    exit 1
fi

# ============ 部署完成 ============
echo ""
print_title "部署完成"
print_var "部署环境" "${PROFILE}"
print_var "代码分支" "${BRANCH}"
print_var "项目目录" "${PROJECT_DIR}"
print_var "访问地址" "http://localhost:${APP_PORT}"
print_var "API 文档" "http://localhost:${APP_PORT}/doc.html"
print_var "健康检查" "${HEALTH_URL}"
print_var "日志文件" "${LOG_DIR}/sparkit.log"
echo -e "${SEP_LINE}"

echo ""
echo -e "${GREEN}  🎉 Sparkit 部署成功！${NC}"
echo ""
echo -e "  ${CYAN}常用管理命令：${NC}"
echo -e "    ./status.sh        ${BOLD}查看服务状态${NC}"
echo -e "    ./log-monitor.sh   ${BOLD}实时查看日志${NC}"
echo -e "    ./log-monitor.sh ERROR  ${BOLD}查看错误日志${NC}"
echo -e "    ./health-check.sh  ${BOLD}健康检查${NC}"
echo -e "    ./restart.sh       ${BOLD}重启服务${NC}"
echo -e "    ./stop.sh          ${BOLD}停止服务${NC}"
echo ""

# 提示未配置仓库地址（如果跳过的话）
if [ ${#GIT_REPOS[@]} -eq 0 ] || [ -z "${GIT_REPOS[0]}" ]; then
    echo -e "${YELLOW}  💡 提示：你还没有配置 Git 仓库地址。${NC}"
    echo -e "${YELLOW}     下次部署时编辑 config.sh 中的 GIT_REPOS 即可自动拉取代码。${NC}"
    echo ""
fi
