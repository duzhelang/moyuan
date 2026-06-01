@echo off
setlocal enabledelayedexpansion

REM 测试运行脚本 (Windows版本)
REM 使用方法: scripts\test.bat [选项]
REM 选项:
REM   all     - 运行所有测试
REM   frontend - 只运行前端测试
REM   backend  - 只运行后端测试
REM   coverage - 生成覆盖率报告

if "%1"=="" goto help
if "%1"=="all" goto all
if "%1"=="frontend" goto frontend
if "%1"=="backend" goto backend
if "%1"=="coverage" goto coverage
if "%1"=="help" goto help
if "%1"=="--help" goto help
if "%1"=="-h" goto help

echo 错误: 未知选项 '%1'
echo.
goto help

:help
echo 墨渊项目测试运行脚本
echo.
echo 使用方法: %0 [选项]
echo.
echo 选项:
echo   all       运行所有测试
echo   frontend  只运行前端测试
echo   backend   只运行后端测试
echo   coverage  生成覆盖率报告
echo   help      显示帮助信息
echo.
echo 示例:
echo   %0 all
echo   %0 frontend
echo   %0 backend
echo   %0 coverage
goto end

:frontend
echo 运行前端测试...
cd frontend

REM 检查是否安装了依赖
if not exist "node_modules" (
    echo 安装前端依赖...
    call pnpm install
)

REM 运行类型检查
echo 运行类型检查...
call pnpm type-check
if errorlevel 1 (
    echo 类型检查失败!
    cd ..
    exit /b 1
)

REM 运行代码规范检查
echo 运行代码规范检查...
call pnpm lint
if errorlevel 1 (
    echo 代码规范检查失败!
    cd ..
    exit /b 1
)

REM 运行单元测试
echo 运行单元测试...
call pnpm test:run
if errorlevel 1 (
    echo 前端测试失败!
    cd ..
    exit /b 1
)

cd ..
echo 前端测试完成!
goto end

:backend
echo 运行后端测试...
cd sc-moyuan-backend

REM 运行单元测试
echo 运行单元测试...
call mvn test
if errorlevel 1 (
    echo 后端测试失败!
    cd ..
    exit /b 1
)

cd ..
echo 后端测试完成!
goto end

:coverage
echo 生成覆盖率报告...

REM 前端覆盖率
echo 生成前端覆盖率报告...
cd frontend
call pnpm test:coverage
cd ..

REM 后端覆盖率
echo 生成后端覆盖率报告...
cd sc-moyuan-backend
call mvn test jacoco:report
cd ..

echo 覆盖率报告生成完成!
echo 前端覆盖率报告: frontend\coverage\index.html
echo 后端覆盖率报告: sc-moyuan-backend\target\site\jacoco\index.html
goto end

:all
echo 运行所有测试...
call %0 frontend
if errorlevel 1 exit /b 1
call %0 backend
if errorlevel 1 exit /b 1
echo 所有测试完成!
goto end

:end