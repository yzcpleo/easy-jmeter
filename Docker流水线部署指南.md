# 🚀 Easy JMeter Docker 流水线部署指南

## 📋 概述

本指南适用于CI/CD流水线环境，通过已构建的JAR包快速创建Docker镜像并部署Server和Agent服务。支持无预制镜像的情况下，从源码到容器化部署的完整流程。

## 🏗️ 部署架构

```
CI/CD Pipeline → JAR包 → Docker构建 → 镜像推送 → 容器部署
     ↓              ↓           ↓          ↓           ↓
   源码编译    → easyJmeter.jar → Docker镜像 → 镜像仓库  → 生产环境
```

## 📦 准备工作

### 环境要求
- Docker Engine 20.10+
- Docker Compose 2.0+
- 已构建的JAR包：`api/target/easyJmeter-0.1.0-RELEASE.jar`

### 目录结构
```
project-root/
├── api/target/easyJmeter-0.1.0-RELEASE.jar  # 已构建的JAR包
├── docker/                                   # Docker相关文件
│   ├── Dockerfile.server                    # Server镜像构建文件
│   ├── Dockerfile.agent                     # Agent镜像构建文件
│   ├── docker-entrypoint.sh                # 容器入口脚本
│   └── docker-compose.prod.yml             # 生产环境配置
├── deploy/                                   # 部署脚本
│   ├── build-images.sh                     # 镜像构建脚本
│   ├── deploy.sh                           # 部署脚本
│   └── config/                             # 配置文件模板
└── scripts/                                 # 工具脚本
```

## 🐳 1. 创建Docker构建文件

### 1.1 服务端Dockerfile

```dockerfile
# docker/Dockerfile.server
FROM openjdk:8-jre-alpine

LABEL maintainer="Easy JMeter Team"
LABEL version="2.0.0"
LABEL description="Easy JMeter Server - Performance Testing Platform"

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 创建应用目录
WORKDIR /opt/easy-jmeter

# 复制JAR包和启动脚本
COPY api/target/*.jar ./easyJmeter.jar
COPY docker/docker-entrypoint.sh ./
RUN chmod +x docker-entrypoint.sh

# 创建日志和数据目录
RUN mkdir -p logs assets temp && \
    addgroup -g 1000 jmeter && \
    adduser -D -s /bin/sh -u 1000 -G jmeter jmeter && \
    chown -R jmeter:jmeter /opt/easy-jmeter

# 安装必要工具
RUN apk add --no-cache curl procps

# JVM参数优化
ENV JAVA_OPTS="-server -Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.awt.headless=true"

# 应用参数
ENV APP_OPTS="-Dfile.encoding=UTF-8 -Dspring.profiles.active=prod -Dsocket.server.enable=true -Dsocket.client.enable=false"

# 暴露端口
EXPOSE 5000 9000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:5000/actuator/health || exit 1

# 使用非root用户
USER jmeter

ENTRYPOINT ["./docker-entrypoint.sh"]
CMD ["server"]
```

### 1.2 Agent端Dockerfile

