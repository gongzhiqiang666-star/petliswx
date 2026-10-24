#!/bin/bash

# PETLIS后端测试执行脚本
# 用于运行所有单元测试和集成测试

set -e

echo "======================================"
echo "PETLIS Backend Test Runner"
echo "======================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 进入项目目录
cd "$(dirname "$0")"

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}错误: Maven未安装，请先安装Maven${NC}"
    exit 1
fi

echo -e "${YELLOW}正在运行测试...${NC}"
echo ""

# 运行测试
echo "1. 运行所有测试..."
mvn clean test

# 检查测试结果
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ 所有测试通过！${NC}"
    echo ""
    echo "测试报告位置:"
    echo "  - HTML报告: target/surefire-reports/index.html"
    echo "  - XML报告: target/surefire-reports/"
    echo ""
else
    echo ""
    echo -e "${RED}✗ 测试失败，请查看上方错误信息${NC}"
    exit 1
fi
