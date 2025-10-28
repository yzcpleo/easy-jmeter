#!/bin/bash

set -e

# 配置变量
PROJECT_NAME="easy-jmeter"
VERSION="${1:-latest}"
REGISTRY="${REGISTRY:-}"  # 私有仓库地址，可通过环境变量设置
PUSH_TO_REGISTRY="${2:-false}"

echo "========================================="
echo "  Easy JMeter Docker Images Build"
echo "========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查Docker是否安装
if ! command -v docker &> /dev/null; then
    print_error "Docker not found. Please install Docker first."
    exit 1
fi

# 检查JAR包是否存在
JAR_FILE="api/target/easyJmeter-0.1.0-RELEASE.jar"
if [ ! -f "$JAR_FILE" ]; then
    print_error "JAR file not found: $JAR_FILE"
    print_info "Please build the project first:"
    echo "  cd api && mvn clean package -DskipTests"
    exit 1
fi

print_success "Found JAR file: $JAR_FILE ($(du -h "$JAR_FILE" | cut -f1))"

# 检查Docker构建文件
DOCKERFILE_SERVER="docker/Dockerfile.server"
DOCKERFILE_AGENT="docker/Dockerfile.agent"
ENTRYPOINT_SCRIPT="docker/docker-entrypoint.sh"

for file in "$DOCKERFILE_SERVER" "$DOCKERFILE_AGENT" "$ENTRYPOINT_SCRIPT"; do
    if [ ! -f "$file" ]; then
        print_error "Required file not found: $file"
        exit 1
    fi
done

print_success "All required Docker files found"

# 显示构建信息
print_info "Build Configuration:"
echo "  Project Name: $PROJECT_NAME"
echo "  Version: $VERSION"
echo "  Registry: ${REGISTRY:-<none>}"
echo "  Push to Registry: $PUSH_TO_REGISTRY"
echo "  JAR File: $JAR_FILE"

# 构建Server镜像
print_info "Building Server image..."
docker build \
    -f "$DOCKERFILE_SERVER" \
    -t "${PROJECT_NAME}/server:${VERSION}" \
    -t "${PROJECT_NAME}/server:latest" \
    --build-arg BUILD_DATE="$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
    --build-arg VERSION="$VERSION" \
    .

if [ $? -eq 0 ]; then
    print_success "Server image built successfully"
else
    print_error "Failed to build Server image"
    exit 1
fi

# 构建Agent镜像
print_info "Building Agent image..."
docker build \
    -f "$DOCKERFILE_AGENT" \
    -t "${PROJECT_NAME}/agent:${VERSION}" \
    -t "${PROJECT_NAME}/agent:latest" \
    --build-arg BUILD_DATE="$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
    --build-arg VERSION="$VERSION" \
    .

if [ $? -eq 0 ]; then
    print_success "Agent image built successfully"
else
    print_error "Failed to build Agent image"
    exit 1
fi

print_success "All images built successfully!"

# 显示构建的镜像
echo ""
print_info "Built images:"
docker images | grep "$PROJECT_NAME" | head -10

# 镜像大小统计
SERVER_SIZE=$(docker images "${PROJECT_NAME}/server:${VERSION}" --format "table {{.Size}}" | tail -n +2)
AGENT_SIZE=$(docker images "${PROJECT_NAME}/agent:${VERSION}" --format "table {{.Size}}" | tail -n +2)

echo ""
print_info "Image sizes:"
echo "  Server: $SERVER_SIZE"
echo "  Agent: $AGENT_SIZE"

# 推送到镜像仓库（可选）
if [ "$PUSH_TO_REGISTRY" = "--push" ] || [ "$PUSH_TO_REGISTRY" = "true" ]; then
    if [ -z "$REGISTRY" ]; then
        print_warning "Registry not specified, skipping push"
    else
        print_info "Pushing images to registry: $REGISTRY"
        
        # 标记镜像
        docker tag "${PROJECT_NAME}/server:${VERSION}" "${REGISTRY}/${PROJECT_NAME}/server:${VERSION}"
        docker tag "${PROJECT_NAME}/agent:${VERSION}" "${REGISTRY}/${PROJECT_NAME}/agent:${VERSION}"
        
        if [ "$VERSION" != "latest" ]; then
            docker tag "${PROJECT_NAME}/server:latest" "${REGISTRY}/${PROJECT_NAME}/server:latest"
            docker tag "${PROJECT_NAME}/agent:latest" "${REGISTRY}/${PROJECT_NAME}/agent:latest"
        fi
        
        # 推送镜像
        print_info "Pushing server image..."
        docker push "${REGISTRY}/${PROJECT_NAME}/server:${VERSION}"
        
        print_info "Pushing agent image..."
        docker push "${REGISTRY}/${PROJECT_NAME}/agent:${VERSION}"
        
        if [ "$VERSION" != "latest" ]; then
            docker push "${REGISTRY}/${PROJECT_NAME}/server:latest"
            docker push "${REGISTRY}/${PROJECT_NAME}/agent:latest"
        fi
        
        print_success "Images pushed successfully!"
    fi
fi

echo ""
print_success "Build completed successfully!"
echo ""
print_info "Next steps:"
echo "  1. Setup JMeter volume: docker volume create jmeter-install"
echo "  2. Deploy services: ./deploy/deploy.sh"
echo ""
print_info "Manual deployment:"
echo "  docker-compose -f docker/docker-compose.prod.yml up -d"

# 清理构建缓存（可选）
if [ "$3" = "--clean" ]; then
    print_info "Cleaning up Docker build cache..."
    docker builder prune -f
    print_success "Build cache cleaned"
fi
