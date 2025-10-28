#!/bin/bash
#=================================
# Easy JMeter 快速启动脚本 (Linux)
#=================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 获取脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 清屏函数
clear_screen() {
    clear
}

# 打印菜单
show_menu() {
    clear_screen
    echo -e "${CYAN}========================================"
    echo -e "   Easy JMeter 快速启动菜单"
    echo -e "========================================${NC}"
    echo
    echo -e "${BLUE}请选择要执行的操作:${NC}"
    echo
    echo -e "   ${GREEN}1.${NC} 启动 Server (管理端)"
    echo -e "   ${GREEN}2.${NC} 启动 Agent (压力机)"
    echo -e "   ${YELLOW}3.${NC} 停止 Server"
    echo -e "   ${YELLOW}4.${NC} 停止 Agent"
    echo -e "   ${BLUE}5.${NC} 查看运行状态"
    echo -e "   ${BLUE}6.${NC} 查看日志"
    echo -e "   ${BLUE}7.${NC} 系统信息"
    echo -e "   ${RED}8.${NC} 退出"
    echo
}

# 检查进程状态
check_status() {
    echo -e "${BLUE}==================== 运行状态 ====================${NC}"
    echo
    
    # 检查Server
    local server_pid_file="${SCRIPT_DIR}/../server.pid"
    echo -e "${CYAN}Server状态:${NC}"
    if [[ -f "${server_pid_file}" ]]; then
        local server_pid=$(cat "${server_pid_file}")
        if kill -0 "${server_pid}" 2>/dev/null; then
            echo -e "  ${GREEN}✓ 运行中${NC} (PID: ${server_pid})"
            echo -e "  ${BLUE}  Web地址: http://localhost:5000${NC}"
            echo -e "  ${BLUE}  管理界面: http://localhost:3000${NC}"
        else
            echo -e "  ${RED}✗ 已停止${NC} (PID文件存在但进程不运行)"
        fi
    else
        # 尝试通过进程查找
        local server_pids=$(pgrep -f "socket.server.enable=true" 2>/dev/null)
        if [[ -n "${server_pids}" ]]; then
            echo -e "  ${YELLOW}? 可能运行中${NC} (PID: ${server_pids})"
        else
            echo -e "  ${RED}✗ 未运行${NC}"
        fi
    fi
    
    echo
    
    # 检查Agent
    local agent_pid_file="${SCRIPT_DIR}/../agent.pid"
    echo -e "${CYAN}Agent状态:${NC}"
    if [[ -f "${agent_pid_file}" ]]; then
        local agent_pid=$(cat "${agent_pid_file}")
        if kill -0 "${agent_pid}" 2>/dev/null; then
            echo -e "  ${GREEN}✓ 运行中${NC} (PID: ${agent_pid})"
        else
            echo -e "  ${RED}✗ 已停止${NC} (PID文件存在但进程不运行)"
        fi
    else
        # 尝试通过进程查找
        local agent_pids=$(pgrep -f "socket.client.enable=true" 2>/dev/null)
        if [[ -n "${agent_pids}" ]]; then
            echo -e "  ${YELLOW}? 可能运行中${NC} (PID: ${agent_pids})"
        else
            echo -e "  ${RED}✗ 未运行${NC}"
        fi
    fi
    
    echo
    
    # 显示所有Java进程
    local java_processes=$(pgrep -f "java.*easyJmeter" 2>/dev/null)
    if [[ -n "${java_processes}" ]]; then
        echo -e "${CYAN}Java进程:${NC}"
        for pid in ${java_processes}; do
            local cmd=$(ps -p "${pid}" -o cmd --no-headers 2>/dev/null | cut -c1-80)
            echo -e "  PID ${pid}: ${cmd}..."
        done
    fi
    
    echo
}

