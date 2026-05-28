@echo off
echo ========================================
echo 古今诗话——墨渊 数据库初始化脚本
echo ========================================
echo.

echo [1/3] 检查MySQL环境...
mysql --version
if %errorlevel% neq 0 (
    echo 错误: 未找到MySQL环境，请安装MySQL 8.0+
    pause
    exit /b 1
)
echo.

echo [2/3] 创建数据库...
echo 正在创建数据库 moyuan...
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS moyuan DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if %errorlevel% neq 0 (
    echo 错误: 数据库创建失败
    pause
    exit /b 1
)
echo 数据库创建成功
echo.

echo [3/3] 初始化表结构和数据...
echo 正在执行初始化脚本...
mysql -u root -p moyuan < backend\src\main\resources\db\init.sql
if %errorlevel% neq 0 (
    echo 错误: 初始化脚本执行失败
    pause
    exit /b 1
)
echo 数据库初始化完成
echo.

echo ========================================
echo 数据库初始化成功！
echo 数据库名: moyuan
echo 表: poem, poet, dynasty, category
echo 现代诗词数据已导入
echo ========================================
pause
