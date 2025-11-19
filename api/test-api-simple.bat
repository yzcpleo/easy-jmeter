@echo off
chcp 65001 >nul
echo ===== API测试脚本 =====
echo.

echo [1] 测试服务是否运行...
curl -s http://localhost:5000/cms/user/information >nul 2>&1
if %errorlevel% equ 0 (
    echo 服务运行正常
) else (
    echo 服务未运行或无法访问
    pause
    exit /b 1
)

echo.
echo [2] 测试登录接口...
curl -X POST http://localhost:5000/cms/user/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"root\",\"password\":\"123456\"}" ^
  -o login-response.json 2>nul

if exist login-response.json (
    echo 登录请求已发送，响应已保存到 login-response.json
    type login-response.json
) else (
    echo 登录请求失败
)

echo.
echo ===== 测试完成 =====
pause

