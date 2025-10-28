#!/bin/bash
#=================================
# 设置Linux脚本执行权限
#=================================

echo "设置Easy JMeter启动脚本执行权限..."

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 设置Linux脚本权限
echo "正在设置Linux脚本执行权限..."
chmod +x "${SCRIPT_DIR}/linux/start-server.sh"
chmod +x "${SCRIPT_DIR}/linux/start-agent.sh" 
chmod +x "${SCRIPT_DIR}/linux/stop-server.sh"
chmod +x "${SCRIPT_DIR}/linux/stop-agent.sh"

echo "权限设置完成！"
echo
echo "现在您可以使用以下命令："
echo "  启动Server: ./scripts/linux/start-server.sh"
echo "  启动Agent:  ./scripts/linux/start-agent.sh"
echo "  停止Server: ./scripts/linux/stop-server.sh"
echo "  停止Agent:  ./scripts/linux/stop-agent.sh"
