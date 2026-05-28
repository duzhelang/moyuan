@echo off
echo ========================================
echo 古今诗话——墨渊 现代诗词管理系统启动脚本
echo ========================================
echo.

echo [1/4] 检查Java环境...
java -version
if %errorlevel% neq 0 (
    echo 错误: 未找到Java环境，请安装Java 17+
    pause
    exit /b 1
)
echo.

echo [2/4] 检查Maven环境...
mvn -version
if %errorlevel% neq 0 (
    echo 错误: 未找到Maven环境，请安装Maven 3.8+
    pause
    exit /b 1
)
echo.

echo [3/4] 启动后端服务...
echo 正在启动Spring Boot应用...
echo 应用地址: http://localhost:8080
echo API文档: http://localhost:8080/api
echo 测试页面: http://localhost:8080/test.html
echo.
echo 按Ctrl+C停止服务
echo.

cd backend
mvn spring-boot:run

echo.
echo [4/4] 服务已停止
pause