```dockerfile
# docker/Dockerfile.agent
FROM openjdk:8-jre-alpine

LABEL maintainer="Easy JMeter Team"
LABEL version="2.0.0"
LABEL description="Easy JMeter Agent - Performance Testing Executor"

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 创建应用目录
WORKDIR /opt/easy-jmeter

# 复制JAR包和启动脚本
COPY api/target/*.jar ./easyJmeter.jar
COPY docker/docker-entrypoint.sh ./
RUN chmod +x docker-entrypoint.sh

# 创建必要目录
RUN mkdir -p logs temp jmeter-results && \
    addgroup -g 1000 jmeter && \
    adduser -D -s /bin/sh -u 1000 -G jmeter jmeter && \
    chown -R jmeter:jmeter /opt/easy-jmeter

# 安装必要工具和JMeter依赖
RUN apk add --no-cache curl procps wget unzip

# 下载并安装JMeter（如果不通过Volume挂载）
ENV JMETER_VERSION=5.6.2
ENV JMETER_HOME=/opt/apache-jmeter
ENV PATH=$JMETER_HOME/bin:$PATH

# 可选：内置JMeter（增加镜像大小但更独立）
# RUN wget -O /tmp/jmeter.tgz https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz && \
#     tar -xzf /tmp/jmeter.tgz -C /opt/ && \
#     mv /opt/apache-jmeter-${JMETER_VERSION} ${JMETER_HOME} && \
#     rm /tmp/jmeter.tgz && \
#     chown -R jmeter:jmeter ${JMETER_HOME}

# JVM参数优化（Agent需要更多内存）
ENV JAVA_OPTS="-server -Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.awt.headless=true"

# 应用参数
ENV APP_OPTS="-Dfile.encoding=UTF-8 -Dspring.profiles.active=prod -Dsocket.server.enable=false -Dsocket.client.enable=true"

# 暴露端口
EXPOSE 5000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:5000/actuator/health || exit 1

# 使用非root用户
USER jmeter

ENTRYPOINT ["./docker-entrypoint.sh"]
CMD ["agent"]
```

### 1.3 容器入口脚本

```bash
#!/bin/sh
# docker/docker-entrypoint.sh

set -e

echo "========================================="
echo "  Easy JMeter Docker Container Starting"
echo "========================================="

# 打印环境信息
echo "Container Type: $1"
echo "Java Version: $(java -version 2>&1 | head -1)"
echo "Memory Info: $(free -h 2>/dev/null || echo 'Memory info not available')"
echo "Time Zone: $(date)"
echo "Working Directory: $(pwd)"

# 设置默认配置
if [ "$1" = "server" ]; then
    echo "Starting Easy JMeter Server..."
    
    # Server特定环境变量
    export APP_OPTS="${APP_OPTS} -Dsocket.server.enable=true -Dsocket.client.enable=false"
    
    # 等待数据库服务就绪
    if [ -n "$DB_HOST" ]; then
        echo "Waiting for database at $DB_HOST:${DB_PORT:-3306}..."
        while ! nc -z $DB_HOST ${DB_PORT:-3306}; do
            sleep 2
        done
        echo "Database is ready!"
    fi
    
elif [ "$1" = "agent" ]; then
    echo "Starting Easy JMeter Agent..."
    
    # Agent特定环境变量
    export APP_OPTS="${APP_OPTS} -Dsocket.server.enable=false -Dsocket.client.enable=true"
    
    # 检查JMeter安装
    if [ ! -d "$JMETER_HOME" ]; then
        echo "Warning: JMETER_HOME ($JMETER_HOME) not found!"
        echo "Please ensure JMeter is properly mounted or installed."
    else
        echo "JMeter Home: $JMETER_HOME"
        export PATH="$JMETER_HOME/bin:$PATH"
    fi
    
    # 等待Server服务就绪
    if [ -n "$SERVER_HOST" ]; then
        echo "Waiting for server at $SERVER_HOST:${SERVER_PORT:-9000}..."
        while ! nc -z $SERVER_HOST ${SERVER_PORT:-9000}; do
            sleep 5
        done
        echo "Server is ready!"
    fi
fi

# 打印最终的启动命令
echo "Java Options: $JAVA_OPTS"
echo "App Options: $APP_OPTS"
echo "JAR File: easyJmeter.jar"
echo "========================================="

# 启动应用
exec java $JAVA_OPTS $APP_OPTS -jar easyJmeter.jar
```

## 🐳 2. 生产环境Docker Compose配置

