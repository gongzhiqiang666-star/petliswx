#!/bin/bash

# PETLIS 后端测试快速运行脚本

echo "======================================"
echo "PETLIS Backend Test Quick Runner"
echo "======================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 进入项目目录
cd "$(dirname "$0")"

echo -e "${YELLOW}请选择测试选项：${NC}"
echo ""
echo "1) 编译项目"
echo "2) 运行所有测试"
echo "3) 运行PdfServiceTest（无需数据库）"
echo "4) 查看测试报告"
echo "5) 清理并重新编译"
echo "6) 退出"
echo ""

read -p "请输入选项 (1-6): " choice

case $choice in
    1)
        echo -e "${GREEN}正在编译项目...${NC}"
        mvn clean compile
        ;;

    2)
        echo -e "${GREEN}正在运行所有测试...${NC}"
        mvn test
        ;;

    3)
        echo -e "${GREEN}正在运行PdfServiceTest...${NC}"
        mvn test -Dtest=PdfServiceTest
        ;;

    4)
        echo -e "${GREEN}测试文档：${NC}"
        echo "  - TEST_GUIDE.md - 详细测试指南"
        echo "  - TESTING_SUMMARY.md - 测试总结"
        echo "  - TEST_COMPLETION_REPORT.md - 执行报告"
        echo "  - FINAL_TEST_REPORT.md - 最终报告"
        echo ""
        echo "测试报告位置: target/surefire-reports/"
        ;;

    5)
        echo -e "${GREEN}正在清理并重新编译...${NC}"
        mvn clean compile
        ;;

    6)
        echo "退出"
        exit 0
        ;;

    *)
        echo "无效选项"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}完成！${NC}"
