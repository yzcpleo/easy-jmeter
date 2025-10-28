#!/bin/bash
#=================================
# Easy JMeter Agent 启动脚本 (Linux)
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
echo "   启动 Easy JMeter Agent"
echo "========================================"
echo

# 获取脚本所在目录的绝对路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_HOME="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# 设置变量
JAR_FILE="${APP_HOME}/api/target/easyJmeter-0.1.0-RELEASE.jar"
CONFIG_FILE="${APP_HOME}/application-agent.yml"
LOG_DIR="${APP_HOME}/logs/agent"
PID_FILE="${APP_HOME}/agent.pid"
LOG_FILE="${LOG_DIR}/agent.log"

# 创建日志目录
mkdir -p "${LOG_DIR}"

# 检查Java是否安装
if ! command -v java &> /dev/null; then
    print_error "Java未安装或不在PATH中"
    print_info "请安装Java 8或更高版本"
    exit 1
fi

# 检查JAR文件是否存在
if [[ ! -f "${JAR_FILE}" ]]; then
    print_error "JAR文件不存在: ${JAR_FILE}"
    print_info "请先编译项目: cd api && mvn clean package -DskipTests"
    exit 1
fi

# 检查或创建配置文件
if [[ ! -f "${CONFIG_FILE}" ]]; then
    print_warning "配置文件不存在，创建默认配置: ${CONFIG_FILE}"
    create_default_config "${CONFIG_FILE}"
fi

# 创建默认配置文件的函数
create_default_config() {
    local config_file="$1"
    cat > "${config_file}" << 'EOF'
# Easy JMeter Agent Configuration
server:
  port: 5000

spring:
  application:
    name: easy-jmeter-agent
  profiles:
    active: prod

# SocketIO客户端配置 - 连接到Server
socket:
  client:
    serverUrl: http://localhost:9000  # 请修改为实际的Server地址
    enable: true
  server:
    enable: false

# InfluxDB配置 (可选，用于性能数据存储)
spring:
  influx:
    url: http://localhost:8086
    user: root  
    password: root
    database: easyJmeter

# JMeter配置
jmeter:
  path: /opt/apache-jmeter  # 请修改为实际的JMeter安装路径

logging:
  level:
    io.github.guojiaxing1995.easyJmeter: INFO
  file:
    name: ${LOG_DIR}/agent.log
EOF
    print_info "已创建默认配置文件: ${config_file}"
    print_warning "请根据实际情况修改以下配置项:"
    print_warning "  - socket.client.serverUrl: Server服务器地址"
    print_warning "  - jmeter.path: JMeter安装路径"
    print_warning "  - spring.influx.url: InfluxDB地址"
}

# 检查JMeter是否安装
JMETER_PATH="/opt/apache-jmeter"
if [[ ! -f "${JMETER_PATH}/bin/jmeter" ]]; then
    print_warning "JMeter未找到: ${JMETER_PATH}"
    print_info "常见JMeter安装路径:"
    print_info "  - /opt/apache-jmeter"
    print_info "  - /usr/local/apache-jmeter"
    print_info "  - ~/apache-jmeter"
    
    # 尝试查找JMeter
    POSSIBLE_PATHS=(
        "/usr/local/apache-jmeter"
        "/opt/jmeter"
        "/usr/local/jmeter"
        "${HOME}/apache-jmeter"
        "${HOME}/jmeter"
    )
    
    for path in "${POSSIBLE_PATHS[@]}"; do
        if [[ -f "${path}/bin/jmeter" ]]; then
            JMETER_PATH="${path}"
            print_info "找到JMeter: ${JMETER_PATH}"
            break
        fi
    done
    
    if [[ ! -f "${JMETER_PATH}/bin/jmeter" ]]; then
        print_warning "请确保JMeter已安装并配置正确的路径"
        read -p "请输入JMeter安装路径 (或按Enter跳过): " CUSTOM_PATH
        if [[ -n "${CUSTOM_PATH}" ]]; then
            JMETER_PATH="${CUSTOM_PATH}"
        fi
    fi