```yaml
# docker/docker-compose.prod.yml
version: "3.8"

services:
  # =================== 应用服务 ===================
  easy-jmeter-server:
    build:
      context: ../
      dockerfile: docker/Dockerfile.server
    image: easy-jmeter/server:${VERSION:-latest}
    container_name: easy-jmeter-server
    restart: unless-stopped
    ports:
      - "${SERVER_HTTP_PORT:-5000}:5000"
      - "${SERVER_SOCKET_PORT:-9000}:9000"
    networks:
      - easy-jmeter-net
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - TZ=Asia/Shanghai
      # 数据库配置
      - DB_HOST=easy-jmeter-mysql
      - DB_PORT=3306
      - DB_NAME=easy-jmeter
      - DB_USER=root
      - DB_PASSWORD=${MYSQL_ROOT_PASSWORD:-root}
      # MongoDB配置
      - MONGO_HOST=easy-jmeter-mongodb
      - MONGO_PORT=27017
      - MONGO_DB=easyJmeter
      - MONGO_USER=root
      - MONGO_PASSWORD=${MONGO_PASSWORD:-mongo2020}
      # InfluxDB配置
      - INFLUX_HOST=easy-jmeter-influxdb
      - INFLUX_PORT=8086
      - INFLUX_DB=easyJmeter
      - INFLUX_USER=root
      - INFLUX_PASSWORD=${INFLUX_PASSWORD:-root}
      # MinIO配置
      - MINIO_HOST=easy-jmeter-minio
      - MINIO_PORT=9000
      - MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY:-root}
      - MINIO_SECRET_KEY=${MINIO_SECRET_KEY:-minio2023}
    volumes:
      - server-logs:/opt/easy-jmeter/logs
      - server-assets:/opt/easy-jmeter/assets
    depends_on:
      - easy-jmeter-mysql
      - easy-jmeter-mongodb
      - easy-jmeter-influxdb
      - easy-jmeter-minio
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5000/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 120s

  easy-jmeter-agent-1:
    build:
      context: ../
      dockerfile: docker/Dockerfile.agent
    image: easy-jmeter/agent:${VERSION:-latest}
    container_name: easy-jmeter-agent-1
    restart: unless-stopped
    networks:
      easy-jmeter-net:
        ipv4_address: 172.20.0.10
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - TZ=Asia/Shanghai
      # Server连接配置
      - SERVER_HOST=easy-jmeter-server
      - SERVER_PORT=9000
      - SOCKET_CLIENT_SERVER_URL=http://easy-jmeter-server:9000
      # JMeter配置
      - JMETER_HOME=/opt/apache-jmeter
    volumes:
      - agent1-logs:/opt/easy-jmeter/logs
      - agent1-results:/opt/easy-jmeter/jmeter-results
      - jmeter-install:/opt/apache-jmeter:ro  # 只读挂载JMeter
    depends_on:
      - easy-jmeter-server

  easy-jmeter-agent-2:
    build:
      context: ../
      dockerfile: docker/Dockerfile.agent
    image: easy-jmeter/agent:${VERSION:-latest}
    container_name: easy-jmeter-agent-2
    restart: unless-stopped
    networks:
      easy-jmeter-net:
        ipv4_address: 172.20.0.11
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - TZ=Asia/Shanghai
      - SERVER_HOST=easy-jmeter-server
      - SERVER_PORT=9000
      - SOCKET_CLIENT_SERVER_URL=http://easy-jmeter-server:9000
      - JMETER_HOME=/opt/apache-jmeter
    volumes:
      - agent2-logs:/opt/easy-jmeter/logs
      - agent2-results:/opt/easy-jmeter/jmeter-results
      - jmeter-install:/opt/apache-jmeter:ro
    depends_on:
      - easy-jmeter-server

  # =================== 基础服务 ===================
  easy-jmeter-mysql:
    image: mysql:5.7
    container_name: easy-jmeter-mysql
    restart: unless-stopped
    ports:
      - "${MYSQL_PORT:-3306}:3306"
    networks:
      - easy-jmeter-net
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-root}
      - MYSQL_DATABASE=easy-jmeter
      - TZ=Asia/Shanghai
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init-sql:/docker-entrypoint-initdb.d:ro
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --innodb-buffer-pool-size=256M
      --max-connections=500

  easy-jmeter-mongodb:
    image: mongo:4.2
    container_name: easy-jmeter-mongodb
    restart: unless-stopped
    ports:
      - "${MONGO_PORT:-27017}:27017"
    networks:
      - easy-jmeter-net
    environment:
      - MONGO_INITDB_ROOT_USERNAME=root
      - MONGO_INITDB_ROOT_PASSWORD=${MONGO_PASSWORD:-mongo2020}
      - TZ=Asia/Shanghai
    volumes:
      - mongodb-data:/data/db

  easy-jmeter-influxdb:
    image: influxdb:1.8
    container_name: easy-jmeter-influxdb
    restart: unless-stopped
    ports:
      - "${INFLUX_PORT:-8086}:8086"
    networks:
      - easy-jmeter-net
    environment:
      - INFLUXDB_DB=easyJmeter
      - INFLUXDB_ADMIN_USER=admin
      - INFLUXDB_ADMIN_PASSWORD=admin
      - INFLUXDB_USER=root
      - INFLUXDB_USER_PASSWORD=${INFLUX_PASSWORD:-root}
      - TZ=Asia/Shanghai
    volumes:
      - influxdb-data:/var/lib/influxdb

  easy-jmeter-minio:
    image: bitnami/minio:2023
    container_name: easy-jmeter-minio
    restart: unless-stopped
    ports:
      - "${MINIO_PORT:-9000}:9000"
      - "${MINIO_CONSOLE_PORT:-9001}:9001"
    networks:
      - easy-jmeter-net
    environment:
      - MINIO_ROOT_USER=${MINIO_ACCESS_KEY:-root}
      - MINIO_ROOT_PASSWORD=${MINIO_SECRET_KEY:-minio2023}
      - MINIO_DEFAULT_BUCKETS=dev:public
    volumes:
      - minio-data:/bitnami/minio/data

  # =================== 前端服务 (可选) ===================
  easy-jmeter-web:
    image: nginx:alpine
    container_name: easy-jmeter-web
    restart: unless-stopped
    ports:
      - "${WEB_PORT:-80}:80"
    networks:
      - easy-jmeter-net
    volumes:
      - ./web-dist:/usr/share/nginx/html:ro
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
    depends_on:
      - easy-jmeter-server

networks:
  easy-jmeter-net:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
          gateway: 172.20.0.1

volumes:
  # 应用数据
  server-logs:
  server-assets:
  agent1-logs:
  agent1-results:
  agent2-logs:
  agent2-results:
  jmeter-install:
    external: true  # 外部JMeter安装卷
  
  # 数据库数据
  mysql-data:
  mongodb-data:
  influxdb-data:
  minio-data:
```

