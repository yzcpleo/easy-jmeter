#!/bin/bash

# =================================
# Easy JMeter 一键Docker部署脚本
# =================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_step() { echo -e "${CYAN}[STEP]${NC} $1"; }

echo "========================================="
echo "🚀 Easy JMeter Quick Docker Deployment"
echo "========================================="

# 检查参数
VERSION="${1:-latest}"
MODE="${2:-full}"  # full, build-only, deploy-only

print_info "Quick Deployment Configuration:"
echo "  Version: $VERSION"
echo "  Mode: $MODE"
echo "  Working Directory: $(pwd)"

# 环境检查
check_environment() {
    print_step "Checking environment..."
    
    # 检查必要工具
    for cmd in docker docker-compose java mvn; do
        if ! command -v $cmd &> /dev/null; then
            print_error "$cmd not found. Please install $cmd first."
            exit 1
        fi
    done
    
    # 检查Docker服务
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker daemon not running. Please start Docker first."
        exit 1
    fi
    
    print_success "Environment check passed"
}

# 构建JAR包
build_jar() {
    print_step "Building JAR package..."
    
    if [ ! -f "api/target/easyJmeter-0.1.0-RELEASE.jar" ] || [ "$MODE" = "full" ]; then
        print_info "Building Java application..."
        cd api
        mvn clean package -DskipTests -q
        cd ..
        
        if [ -f "api/target/easyJmeter-0.1.0-RELEASE.jar" ]; then
            JAR_SIZE=$(du -h "api/target/easyJmeter-0.1.0-RELEASE.jar" | cut -f1)
            print_success "JAR built successfully (${JAR_SIZE})"
        else
            print_error "Failed to build JAR package"
            exit 1
        fi
    else
        print_info "JAR package already exists, skipping build"
    fi
}

