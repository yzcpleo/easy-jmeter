@echo off
REM Easy JMeter Client 启动脚本 (Windows版本)
REM 用于在宿主机上部署Client模式，连接宿主机的服务

echo =========================================
echo   Easy JMeter Client Deployment
echo =========================================

REM 检查必要的目录
if not exist "logs" mkdir logs
if not exist "jmeter-results" mkdir jmeter-results
if not exist "temp" mkdir temp

REM 检查配置文件
if not exist "docker\application-client.yml" (
    echo Error: docker\application-client.yml not found!
    pause
    exit /b 1
)

REM 检查JAR包
if not exist "api\target\easyJmeter-*.jar" (
    echo Error: JAR file not found in api\target\ directory!
    echo Please build the project first: mvn clean package
    pause
    exit /b 1
)

echo Building Easy JMeter Client image...
docker build -f docker/Dockerfile.client -t easyjmeter-client:latest .

echo Starting Easy JMeter Client container...
docker-compose -f docker-compose.client.yml up -d

echo =========================================
echo   Client Deployment Complete!
echo =========================================
echo Container Status:
docker-compose -f docker-compose.client.yml ps

echo.
echo Access Information:
echo - API Endpoint: http://localhost:5000
echo - Health Check: http://localhost:5000/actuator/health
echo - Logs: ./logs/
echo - Results: ./jmeter-results/

echo.
echo To view logs:
echo docker logs -f easyjmeter-client

echo.
echo To stop client:
echo docker-compose -f docker-compose.client.yml down

echo.
echo To restart client:
echo docker-compose -f docker-compose.client.yml restart

pause