## 📝 3. 构建和部署脚本

### 3.1 镜像构建脚本

```bash
#!/bin/bash
# deploy/build-images.sh

set -e

# 配置变量
PROJECT_NAME="easy-jmeter"
VERSION="${1:-latest}"
REGISTRY="${REGISTRY:-localhost:5000}"  # 私有仓库地址

echo "========================================="
echo "  Easy JMeter Docker Images Build"
echo "========================================="

# 检查JAR包是否存在
if [ ! -f "api/target/easyJmeter-0.1.0-RELEASE.jar" ]; then
    echo "❌ JAR file not found: api/target/easyJmeter-0.1.0-RELEASE.jar"
    echo "Please build the project first: cd api && mvn clean package -DskipTests"
    exit 1
fi

echo "✅ Found JAR file: api/target/easyJmeter-0.1.0-RELEASE.jar"

# 创建构建目录
mkdir -p docker/build-context

# 复制构建文件
cp -r api/target docker/build-context/
cp docker/docker-entrypoint.sh docker/build-context/

# 构建Server镜像
echo "🐳 Building Server image..."
docker build \
    -f docker/Dockerfile.server \
    -t ${PROJECT_NAME}/server:${VERSION} \
    -t ${PROJECT_NAME}/server:latest \
    .

# 构建Agent镜像
echo "🐳 Building Agent image..."
docker build \
    -f docker/Dockerfile.agent \
    -t ${PROJECT_NAME}/agent:${VERSION} \
    -t ${PROJECT_NAME}/agent:latest \
    .

# 清理构建目录
rm -rf docker/build-context

echo "✅ Build completed successfully!"
echo ""
echo "Created images:"
echo "  - ${PROJECT_NAME}/server:${VERSION}"
echo "  - ${PROJECT_NAME}/agent:${VERSION}"
echo ""

# 推送到镜像仓库（可选）
if [ "$2" = "--push" ] && [ -n "$REGISTRY" ]; then
    echo "📤 Pushing images to registry: $REGISTRY"
    
    # 标记镜像
    docker tag ${PROJECT_NAME}/server:${VERSION} ${REGISTRY}/${PROJECT_NAME}/server:${VERSION}
    docker tag ${PROJECT_NAME}/agent:${VERSION} ${REGISTRY}/${PROJECT_NAME}/agent:${VERSION}
    
    # 推送镜像
    docker push ${REGISTRY}/${PROJECT_NAME}/server:${VERSION}
    docker push ${REGISTRY}/${PROJECT_NAME}/agent:${VERSION}
    
    echo "✅ Images pushed successfully!"
fi

echo ""
echo "Next steps:"
echo "  1. Setup JMeter volume: docker volume create jmeter-install"
echo "  2. Deploy services: ./deploy/deploy.sh"
```