fi

# 检查是否已经运行
if [[ -f "${PID_FILE}" ]]; then
    OLD_PID=$(cat "${PID_FILE}")
    if kill -0 "${OLD_PID}" 2>/dev/null; then
        print_warning "Agent已经在运行中 (PID: ${OLD_PID})"
        print_info "如果需要重启，请先运行: ./stop-agent.sh"
        exit 1
    else
        print_info "清理无效的PID文件..."
        rm -f "${PID_FILE}"
    fi
fi

print_info "启动配置:"
print_info "  JAR文件: ${JAR_FILE}"
print_info "  配置文件: ${CONFIG_FILE}"
print_info "  JMeter路径: ${JMETER_PATH}"
print_info "  日志目录: ${LOG_DIR}"
echo

# 检测系统内存并设置JVM参数 (Agent需要更多内存)
TOTAL_MEM=$(free -m | awk 'NR==2{printf "%.0f", $2}')
if [[ ${TOTAL_MEM} -gt 8192 ]]; then
    XMS="4g"
    XMX="6g"
elif [[ ${TOTAL_MEM} -gt 4096 ]]; then
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

# JVM参数设置 (Agent需要更多内存用于JMeter执行)
JVM_OPTS="-server"
JVM_OPTS="${JVM_OPTS} -Xms${XMS} -Xmx${XMX}"
JVM_OPTS="${JVM_OPTS} -XX:+UseG1GC"
JVM_OPTS="${JVM_OPTS} -XX:MaxGCPauseMillis=200"
JVM_OPTS="${JVM_OPTS} -XX:G1HeapRegionSize=16m"
JVM_OPTS="${JVM_OPTS} -XX:+UseStringDeduplication"
JVM_OPTS="${JVM_OPTS} -XX:+HeapDumpOnOutOfMemoryError"
JVM_OPTS="${JVM_OPTS} -XX:HeapDumpPath=${LOG_DIR}/heapdump.hprof"
JVM_OPTS="${JVM_OPTS} -XX:+PrintGCDetails"
JVM_OPTS="${JVM_OPTS} -XX:+PrintGCTimeStamps"
JVM_OPTS="${JVM_OPTS} -Xloggc:${LOG_DIR}/gc.log"

# 应用参数设置
APP_OPTS="-Dfile.encoding=UTF-8"
APP_OPTS="${APP_OPTS} -Dspring.profiles.active=prod"
APP_OPTS="${APP_OPTS} -Dsocket.server.enable=false"
APP_OPTS="${APP_OPTS} -Dsocket.client.enable=true"
APP_OPTS="${APP_OPTS} -Dlogging.file.path=${LOG_DIR}"
APP_OPTS="${APP_OPTS} -Djava.awt.headless=true"
APP_OPTS="${APP_OPTS} -Djmeter.home=${JMETER_PATH}"

print_info "正在启动 Easy JMeter Agent..."
echo

# 启动应用 (后台运行)
nohup java ${JVM_OPTS} ${APP_OPTS} \
    -jar "${JAR_FILE}" \
    --spring.config.location="${CONFIG_FILE}" \
    > "${LOG_FILE}" 2>&1 &

# 获取进程ID
AGENT_PID=$!
echo ${AGENT_PID} > "${PID_FILE}"

# 等待启动
sleep 3

# 检查进程是否还在运行
if kill -0 ${AGENT_PID} 2>/dev/null; then
    print_success "Easy JMeter Agent 启动成功!"
    print_info "  进程ID: ${AGENT_PID}"
    print_info "  连接地址: 根据配置文件中的socket.client.serverUrl"
    print_info "  JMeter路径: ${JMETER_PATH}"
    print_info "  配置文件: ${CONFIG_FILE}"
    print_info "  日志文件: ${LOG_FILE}"
    print_info "  停止命令: ./scripts/linux/stop-agent.sh"
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
