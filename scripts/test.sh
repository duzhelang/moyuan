#!/bin/bash

# 测试运行脚本
# 使用方法: ./scripts/test.sh [选项]
# 选项:
#   all     - 运行所有测试
#   frontend - 只运行前端测试
#   backend  - 只运行后端测试
#   coverage - 生成覆盖率报告

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印帮助信息
show_help() {
    echo -e "${GREEN}墨渊项目测试运行脚本${NC}"
    echo ""
    echo "使用方法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  all       运行所有测试"
    echo "  frontend  只运行前端测试"
    echo "  backend   只运行后端测试"
    echo "  coverage  生成覆盖率报告"
    echo "  help      显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 all"
    echo "  $0 frontend"
    echo "  $0 backend"
    echo "  $0 coverage"
}

# 运行前端测试
run_frontend_tests() {
    echo -e "${YELLOW}运行前端测试...${NC}"
    cd frontend
    
    # 检查是否安装了依赖
    if [ ! -d "node_modules" ]; then
        echo -e "${YELLOW}安装前端依赖...${NC}"
        pnpm install
    fi
    
    # 运行类型检查
    echo -e "${YELLOW}运行类型检查...${NC}"
    pnpm type-check
    
    # 运行代码规范检查
    echo -e "${YELLOW}运行代码规范检查...${NC}"
    pnpm lint
    
    # 运行单元测试
    echo -e "${YELLOW}运行单元测试...${NC}"
    pnpm test:run
    
    cd ..
    echo -e "${GREEN}前端测试完成!${NC}"
}

# 运行后端测试
run_backend_tests() {
    echo -e "${YELLOW}运行后端测试...${NC}"
    cd sc-moyuan-backend
    
    # 运行单元测试
    echo -e "${YELLOW}运行单元测试...${NC}"
    mvn test
    
    cd ..
    echo -e "${GREEN}后端测试完成!${NC}"
}

# 生成覆盖率报告
generate_coverage() {
    echo -e "${YELLOW}生成覆盖率报告...${NC}"
    
    # 前端覆盖率
    echo -e "${YELLOW}生成前端覆盖率报告...${NC}"
    cd frontend
    pnpm test:coverage
    cd ..
    
    # 后端覆盖率
    echo -e "${YELLOW}生成后端覆盖率报告...${NC}"
    cd sc-moyuan-backend
    mvn test jacoco:report
    cd ..
    
    echo -e "${GREEN}覆盖率报告生成完成!${NC}"
    echo -e "${GREEN}前端覆盖率报告: frontend/coverage/index.html${NC}"
    echo -e "${GREEN}后端覆盖率报告: sc-moyuan-backend/target/site/jacoco/index.html${NC}"
}

# 运行所有测试
run_all_tests() {
    echo -e "${GREEN}运行所有测试...${NC}"
    run_frontend_tests
    run_backend_tests
    echo -e "${GREEN}所有测试完成!${NC}"
}

# 主逻辑
case "$1" in
    all)
        run_all_tests
        ;;
    frontend)
        run_frontend_tests
        ;;
    backend)
        run_backend_tests
        ;;
    coverage)
        generate_coverage
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo -e "${RED}错误: 未知选项 '$1'${NC}"
        echo ""
        show_help
        exit 1
        ;;
esac