### 3.2 部署脚本

```bash
#!/bin/bash
# deploy/deploy.sh

set -e

# 配置变量
PROJECT_NAME="easy-jmeter"
VERSION="${1:-latest}"
COMPOSE_FILE="docker/docker-compose.prod.yml"
ENV_FILE="${2:-.env}"

echo "========================================="
echo "  Easy JMeter Production Deployment"
echo "========================================="

# 检查环境文件
if [ ! -f "$ENV_FILE" ]; then
    echo "📝 Creating default environment file: $ENV_FILE"
    cat > $ENV_FILE << 'EOF'
# Easy JMeter Production Environment Variables

# 应用版本
VERSION=latest

# 端口配置
SERVER_HTTP_PORT=5000
SERVER_SOCKET_PORT=9000
WEB_PORT=80

# 数据库配置
MYSQL_PORT=3306
MYSQL_ROOT_PASSWORD=root

MONGO_PORT=27017
MONGO_PASSWORD=mongo2020

INFLUX_PORT=8086
INFLUX_PASSWORD=root

# MinIO配置
MINIO_PORT=9000
MINIO_CONSOLE_PORT=9001
MINIO_ACCESS_KEY=root
MINIO_SECRET_KEY=minio2023

# JMeter配置
JMETER_VERSION=5.6.2
EOF
    echo "✅ Default environment file created. Please review and modify if needed."
fi

# 检查Docker Compose文件
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "❌ Docker Compose file not found: $COMPOSE_FILE"
    exit 1
fi

# 创建JMeter数据卷（如果不存在）
if ! docker volume ls | grep -q jmeter-install; then
    echo "📦 Creating JMeter installation volume..."
    docker volume create jmeter-install
    
    echo "⬇️ Downloading and installing JMeter ${JMETER_VERSION:-5.6.2}..."
    docker run --rm \
        -v jmeter-install:/opt/apache-jmeter \
        alpine/curl:latest sh -c "
            apk add --no-cache tar gzip && \
            cd /tmp && \
            wget -O jmeter.tgz https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-${JMETER_VERSION:-5.6.2}.tgz && \
            tar -xzf jmeter.tgz --strip-components=1 -C /opt/apache-jmeter && \
            chmod +x /opt/apache-jmeter/bin/jmeter*
        "
    echo "✅ JMeter installation completed!"
fi

# 创建必要目录
mkdir -p logs/{server,agent1,agent2}
mkdir -p data/{mysql,mongodb,influxdb,minio}

# 停止现有服务
echo "🛑 Stopping existing services..."
docker-compose -f $COMPOSE_FILE --env-file $ENV_FILE down

# 启动基础服务
echo "🚀 Starting infrastructure services..."
docker-compose -f $COMPOSE_FILE --env-file $ENV_FILE up -d \
    easy-jmeter-mysql \
    easy-jmeter-mongodb \
    easy-jmeter-influxdb \
    easy-jmeter-minio

# 等待数据库就绪
echo "⏳ Waiting for databases to be ready..."
sleep 30

# 启动应用服务
echo "🚀 Starting application services..."
docker-compose -f $COMPOSE_FILE --env-file $ENV_FILE up -d \
    easy-jmeter-server

# 等待Server就绪
echo "⏳ Waiting for server to be ready..."
sleep 20

# 启动Agent服务
echo "🚀 Starting agent services..."
docker-compose -f $COMPOSE_FILE --env-file $ENV_FILE up -d \
    easy-jmeter-agent-1 \
    easy-jmeter-agent-2

# 启动Web服务（如果存在前端资源）
if [ -d "web-dist" ]; then
    echo "🌐 Starting web service..."
    docker-compose -f $COMPOSE_FILE --env-file $ENV_FILE up -d easy-jmeter-web
fi

echo ""
echo "✅ Deployment completed successfully!"
echo ""
echo "Services Status:"
docker-compose -f $COMPOSE_FILE --env-file $ENV_FILE ps

echo ""
echo "Access URLs:"
echo "  🖥️  Server API: http://localhost:${SERVER_HTTP_PORT:-5000}"
echo "  📊  Server Socket: http://localhost:${SERVER_SOCKET_PORT:-9000}"
echo "  🌐  Web Interface: http://localhost:${WEB_PORT:-80}"
echo "  💾  MinIO Console: http://localhost:${MINIO_CONSOLE_PORT:-9001}"
echo ""
echo "Log Commands:"
echo "  Server: docker logs -f easy-jmeter-server"
echo "  Agent1: docker logs -f easy-jmeter-agent-1"
echo "  Agent2: docker logs -f easy-jmeter-agent-2"
```