# 查看日志菜单
show_logs() {
    clear_screen
    echo -e "${CYAN}========================================"
    echo -e "           日志查看菜单"
    echo -e "========================================${NC}"
    echo
    echo -e "${BLUE}请选择要查看的日志:${NC}"
    echo
    echo -e "   ${GREEN}1.${NC} Server实时日志"
    echo -e "   ${GREEN}2.${NC} Agent实时日志"
    echo -e "   ${YELLOW}3.${NC} Server错误日志"
    echo -e "   ${YELLOW}4.${NC} Agent错误日志"
    echo -e "   ${BLUE}5.${NC} 返回主菜单"
    echo
    
    read -p "请选择 (1-5): " log_choice
    
    case $log_choice in
        1)
            local server_log="${SCRIPT_DIR}/../logs/server/server.log"
            if [[ -f "${server_log}" ]]; then
                echo -e "${BLUE}显示Server实时日志 (按Ctrl+C退出)${NC}"
                tail -f "${server_log}"
            else
                echo -e "${RED}Server日志文件不存在: ${server_log}${NC}"
            fi
            ;;
        2)
            local agent_log="${SCRIPT_DIR}/../logs/agent/agent.log"
            if [[ -f "${agent_log}" ]]; then
                echo -e "${BLUE}显示Agent实时日志 (按Ctrl+C退出)${NC}"
                tail -f "${agent_log}"
            else
                echo -e "${RED}Agent日志文件不存在: ${agent_log}${NC}"
            fi
            ;;
        3)
            local server_log="${SCRIPT_DIR}/../logs/server/server.log"
            if [[ -f "${server_log}" ]]; then
                echo -e "${YELLOW}Server最近错误 (最后50行包含error/exception):${NC}"
                grep -i -E "(error|exception)" "${server_log}" | tail -50
            else
                echo -e "${RED}Server日志文件不存在${NC}"
            fi
            ;;
        4)
            local agent_log="${SCRIPT_DIR}/../logs/agent/agent.log"
            if [[ -f "${agent_log}" ]]; then
                echo -e "${YELLOW}Agent最近错误 (最后50行包含error/exception):${NC}"
                grep -i -E "(error|exception)" "${agent_log}" | tail -50
            else
                echo -e "${RED}Agent日志文件不存在${NC}"
            fi
            ;;
        5)
            return
            ;;
        *)
            echo -e "${RED}无效选项${NC}"
            ;;
    esac
    
    if [[ $log_choice != 5 ]]; then
        echo
        read -p "按Enter键继续..."
        show_logs
    fi
}

# 显示系统信息
show_system_info() {
    clear_screen
    echo -e "${CYAN}========================================"
    echo -e "           系统信息"
    echo -e "========================================${NC}"
    echo
    
    echo -e "${BLUE}Java版本:${NC}"
    if command -v java &> /dev/null; then
        java -version 2>&1 | head -3
    else
        echo -e "${RED}  Java未安装${NC}"
    fi
    echo
    
    echo -e "${BLUE}系统信息:${NC}"
    echo -e "  操作系统: $(uname -s)"
    echo -e "  内核版本: $(uname -r)"
    echo -e "  架构: $(uname -m)"
    echo
    
    echo -e "${BLUE}内存信息:${NC}"
    free -h
    echo
    
    echo -e "${BLUE}磁盘空间:${NC}"
    df -h "${SCRIPT_DIR}/../" | tail -1
    echo
    
    echo -e "${BLUE}网络端口:${NC}"
    echo -e "  5000端口: $(netstat -tln 2>/dev/null | grep :5000 | wc -l) 个监听"
    echo -e "  9000端口: $(netstat -tln 2>/dev/null | grep :9000 | wc -l) 个监听"
    echo
    
    read -p "按Enter键返回主菜单..."
}

# 主循环
main() {
    while true; do
        show_menu
        read -p "请选择 (1-8): " choice
        
        case $choice in
            1)
                echo
                echo -e "${BLUE}启动 Easy JMeter Server...${NC}"
                "${SCRIPT_DIR}/linux/start-server.sh"
                read -p "按Enter键返回主菜单..."
                ;;
            2)
                echo
                echo -e "${BLUE}启动 Easy JMeter Agent...${NC}"
                "${SCRIPT_DIR}/linux/start-agent.sh"
                read -p "按Enter键返回主菜单..."
                ;;
            3)
                echo
                echo -e "${YELLOW}停止 Easy JMeter Server...${NC}"
                "${SCRIPT_DIR}/linux/stop-server.sh"
                read -p "按Enter键返回主菜单..."
                ;;
            4)
                echo
                echo -e "${YELLOW}停止 Easy JMeter Agent...${NC}"
                "${SCRIPT_DIR}/linux/stop-agent.sh"
                read -p "按Enter键返回主菜单..."
                ;;
            5)
                check_status
                read -p "按Enter键返回主菜单..."
                ;;
            6)
                show_logs
                ;;
            7)
                show_system_info
                ;;
            8)
                echo
                echo -e "${GREEN}再见！${NC}"
                exit 0
                ;;
            *)
                echo
                echo -e "${RED}无效的选项，请重新选择。${NC}"
                sleep 2
                ;;
        esac
    done
}

# 检查脚本权限
if [[ ! -x "${SCRIPT_DIR}/linux/start-server.sh" ]]; then
    echo -e "${YELLOW}检测到脚本没有执行权限，正在设置...${NC}"
    chmod +x "${SCRIPT_DIR}/linux/"*.sh
    chmod +x "${SCRIPT_DIR}/setup-permissions.sh"
    echo -e "${GREEN}权限设置完成${NC}"
    echo
fi

# 启动主程序
main
