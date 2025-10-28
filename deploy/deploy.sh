#!/bin/bash

set -e

# 配置变量
PROJECT_NAME="easy-jmeter"
VERSION="${1:-latest}"
COMPOSE_FILE="docker/docker-compose.prod.yml"
ENV_FILE="${2:-.env}"
MODE="${3:-full}"  # full, infra-only, app-only

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_step() { echo -e "${CYAN}[STEP]${NC} $1"; }

echo "========================================="
echo "  Easy JMeter Production Deployment"
echo "========================================="

print_info "Deployment Configuration:"
echo "  Project: $PROJECT_NAME"
echo "  Version: $VERSION"
echo "  Compose File: $COMPOSE_FILE"
echo "  Environment File: $ENV_FILE"
echo "  Mode: $MODE"

# 检查必要工具
for cmd in docker docker-compose curl; do
    if ! command -v $cmd &> /dev/null; then
        print_error "$cmd not found. Please install $cmd first."
        exit 1
    fi
done

# 检查环境文件
if [ ! -f "$ENV_FILE" ]; then
    print_warning "Environment file not found: $ENV_FILE"
    print_info "Creating default environment file..."
    
    cat > "$ENV_FILE" << 'EOF'
# Easy JMeter Production Environment Variables

# 应用版本
VERSION=latest

# 端口配置
SERVER_HTTP_PORT=5000
SERVER_SOCKET_PORT=9000
WEB_PORT=80

# 数据库配置
MYSQL_PORT=3306
MYSQL_ROOT_PASSWORD=EasyJmeter@2023
MYSQL_USER_PASSWORD=jmeter123

MONGO_PORT=27017
MONGO_PASSWORD=mongo@2023

INFLUX_PORT=8086
INFLUX_PASSWORD=influx@2023

# MinIO配置
MINIO_PORT=9000
MINIO_CONSOLE_PORT=9001
MINIO_ACCESS_KEY=easyJmeterAdmin
MINIO_SECRET_KEY=easyJmeter@MinIO2023

# JMeter配置
JMETER_VERSION=5.6.2

# 性能配置
COMPOSE_HTTP_TIMEOUT=300
COMPOSE_PARALLEL_LIMIT=10
EOF
    
    print_success "Default environment file created: $ENV_FILE"
    print_warning "Please review and modify the passwords and configuration if needed!"
fi

# 检查Docker Compose文件
if [ ! -f "$COMPOSE_FILE" ]; then
    print_error "Docker Compose file not found: $COMPOSE_FILE"
    exit 1
fi

# 创建JMeter数据卷和安装JMeter
setup_jmeter() {
    if ! docker volume ls | grep -q jmeter-install; then
        print_step "Creating JMeter installation volume..."
        docker volume create jmeter-install
        
        print_step "Downloading and installing JMeter ${JMETER_VERSION:-5.6.2}..."
        docker run --rm \
            -v jmeter-install:/opt/apache-jmeter \
            alpine/curl:latest sh -c "
                apk add --no-cache tar gzip && \
                cd /tmp && \
                echo 'Downloading JMeter...' && \
                wget -q --timeout=300 -O jmeter.tgz https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VERSION:-5.6.2}.tgz && \
                echo 'Extracting JMeter...' && \
                tar -xzf jmeter.tgz --strip-components=1 -C /opt/apache-jmeter && \
                echo 'Setting permissions...' && \
                chmod +x /opt/apache-jmeter/bin/jmeter* && \
                echo 'JMeter installation completed!'
            "
        
        if [ $? -eq 0 ]; then
            print_success "JMeter installation completed!"
        else
            print_error "Failed to install JMeter"
            return 1
        fi
    else
        print_info "JMeter volume already exists, skipping installation"
    fi
}

