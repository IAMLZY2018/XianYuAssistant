#!/bin/bash
#
# XianYuAssistant 一键安装脚本
# 使用方法: curl -fsSL https://raw.githubusercontent.com/IAMLZY2018/XianYuAssistant/master/install.sh | bash
#

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 配置
INSTALL_DIR="${HOME}/xianyu-assistant"
PORT="${PORT:-12400}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"
JDK_VERSION="21"

# GitHub API
GITHUB_API="https://api.github.com/repos/IAMLZY2018/XianYuAssistant/releases/latest"
GITEE_API="https://gitee.com/api/v5/repos/lzy2018cn/xian-yu-assistant/releases/latest"

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════╗"
echo "║         XianYuAssistant 一键安装脚本                  ║"
echo "╚════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# 检测操作系统
detect_os() {
    # 检测 Termux (Android)
    if [ -n "$TERMUX_VERSION" ] || [ -d "/data/data/com.termux" ]; then
        OS="termux"
    elif [ -f /etc/os-release ]; then
        . /etc/os-release
        OS=$ID
    elif [ "$(uname)" = "Darwin" ]; then
        OS="macos"
    else
        OS="unknown"
    fi
}

# 检查 JDK
check_jdk() {
    if command -v java &> /dev/null; then
        JAVA_VER=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | awk -F '.' '{print $1}')
        if [ "$JAVA_VER" = "1" ]; then
            JAVA_VER=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | awk -F '.' '{print $2}')
        fi
        return 0
    fi
    return 1
}

# 安装 JDK 21
install_jdk() {
    echo ""
    echo -e "${YELLOW}需要安装 JDK ${JDK_VERSION}${NC}"
    echo -e "${YELLOW}输入 y 确认安装，其他键取消${NC}"
    read -p "请选择: " confirm
    
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        echo -e "${RED}已取消安装${NC}"
        exit 1
    fi
    
    echo -e "${YELLOW}正在安装 JDK ${JDK_VERSION}...${NC}"
    
    case $OS in
        ubuntu|debian)
            sudo apt-get update -qq
            sudo apt-get install -y openjdk-${JDK_VERSION}-jdk
            ;;
        centos|rhel|rocky|almalinux)
            sudo yum install -y java-${JDK_VERSION}-openjdk java-${JDK_VERSION}-openjdk-devel
            ;;
        fedora)
            sudo dnf install -y java-${JDK_VERSION}-openjdk java-${JDK_VERSION}-openjdk-devel
            ;;
        macos)
            if command -v brew &> /dev/null; then
                brew install openjdk@${JDK_VERSION}
            else
                echo -e "${RED}请先安装 Homebrew: https://brew.sh${NC}"
                exit 1
            fi
            ;;
        arch|manjaro)
            sudo pacman -S --noconfirm jdk-openjdk
            ;;
        termux)
            pkg update
            pkg install openjdk-21
            ;;
        *)
            echo -e "${RED}不支持的系统，请手动安装 JDK ${JDK_VERSION}${NC}"
            exit 1
            ;;
    esac
}

# 选择下载源
select_source() {
    echo ""
    echo -e "${YELLOW}请选择下载源:${NC}"
    echo -e "  ${GREEN}1${NC}. Gitee (国内推荐)"
    echo -e "  ${GREEN}2${NC}. GitHub"
    echo ""
    read -p "请输入选择 [1/2, 默认1]: " source_choice
    
    case "$source_choice" in
        2)
            SOURCE="github"
            echo -e "${GREEN}✓ 已选择 GitHub${NC}"
            ;;
        *)
            SOURCE="gitee"
            echo -e "${GREEN}✓ 已选择 Gitee${NC}"
            ;;
    esac
}

