#!/bin/sh

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
            echo "Database not ready, waiting 2 seconds..."
            sleep 2
        done
        echo "Database is ready!"
    fi
    
    # 等待MongoDB服务就绪
    if [ -n "$MONGO_HOST" ]; then
        echo "Waiting for MongoDB at $MONGO_HOST:${MONGO_PORT:-27017}..."
        while ! nc -z $MONGO_HOST ${MONGO_PORT:-27017}; do
            echo "MongoDB not ready, waiting 2 seconds..."
            sleep 2
        done
        echo "MongoDB is ready!"
    fi
    
    # 等待InfluxDB服务就绪
    if [ -n "$INFLUX_HOST" ]; then
        echo "Waiting for InfluxDB at $INFLUX_HOST:${INFLUX_PORT:-8086}..."
        while ! nc -z $INFLUX_HOST ${INFLUX_PORT:-8086}; do
            echo "InfluxDB not ready, waiting 2 seconds..."
            sleep 2
        done
        echo "InfluxDB is ready!"
    fi
    
    # 等待MinIO服务就绪
    if [ -n "$MINIO_HOST" ]; then
        echo "Waiting for MinIO at $MINIO_HOST:${MINIO_PORT:-9000}..."
        while ! nc -z $MINIO_HOST ${MINIO_PORT:-9000}; do
            echo "MinIO not ready, waiting 2 seconds..."
            sleep 2
        done
        echo "MinIO is ready!"
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
        
        # 验证JMeter可执行性
        if [ -f "$JMETER_HOME/bin/jmeter" ]; then
            chmod +x "$JMETER_HOME/bin/jmeter"
            echo "JMeter executable found and permissions set"
        else
            echo "Warning: JMeter executable not found at $JMETER_HOME/bin/jmeter"
        fi
    fi
    
    # 等待Server服务就绪
    if [ -n "$SERVER_HOST" ]; then
        echo "Waiting for server at $SERVER_HOST:${SERVER_PORT:-9000}..."
        while ! nc -z $SERVER_HOST ${SERVER_PORT:-9000}; do
            echo "Server not ready, waiting 5 seconds..."
            sleep 5
        done
        echo "Server is ready!"
    fi
fi

# 设置动态配置
if [ -n "$SOCKET_CLIENT_SERVER_URL" ]; then
    export APP_OPTS="${APP_OPTS} -Dsocket.client.serverUrl=${SOCKET_CLIENT_SERVER_URL}"
fi

# 数据库连接配置
if [ -n "$DB_HOST" ]; then
    DB_URL="jdbc:mysql://${DB_HOST}:${DB_PORT:-3306}/${DB_NAME:-easy-jmeter}?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF8"
    export APP_OPTS="${APP_OPTS} -Dspring.datasource.url=${DB_URL}"
    export APP_OPTS="${APP_OPTS} -Dspring.datasource.username=${DB_USER:-root}"
    export APP_OPTS="${APP_OPTS} -Dspring.datasource.password=${DB_PASSWORD:-root}"
fi

# MongoDB连接配置
if [ -n "$MONGO_HOST" ]; then
    export APP_OPTS="${APP_OPTS} -Dspring.data.mongodb.host=${MONGO_HOST}"
    export APP_OPTS="${APP_OPTS} -Dspring.data.mongodb.port=${MONGO_PORT:-27017}"
    export APP_OPTS="${APP_OPTS} -Dspring.data.mongodb.database=${MONGO_DB:-easyJmeter}"
    if [ -n "$MONGO_USER" ]; then
        export APP_OPTS="${APP_OPTS} -Dspring.data.mongodb.username=${MONGO_USER}"
        export APP_OPTS="${APP_OPTS} -Dspring.data.mongodb.password=${MONGO_PASSWORD}"
        export APP_OPTS="${APP_OPTS} -Dspring.data.mongodb.authentication-database=admin"
    fi
fi

# InfluxDB连接配置
if [ -n "$INFLUX_HOST" ]; then
    export APP_OPTS="${APP_OPTS} -Dspring.influx.url=http://${INFLUX_HOST}:${INFLUX_PORT:-8086}"
    export APP_OPTS="${APP_OPTS} -Dspring.influx.user=${INFLUX_USER:-root}"
    export APP_OPTS="${APP_OPTS} -Dspring.influx.password=${INFLUX_PASSWORD:-root}"
    export APP_OPTS="${APP_OPTS} -Dspring.influx.database=${INFLUX_DB:-easyJmeter}"
fi

# MinIO连接配置
if [ -n "$MINIO_HOST" ]; then
    export APP_OPTS="${APP_OPTS} -Dminio.endpoint=http://${MINIO_HOST}:${MINIO_PORT:-9000}"
    export APP_OPTS="${APP_OPTS} -Dminio.accessKey=${MINIO_ACCESS_KEY:-root}"
    export APP_OPTS="${APP_OPTS} -Dminio.secretKey=${MINIO_SECRET_KEY:-minio2023}"
    export APP_OPTS="${APP_OPTS} -Dminio.bucketName=${MINIO_BUCKET:-dev}"
fi

# 打印最终的启动命令
echo "========================================="
echo "Java Options: $JAVA_OPTS"
echo "App Options: $APP_OPTS"
echo "JAR File: easyJmeter.jar"
echo "========================================="

# 启动应用
exec java $JAVA_OPTS $APP_OPTS -jar easyJmeter.jar