# 创建必要目录
create_directories() {
    print_step "Creating necessary directories..."
    
    # 创建日志目录
    mkdir -p logs/{server,agent1,agent2,web}
    mkdir -p data/{mysql,mongodb,influxdb,minio}
    mkdir -p docker/init-sql
    
    # 设置权限
    chmod -R 755 logs data
    
    print_success "Directories created successfully"
}

# 创建数据库初始化脚本
create_init_scripts() {
    print_step "Creating database initialization scripts..."
    
    # 创建MySQL初始化脚本
    if [ ! -f "docker/init-sql/01-schema.sql" ]; then
        if [ -f "api/src/main/resources/schema.sql" ]; then
            cp "api/src/main/resources/schema.sql" "docker/init-sql/01-schema.sql"
            print_info "Copied schema.sql to init scripts"
        else
            print_warning "schema.sql not found, please create database schema manually"
        fi
    fi
    
    # 创建基础数据初始化脚本
    cat > "docker/init-sql/02-init-data.sql" << 'EOF'
-- Easy JMeter 基础数据初始化
-- 管理员账户
INSERT IGNORE INTO `lin_user` (`id`, `username`, `nickname`, `avatar`, `email`, `create_time`, `update_time`, `delete_time`, `admin`) 
VALUES (1, 'admin', 'Administrator', NULL, 'admin@easy-jmeter.com', NOW(), NOW(), NULL, 1);

-- 管理员登录凭证 (密码: admin123)
INSERT IGNORE INTO `lin_user_identity` (`id`, `user_id`, `identity_type`, `identifier`, `credential`, `create_time`, `update_time`, `delete_time`) 
VALUES (1, 1, 'USERNAME_PASSWORD', 'admin', '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7pCu6v2XyFDtC4q.jeeGm/TEZyj15C', NOW(), NOW(), NULL);
EOF
}

# 部署基础服务
deploy_infrastructure() {
    print_step "Deploying infrastructure services..."
    
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d \
        easy-jmeter-mysql \
        easy-jmeter-mongodb \
        easy-jmeter-influxdb \
        easy-jmeter-minio
    
    print_info "Waiting for infrastructure services to be ready..."
    
    # 等待MySQL就绪
    print_info "Waiting for MySQL..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if docker exec easy-jmeter-mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
            print_success "MySQL is ready"
            break
        fi
        sleep 2
        ((timeout-=2))
    done
    
    # 等待MongoDB就绪
    print_info "Waiting for MongoDB..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if docker exec easy-jmeter-mongodb mongo --eval "db.adminCommand('ping')" >/dev/null 2>&1; then
            print_success "MongoDB is ready"
            break
        fi
        sleep 2
        ((timeout-=2))
    done
    
    # 等待InfluxDB就绪
    print_info "Waiting for InfluxDB..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if curl -s http://localhost:${INFLUX_PORT:-8086}/ping >/dev/null 2>&1; then
            print_success "InfluxDB is ready"
            break
        fi
        sleep 2
        ((timeout-=2))
    done
    
    # 等待MinIO就绪
    print_info "Waiting for MinIO..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if curl -s http://localhost:${MINIO_PORT:-9000}/minio/health/live >/dev/null 2>&1; then
            print_success "MinIO is ready"
            break
        fi
        sleep 2
        ((timeout-=2))
    done
    
    print_success "Infrastructure services are ready!"
}

# 部署应用服务
deploy_applications() {
    print_step "Deploying application services..."
    
    # 启动Server
    print_info "Starting Easy JMeter Server..."
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d easy-jmeter-server
    
    # 等待Server就绪
    print_info "Waiting for Server to be ready..."
    timeout=120
    while [ $timeout -gt 0 ]; do
        if curl -s http://localhost:${SERVER_HTTP_PORT:-5000}/actuator/health >/dev/null 2>&1; then
            print_success "Server is ready"
            break
        fi
        sleep 3
        ((timeout-=3))
    done
    
    if [ $timeout -le 0 ]; then
        print_warning "Server health check timeout, but continuing with agent deployment"
    fi
    
    # 启动Agent服务
    print_info "Starting Easy JMeter Agents..."
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d \
        easy-jmeter-agent-1 \
        easy-jmeter-agent-2
    
    # 启动Web服务（如果前端资源存在）
    if [ -d "web/dist" ] || [ -d "docker/web-dist" ]; then
        print_info "Starting Web service..."
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d easy-jmeter-web
    else
        print_warning "Web dist directory not found, skipping web service deployment"
    fi
    
    print_success "Application services deployed!"
}

