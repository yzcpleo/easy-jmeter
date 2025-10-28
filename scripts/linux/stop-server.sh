#!/bin/bash
#=================================
# Easy JMeter Server 停止脚本 (Linux)
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
echo "   停止 Easy JMeter Server"
echo "========================================"
echo

# 获取脚本所在目录的绝对路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_HOME="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# 设置变量
PID_FILE="${APP_HOME}/server.pid"

# 停止指定PID的进程
stop_process() {
    local pid=$1
    local name=$2
    local timeout=${3:-10}
    
    if ! kill -0 "${pid}" 2>/dev/null; then
        print_info "${name} 进程 ${pid} 不存在"
        return 1
    fi
    
    print_info "正在停止 ${name} (PID: ${pid})..."
    
    # 发送TERM信号进行优雅停止
    kill -TERM "${pid}" 2>/dev/null
    
    # 等待进程结束
    local count=0
    while kill -0 "${pid}" 2>/dev/null && [[ ${count} -lt ${timeout} ]]; do
        sleep 1
        ((count++))
        printf "."
    done
    echo
    
    # 检查进程是否已经停止
    if ! kill -0 "${pid}" 2>/dev/null; then
        print_success "${name} 已优雅停止"
        return 0
    fi
    
    print_warning "${name} 未响应TERM信号，发送KILL信号..."
    kill -KILL "${pid}" 2>/dev/null
    
    # 再次等待
    sleep 2
    if ! kill -0 "${pid}" 2>/dev/null; then
        print_success "${name} 已强制停止"
        return 0
    else
        print_error "无法停止 ${name} 进程 ${pid}"
        return 1
    fi
}

# 通过进程特征查找并停止进程
find_and_stop_process() {
    local pattern="$1"
    local name="$2"
    
    print_info "搜索${name}进程: ${pattern}"
    
    # 查找匹配的进程
    local pids=$(pgrep -f "${pattern}" 2>/dev/null)
    
    if [[ -z "${pids}" ]]; then
        print_info "未找到匹配的${name}进程"
        return 1
    fi
    
    # 停止找到的进程
    local stopped=0
    for pid in ${pids}; do
        # 验证这确实是我们要找的进程
        local cmd=$(ps -p "${pid}" -o cmd --no-headers 2>/dev/null)
        if [[ "${cmd}" == *"${pattern}"* ]]; then
            if stop_process "${pid}" "${name}"; then
                ((stopped++))
            fi
        fi
    done
    
    if [[ ${stopped} -gt 0 ]]; then
        print_success "已停止 ${stopped} 个${name}进程"
        return 0
    else
        return 1
    fi
}

# 主停止逻辑
main_stop() {
    local stopped=false
    
    # 1. 尝试通过PID文件停止
    if [[ -f "${PID_FILE}" ]]; then
        local server_pid=$(cat "${PID_FILE}")
        print_info "从PID文件读取到进程ID: ${server_pid}"
        
        if stop_process "${server_pid}" "Easy JMeter Server"; then
            rm -f "${PID_FILE}"
            stopped=true
        else
            print_warning "PID文件中的进程无法停止，尝试清理PID文件..."
            rm -f "${PID_FILE}"
        fi
    fi
    
    # 2. 如果通过PID文件没有成功停止，尝试通过进程特征查找
    if [[ "${stopped}" != true ]]; then
        print_info "未找到PID文件或PID文件无效，尝试通过进程特征查找..."
        
        # 通过socket.server.enable=true特征查找Server进程
        if find_and_stop_process "socket.server.enable=true" "Easy JMeter Server"; then
            stopped=true
        # 通过JAR文件名查找
        elif find_and_stop_process "easyJmeter.*jar.*socket.server.enable=true" "Easy JMeter Server"; then
            stopped=true
        # 通过更宽泛的特征查找
        elif find_and_stop_process "easyJmeter.*jar" "Easy JMeter Server (通用)"; then
            print_warning "找到Easy JMeter相关进程并已停止，请确认是否为Server进程"
            stopped=true
        fi
    fi
    
    if [[ "${stopped}" == true ]]; then
        print_success "Easy JMeter Server 停止完成"
        
        # 清理相关资源
        print_info "清理临时文件..."
        rm -f "${PID_FILE}"
        
        # 显示停止后的状态
        echo
        print_info "停止后状态检查:"
        local remaining_java=$(pgrep -f "java.*easyJmeter" | wc -l)
        if [[ ${remaining_java} -gt 0 ]]; then
            print_warning "仍有 ${remaining_java} 个相关Java进程在运行"
            print_info "如需查看: ps aux | grep easyJmeter"
        else
            print_success "所有相关进程已停止"
        fi
    else
        print_info "未找到运行中的Easy JMeter Server进程"
        print_info "可能的原因:"
        print_info "  1. Server未启动"
        print_info "  2. 进程已经停止"
        print_info "  3. 进程特征发生变化"
        echo
        print_info "手动检查命令: ps aux | grep easyJmeter"
    fi
}

# 如果提供了--force参数，强制停止所有相关进程
if [[ "$1" == "--force" ]]; then
    print_warning "强制停止模式: 将停止所有Easy JMeter相关进程"
    
    # 查找所有相关进程
    local all_pids=$(pgrep -f "easyJmeter|easy-jmeter" 2>/dev/null)
    
    if [[ -n "${all_pids}" ]]; then
        print_info "找到相关进程: ${all_pids}"
        for pid in ${all_pids}; do
            local cmd=$(ps -p "${pid}" -o cmd --no-headers 2>/dev/null)
            print_info "停止进程 ${pid}: ${cmd}"
            kill -KILL "${pid}" 2>/dev/null
        done
        print_success "所有相关进程已强制停止"
    else
        print_info "未找到相关进程"
    fi
    
    rm -f "${PID_FILE}"
else
    # 正常停止模式
    main_stop
fi

echo