# 创建Docker文件
create_docker_files() {
    print_step "Setting up Docker configuration..."
    
    # 创建目录
    mkdir -p docker deploy
    
    # 检查是否需要创建Docker文件
    if [ ! -f "docker/Dockerfile.server" ] || [ ! -f "docker/Dockerfile.agent" ]; then
        print_warning "Docker files not found. Please ensure all Docker files are created."
        print_info "Required files:"
        echo "  - docker/Dockerfile.server"
        echo "  - docker/Dockerfile.agent"
        echo "  - docker/docker-entrypoint.sh"
        echo "  - docker/docker-compose.prod.yml"
        echo ""
        print_info "Please run the full setup from the Docker流水线部署指南.md"
        exit 1
    fi
    
    # 设置执行权限
    chmod +x docker/docker-entrypoint.sh deploy/*.sh 2>/dev/null || true
    
    print_success "Docker configuration ready"
}

# 构建镜像
build_images() {
    if [ "$MODE" = "deploy-only" ]; then
        print_info "Skipping image build (deploy-only mode)"
        return
    fi
    
    print_step "Building Docker images..."
    
    if [ -f "deploy/build-images.sh" ]; then
        chmod +x deploy/build-images.sh
        ./deploy/build-images.sh "$VERSION"
    else
        print_info "Using manual docker build..."
        
        # 构建Server镜像
        print_info "Building server image..."
        docker build -f docker/Dockerfile.server -t "easy-jmeter/server:$VERSION" .
        
        # 构建Agent镜像
        print_info "Building agent image..."
        docker build -f docker/Dockerfile.agent -t "easy-jmeter/agent:$VERSION" .
        
        print_success "Images built successfully"
    fi
}

# 部署服务
deploy_services() {
    if [ "$MODE" = "build-only" ]; then
        print_info "Skipping deployment (build-only mode)"
        return
    fi
    
    print_step "Deploying services..."
    
    if [ -f "deploy/deploy.sh" ]; then
        chmod +x deploy/deploy.sh
        ./deploy/deploy.sh "$VERSION"
    else
        print_info "Using manual deployment..."
        
        # 创建环境变量文件
        if [ ! -f ".env" ]; then
            print_info "Creating default .env file..."
            cat > .env << EOF
VERSION=$VERSION
SERVER_HTTP_PORT=5000
SERVER_SOCKET_PORT=9000
WEB_PORT=80
MYSQL_PORT=3306
MYSQL_ROOT_PASSWORD=EasyJmeter@2023
MONGO_PORT=27017
MONGO_PASSWORD=mongo@2023
INFLUX_PORT=8086
INFLUX_PASSWORD=influx@2023
MINIO_PORT=9000
MINIO_CONSOLE_PORT=9001
MINIO_ACCESS_KEY=easyJmeterAdmin
MINIO_SECRET_KEY=easyJmeter@MinIO2023
JMETER_VERSION=5.6.2
EOF
        fi
        
        # 部署
        docker-compose -f docker/docker-compose.prod.yml up -d
        
        print_success "Services deployed successfully"
    fi
}

# 显示结果
show_results() {
    echo ""
    print_success "Quick deployment completed!"
    echo ""
    print_info "🎯 What was deployed:"
    
    case "$MODE" in
        "build-only")
            echo "  ✅ JAR package built"
            echo "  ✅ Docker images created"
            ;;
        "deploy-only")
            echo "  ✅ Services deployed"
            ;;
        "full"|*)
            echo "  ✅ JAR package built"
            echo "  ✅ Docker images created"
            echo "  ✅ Services deployed"
            ;;
    esac
    
    if [ "$MODE" != "build-only" ]; then
        echo ""
        print_info "🌐 Access URLs:"
        echo "  Server API: http://localhost:5000"
        echo "  Socket.IO: http://localhost:9000"  
        echo "  Web UI: http://localhost:80"
        echo "  MinIO Console: http://localhost:9001"
        
        echo ""
        print_info "🔑 Default Credentials:"
        echo "  Admin User: admin / admin123"
        echo "  MinIO: easyJmeterAdmin / easyJmeter@MinIO2023"
        
        echo ""
        print_info "📋 Useful Commands:"
        echo "  Check status: docker-compose -f docker/docker-compose.prod.yml ps"
        echo "  View logs: docker logs -f easy-jmeter-server"
        echo "  Stop services: docker-compose -f docker/docker-compose.prod.yml down"
    fi
    
    echo ""
    print_info "📖 Next Steps:"
    echo "  1. 访问 Web界面 http://localhost:80"
    echo "  2. 使用 admin/admin123 登录"
    echo "  3. 在'压力机管理'中添加Agent节点"
    echo "  4. 创建测试项目和用例"
    echo "  5. 执行性能测试"
}

# 错误处理
error_handler() {
    print_error "Deployment failed at step: $1"
    echo ""
    print_info "🔍 Troubleshooting:"
    echo "  1. Check Docker daemon status: docker info"
    echo "  2. Check available space: df -h"
    echo "  3. Check logs: docker-compose logs"
    echo "  4. Clean up and retry: docker system prune -f"
    exit 1
}

# 主流程
main() {
    # 设置错误处理
    trap 'error_handler "Environment Check"' ERR
    check_environment
    
    trap 'error_handler "JAR Build"' ERR
    build_jar
    
    trap 'error_handler "Docker Setup"' ERR
    create_docker_files
    
    trap 'error_handler "Image Build"' ERR
    build_images
    
    trap 'error_handler "Service Deploy"' ERR
    deploy_services
    
    # 重置错误处理
    trap - ERR
    
    show_results
}

# 显示使用说明
show_usage() {
    echo "Usage: $0 [VERSION] [MODE]"
    echo ""
    echo "Parameters:"
    echo "  VERSION    Docker image version (default: latest)"
    echo "  MODE       Deployment mode:"
    echo "             full        - Build JAR + Images + Deploy (default)"
    echo "             build-only  - Only build JAR and Images"
    echo "             deploy-only - Only deploy services"
    echo ""
    echo "Examples:"
    echo "  $0                    # Full deployment with latest version"
    echo "  $0 v1.0.0            # Full deployment with specific version"
    echo "  $0 latest build-only # Only build, don't deploy"
    echo "  $0 v1.0.0 deploy-only # Only deploy existing images"
}

# 参数处理
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_usage
    exit 0
fi

# 执行主流程
main

print_success "🎉 Easy JMeter Docker deployment completed successfully!"
