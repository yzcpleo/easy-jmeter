#!/bin/bash
#=================================
# Easy JMeter Server 启动脚本 (Linux)
#=================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

echo
echo "========================================"
echo "   启动 Easy JMeter Server"
echo "========================================"
echo

# 获取脚本所在目录的绝对路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_HOME="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# 设置变量
JAR_FILE="${APP_HOME}/api/target/easyJmeter-0.1.0-RELEASE.jar"
CONFIG_FILE="${APP_HOME}/application-dev.yml"
LOG_DIR="${APP_HOME}/logs/server"
PID_FILE="${APP_HOME}/server.pid"
LOG_FILE="${LOG_DIR}/server.log"

# 创建日志目录
mkdir -p "${LOG_DIR}"

# 检查Java是否安装
if ! command -v java &> /dev/null; then
    print_error "Java未安装或不在PATH中"
    print_info "请安装Java 8或更高版本"
    exit 1
fi

# 检查Java版本
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1$2}')
if [[ ${JAVA_VERSION} -lt 18 ]]; then
    print_warning "检测到Java版本可能过低，推荐使用Java 8或更高版本"
fi

# 检查JAR文件是否存在
if [[ ! -f "${JAR_FILE}" ]]; then
    print_error "JAR文件不存在: ${JAR_FILE}"
    print_info "请先编译项目: cd api && mvn clean package -DskipTests"
    exit 1
fi

# 检查配置文件是否存在
if [[ ! -f "${CONFIG_FILE}" ]]; then
    print_error "配置文件不存在: ${CONFIG_FILE}"
    print_info "请确保配置文件存在"
    exit 1
fi

# 检查是否已经运行
if [[ -f "${PID_FILE}" ]]; then
    OLD_PID=$(cat "${PID_FILE}")
    if kill -0 "${OLD_PID}" 2>/dev/null; then
        print_warning "Server已经在运行中 (PID: ${OLD_PID})"
        print_info "如果需要重启，请先运行: ./stop-server.sh"
        exit 1
    else
        print_info "清理无效的PID文件..."
        rm -f "${PID_FILE}"
    fi
fi

print_info "启动配置:"
print_info "  JAR文件: ${JAR_FILE}"
print_info "  配置文件: ${CONFIG_FILE}"
print_info "  日志目录: ${LOG_DIR}"
print_info "  PID文件: ${PID_FILE}"
echo

# 检测系统内存并设置JVM参数
TOTAL_MEM=$(free -m | awk 'NR==2{printf "%.0f", $2}')
if [[ ${TOTAL_MEM} -gt 4096 ]]; then
    XMS="2g"
    XMX="4g"
elif [[ ${TOTAL_MEM} -gt 2048 ]]; then
    XMS="1g"
    XMX="2g"
else
    XMS="512m"
    XMX="1g"
fi

print_info "系统内存: ${TOTAL_MEM}MB，设置JVM堆内存: ${XMS}-${XMX}"

# JVM参数设置
JVM_OPTS="-server"
JVM_OPTS="${JVM_OPTS} -Xms${XMS} -Xmx${XMX}"
JVM_OPTS="${JVM_OPTS} -XX:+UseG1GC"
JVM_OPTS="${JVM_OPTS} -XX:MaxGCPauseMillis=200"
JVM_OPTS="${JVM_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
JVM_OPTS="${JVM_OPTS} -XX:HeapDumpPath=${LOG_DIR}/heapdump.hprof"
JVM_OPTS="${JVM_OPTS} -XX:+PrintGCDetails"
JVM_OPTS="${JVM_OPTS} -XX:+PrintGCTimeStamps"
JVM_OPTS="${JVM_OPTS} -Xloggc:${LOG_DIR}/gc.log"

# 应用参数设置
APP_OPTS="-Dfile.encoding=UTF-8"
APP_OPTS="${APP_OPTS} -Dspring.profiles.active=dev"
APP_OPTS="${APP_OPTS} -Dsocket.server.enable=true"
APP_OPTS="${APP_OPTS} -Dsocket.client.enable=false"
APP_OPTS="${APP_OPTS} -Dlogging.file.path=${LOG_DIR}"
APP_OPTS="${APP_OPTS} -Djava.awt.headless=true"

print_info "正在启动 Easy JMeter Server..."
echo

# 启动应用 (后台运行)
nohup java ${JVM_OPTS} ${APP_OPTS} \
    -jar "${JAR_FILE}" \
    --spring.config.location="${CONFIG_FILE}" \
    > "${LOG_FILE}" 2>&1 &

# 获取进程ID
SERVER_PID=$!
echo ${SERVER_PID} > "${PID_FILE}"

# 等待启动
sleep 3

# 检查进程是否还在运行
if kill -0 ${SERVER_PID} 2>/dev/null; then
    print_success "Easy JMeter Server 启动成功!"
    print_info "  进程ID: ${SERVER_PID}"
    print_info "  Web地址: http://localhost:5000"
    print_info "  管理界面: http://localhost:3000"
    print_info "  配置文件: ${CONFIG_FILE}"
    print_info "  日志文件: ${LOG_FILE}"
    print_info "  停止命令: ./scripts/linux/stop-server.sh"
    echo
    print_info "查看实时日志: tail -f ${LOG_FILE}"
    echo
    
    # 询问是否查看日志
    read -p "是否查看启动日志? [y/N]: " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "======== 启动日志 (按 Ctrl+C 退出) ========"
        tail -f "${LOG_FILE}"
    fi
else
    print_error "启动失败，进程已退出"
    print_info "请检查日志文件: ${LOG_FILE}"
    if [[ -f "${LOG_FILE}" ]]; then
        echo
        echo "======== 错误日志 ========"
        tail -20 "${LOG_FILE}"
    fi
    rm -f "${PID_FILE}"
    exit 1
fi