## 🔄 4. CI/CD流水线集成

### 4.1 GitLab CI/CD示例

```yaml
# .gitlab-ci.yml
stages:
  - build
  - package
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
  DOCKER_REGISTRY: "your-registry.com"
  PROJECT_NAME: "easy-jmeter"

# Maven构建
build:
  stage: build
  image: maven:3.8.4-openjdk-8
  script:
    - cd api
    - mvn clean package -DskipTests -Dcheckstyle.skip
  artifacts:
    paths:
      - api/target/*.jar
    expire_in: 1 hour
  cache:
    paths:
      - .m2/repository/

# Docker镜像构建
package:
  stage: package
  image: docker:20.10.16
  services:
    - docker:20.10.16-dind
  before_script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
  script:
    - chmod +x deploy/build-images.sh
    - VERSION=$CI_COMMIT_SHORT_SHA ./deploy/build-images.sh
    # 推送到镜像仓库
    - docker tag easy-jmeter/server:$CI_COMMIT_SHORT_SHA $CI_REGISTRY_IMAGE/server:$CI_COMMIT_SHORT_SHA
    - docker tag easy-jmeter/agent:$CI_COMMIT_SHORT_SHA $CI_REGISTRY_IMAGE/agent:$CI_COMMIT_SHORT_SHA
    - docker push $CI_REGISTRY_IMAGE/server:$CI_COMMIT_SHORT_SHA
    - docker push $CI_REGISTRY_IMAGE/agent:$CI_COMMIT_SHORT_SHA
  dependencies:
    - build
  only:
    - main
    - develop

# 生产环境部署
deploy:
  stage: deploy
  image: docker/compose:latest
  before_script:
    - apk add --no-cache openssh-client
    - eval $(ssh-agent -s)
    - echo "$SSH_PRIVATE_KEY" | tr -d '\r' | ssh-add -
    - mkdir -p ~/.ssh
    - chmod 700 ~/.ssh
  script:
    - scp -r docker/ $DEPLOY_USER@$DEPLOY_HOST:~/easy-jmeter/
    - scp -r deploy/ $DEPLOY_USER@$DEPLOY_HOST:~/easy-jmeter/
    - ssh $DEPLOY_USER@$DEPLOY_HOST "cd ~/easy-jmeter && VERSION=$CI_COMMIT_SHORT_SHA ./deploy/deploy.sh"
  environment:
    name: production
    url: http://$DEPLOY_HOST
  only:
    - main
  when: manual
```