# 获取最新版本下载链接
get_latest_release() {
    echo ""
    echo -e "${YELLOW}正在获取最新版本信息...${NC}"
    
    if [ "$SOURCE" = "github" ]; then
        API_URL="$GITHUB_API"
    else
        API_URL="$GITEE_API"
    fi
    
    RELEASE_INFO=$(curl -fsSL --connect-timeout 10 "$API_URL" 2>/dev/null || echo "")
    
    if [ -z "$RELEASE_INFO" ]; then
        if [ "$SOURCE" = "gitee" ]; then
            echo -e "${YELLOW}Gitee API 失败，尝试 GitHub...${NC}"
            SOURCE="github"
            RELEASE_INFO=$(curl -fsSL --connect-timeout 10 "$GITHUB_API" 2>/dev/null || echo "")
        else
            echo -e "${YELLOW}GitHub API 失败，尝试 Gitee...${NC}"
            SOURCE="gitee"
            RELEASE_INFO=$(curl -fsSL --connect-timeout 10 "$GITEE_API" 2>/dev/null || echo "")
        fi
    fi
    
    if [ -z "$RELEASE_INFO" ]; then
        echo -e "${RED}无法获取版本信息，请检查网络${NC}"
        exit 1
    fi
    
    if [ "$SOURCE" = "github" ]; then
        LATEST_VERSION=$(echo "$RELEASE_INFO" | grep -m 1 '"tag_name"' | sed -E 's/.*"([^"]+)".*/\1/')
        JAR_URL=$(echo "$RELEASE_INFO" | grep -m 1 'browser_download_url.*\.jar"' | sed -E 's/.*"([^"]+\.jar)".*/\1/')
    else
        LATEST_VERSION=$(echo "$RELEASE_INFO" | grep -m 1 '"tag_name"' | sed -E 's/.*"([^"]+)".*/\1/')
        JAR_URL=$(echo "$RELEASE_INFO" | grep -m 1 'browser_download_url.*\.jar"' | sed -E 's/.*"([^"]+\.jar)".*/\1/')
    fi
    
    if [ -z "$JAR_URL" ]; then
        echo -e "${RED}未找到 JAR 下载链接${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ 最新版本: ${LATEST_VERSION}${NC}"
    echo -e "${GREEN}✓ 下载地址: ${JAR_URL}${NC}"
}

# 下载 JAR 包
download_jar() {
    mkdir -p "$INSTALL_DIR"
    cd "$INSTALL_DIR"
    
    echo ""
    echo -e "${YELLOW}正在下载 JAR 包...${NC}"
    
    if curl -fSL --connect-timeout 30 -L "$JAR_URL" -o xianyu-assistant.jar; then
        echo -e "${GREEN}✓ 下载成功${NC}"
    else
        echo -e "${RED}下载失败，请检查网络${NC}"
        exit 1
    fi
}

# 启动服务
start_service() {
    cd "$INSTALL_DIR"
    mkdir -p data logs
    
    # 停止旧进程
    if [ -f xianyu.pid ]; then
        kill "$(cat xianyu.pid)" 2>/dev/null || true
        rm -f xianyu.pid
    fi
    
    echo ""
    echo -e "${YELLOW}正在启动服务...${NC}"
    
    nohup java $JAVA_OPTS -Dserver.port=$PORT -jar xianyu-assistant.jar > logs/console.log 2>&1 &
    echo $! > xianyu.pid
    
    # 等待启动
    for i in {1..20}; do
        sleep 1
        if curl -s "http://localhost:$PORT" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ 启动成功${NC}"
            return 0
        fi
    done
    
    echo -e "${RED}启动超时，查看日志: tail -f $INSTALL_DIR/logs/console.log${NC}"
    exit 1
}

# 主流程
main() {
    # 检测系统
    detect_os
    
    # 检查 JDK
    echo -e "${BLUE}检查 JDK 环境...${NC}"
    if check_jdk; then
        if [ "$JAVA_VER" -ge "$JDK_VERSION" ]; then
            echo -e "${GREEN}✓ JDK $JAVA_VER 已安装${NC}"
        else
            echo -e "${YELLOW}当前 JDK $JAVA_VER，需要版本 $JDK_VERSION${NC}"
            install_jdk
        fi
    else
        install_jdk
    fi
    
    # 选择下载源
    select_source
    
    # 获取最新版本
    get_latest_release
    
    # 下载并启动
    download_jar
    start_service
    
    # 获取 IP
    LOCAL_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || ipconfig getifaddr en0 2>/dev/null || echo "127.0.0.1")
    
    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║          安装成功！                   ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BLUE}版本信息:${NC} ${LATEST_VERSION}"
    echo ""
    echo -e "${BLUE}访问地址:${NC}"
    echo -e "  本地:   ${GREEN}http://localhost:${PORT}${NC}"
    echo -e "  局域网: ${GREEN}http://${LOCAL_IP}:${PORT}${NC}"
    echo ""
    echo -e "${BLUE}管理命令:${NC}"
    echo -e "  日志: tail -f $INSTALL_DIR/logs/console.log"
    echo -e "  停止: kill \$(cat $INSTALL_DIR/xianyu.pid)"
    echo ""
}

main "$@"