# 显示部署状态
show_status() {
    echo ""
    print_step "Deployment Status:"
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
    
    echo ""
    print_step "Service Health Check:"
    
    # 检查各个服务
    services=(
        "easy-jmeter-server:${SERVER_HTTP_PORT:-5000}:/actuator/health"
        "easy-jmeter-mysql:${MYSQL_PORT:-3306}"
        "easy-jmeter-mongodb:${MONGO_PORT:-27017}"
        "easy-jmeter-influxdb:${INFLUX_PORT:-8086}:/ping"
        "easy-jmeter-minio:${MINIO_PORT:-9000}:/minio/health/live"
    )
    
    for service_info in "${services[@]}"; do
        IFS=':' read -r service port path <<< "$service_info"
        if [ -z "$path" ]; then
            # 端口检查
            if nc -z localhost "$port" 2>/dev/null; then
                echo "  ✅ $service (Port $port): OK"
            else
                echo "  ❌ $service (Port $port): Failed"
            fi
        else
            # HTTP检查
            if curl -s "http://localhost:$port$path" >/dev/null 2>&1; then
                echo "  ✅ $service: OK"
            else
                echo "  ❌ $service: Failed"
            fi
        fi
    done
}

# 显示访问信息
show_access_info() {
    echo ""
    print_success "Deployment completed successfully!"
    echo ""
    print_info "Access URLs:"
    echo "  🖥️  Server API: http://localhost:${SERVER_HTTP_PORT:-5000}"
    echo "  📊  Server Socket: http://localhost:${SERVER_SOCKET_PORT:-9000}"
    echo "  🌐  Web Interface: http://localhost:${WEB_PORT:-80}"
    echo "  💾  MinIO Console: http://localhost:${MINIO_CONSOLE_PORT:-9001}"
    echo ""
    print_info "Default Credentials:"
    echo "  📋  Web Login: admin / admin123"
    echo "  💾  MinIO: ${MINIO_ACCESS_KEY:-easyJmeterAdmin} / ${MINIO_SECRET_KEY:-easyJmeter@MinIO2023}"
    echo ""
    print_info "Useful Commands:"
    echo "  📋  View logs: docker logs -f <container-name>"
    echo "  🔄  Restart service: docker-compose -f $COMPOSE_FILE restart <service-name>"
    echo "  🛑  Stop all: docker-compose -f $COMPOSE_FILE down"
    echo "  🔍  Service status: docker-compose -f $COMPOSE_FILE ps"
}

# 主部署流程
main() {
    case "$MODE" in
        "infra-only")
            create_directories
            create_init_scripts
            setup_jmeter
            deploy_infrastructure
            ;;
        "app-only")
            deploy_applications
            ;;
        "full"|*)
            create_directories
            create_init_scripts
            setup_jmeter
            
            # 停止现有服务
            print_step "Stopping existing services..."
            docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down 2>/dev/null || true
            
            deploy_infrastructure
            sleep 10  # 给基础服务一些启动时间
            deploy_applications
            ;;
    esac
    
    sleep 5  # 等待服务稳定
    show_status
    show_access_info
}

# 捕获中断信号
trap 'print_warning "Deployment interrupted by user"; exit 1' INT

# 执行主流程
main

# 最终检查
echo ""
print_info "Final verification in 10 seconds..."
sleep 10
show_status