### 4.2 Jenkins Pipeline示例

```groovy
// Jenkinsfile
pipeline {
    agent any
    
    environment {
        PROJECT_NAME = 'easy-jmeter'
        DOCKER_REGISTRY = 'your-registry.com'
        VERSION = "${env.BUILD_NUMBER}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build JAR') {
            agent {
                docker {
                    image 'maven:3.8.4-openjdk-8'
                    args '-v /root/.m2:/root/.m2'
                }
            }
            steps {
                dir('api') {
                    sh 'mvn clean package -DskipTests'
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'api/target/*.jar', fingerprint: true
                }
            }
        }
        
        stage('Build Docker Images') {
            steps {
                script {
                    sh 'chmod +x deploy/build-images.sh'
                    sh "VERSION=${VERSION} ./deploy/build-images.sh --push"
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                script {
                    sh """
                        ssh ${DEPLOY_USER}@${DEPLOY_HOST} '
                            cd ~/easy-jmeter &&
                            git pull origin main &&
                            VERSION=${VERSION} ./deploy/deploy.sh
                        '
                    """
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo 'Deployment completed successfully!'
        }
        failure {
            echo 'Deployment failed!'
        }
    }
}
```

## 🔧 5. 快速部署命令

### 一键部署脚本

```bash
#!/bin/bash
# quick-deploy.sh - 一键部署脚本

echo "🚀 Easy JMeter Quick Deployment Script"
echo "========================================="

# 1. 检查环境
if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose not found. Please install Docker Compose first."
    exit 1
fi

# 2. 检查JAR包
if [ ! -f "api/target/easyJmeter-0.1.0-RELEASE.jar" ]; then
    echo "📦 Building JAR package..."
    cd api && mvn clean package -DskipTests && cd ..
fi

# 3. 构建镜像
echo "🐳 Building Docker images..."
chmod +x deploy/build-images.sh
./deploy/build-images.sh

# 4. 部署服务
echo "🚀 Deploying services..."
chmod +x deploy/deploy.sh
./deploy/deploy.sh

echo ""
echo "✅ Quick deployment completed!"
echo "🌐 Access: http://localhost:5000"
```

## 📋 6. 使用说明

### 6.1 准备工作
```bash
# 1. 克隆项目
git clone <your-repo>
cd easy-jmeter

# 2. 构建JAR包（如果还没有）
cd api
mvn clean package -DskipTests
cd ..

# 3. 创建Docker文件
mkdir -p docker deploy
# 复制上述Dockerfile和脚本内容
```

### 6.2 构建镜像
```bash
# 使用构建脚本
chmod +x deploy/build-images.sh
./deploy/build-images.sh v1.0.0

# 或手动构建
docker build -f docker/Dockerfile.server -t easy-jmeter/server:latest .
docker build -f docker/Dockerfile.agent -t easy-jmeter/agent:latest .
```

### 6.3 部署服务
```bash
# 使用部署脚本
chmod +x deploy/deploy.sh
./deploy/deploy.sh

# 或手动部署
docker-compose -f docker/docker-compose.prod.yml up -d
```

### 6.4 验证部署
```bash
# 检查服务状态
docker ps

# 查看日志
docker logs easy-jmeter-server
docker logs easy-jmeter-agent-1

# 测试API
curl http://localhost:5000/actuator/health
```

## 🎯 7. 生产环境优化建议

### 性能优化
- 根据实际负载调整JVM内存参数
- 使用SSD存储挂载日志和数据目录
- 配置合适的Docker资源限制

### 安全优化
- 使用非root用户运行容器
- 配置防火墙和网络安全组
- 定期更新基础镜像

### 监控告警
- 集成Prometheus + Grafana监控
- 配置日志聚合（如ELK Stack）
- 设置健康检查和自动重启

---

**通过本指南，您可以从JAR包快速构建Docker镜像并实现生产级的容器化部署！** 🎉
