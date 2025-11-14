#!/bin/bash

# Easy JMeter Client 启动脚本
# 用于在宿主机上部署Client模式，连接宿主机的服务

set -e

echo "========================================="
echo "  Easy JMeter Client Deployment"
echo "========================================="

# 检查必要的目录
mkdir -p logs jmeter-results temp

# 检查配置文件
if [ ! -f "docker/application-client.yml" ]; then
    echo "Error: docker/application-client.yml not found!"
    exit 1
fi

# 检查JAR包
if [ ! -f "api/target/easyJmeter-*.jar" ]; then
    echo "Error: JAR file not found in api/target/ directory!"
    echo "Please build the project first: mvn clean package"
    exit 1
fi

# 获取宿主机IP地址
HOST_IP=$(hostname -I | awk '{print $1}')
echo "Detected host IP: $HOST_IP"

# 设置环境变量
export DOCKER_HOST_IP=$HOST_IP

echo "Building Easy JMeter Client image..."
docker build -f docker/Dockerfile.client -t easyjmeter-client:latest .

echo "Starting Easy JMeter Client container..."
docker-compose -f docker-compose.client.yml up -d

echo "========================================="
echo "  Client Deployment Complete!"
echo "========================================="
echo "Container Status:"
docker-compose -f docker-compose.client.yml ps

echo ""
echo "Access Information:"
echo "- API Endpoint: http://localhost:5000"
echo "- Health Check: http://localhost:5000/actuator/health"
echo "- Logs: ./logs/"
echo "- Results: ./jmeter-results/"

echo ""
echo "To view logs:"
echo "docker logs -f easyjmeter-client"

echo ""
echo "To stop client:"
echo "docker-compose -f docker-compose.client.yml down"

echo ""
echo "To restart client:"
echo "docker-compose -f docker-compose.client.yml restart"