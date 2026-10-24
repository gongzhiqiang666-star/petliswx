#!/bin/bash
# PETLIS Backend - 重启服务脚本

echo "======================================"
echo "PETLIS 后端服务重启"
echo "======================================"
echo ""

# 停止现有服务
echo "1. 停止现有服务..."
pkill -f "petlis-backend" || echo "没有运行中的服务"

# 等待进程完全停止
sleep 2

# 进入项目目录
cd "$(dirname "$0")" || exit 1

# 重新编译（确保新代码生效）
echo "2. 编译项目..."
mvn clean compile -DskipTests

# 启动服务
echo "3. 启动服务..."
nohup mvn spring-boot:run > logs/app.log 2>&1 &

# 等待服务启动
echo "4. 等待服务启动..."
sleep 10

# 检查服务状态
echo "5. 检查服务状态..."
if curl -s http://localhost:8082/api/actuator/health > /dev/null; then
    echo "✓ 服务启动成功！"
    echo ""
    echo "查看日志："
    echo "  tail -f logs/app.log"
    echo ""
    echo "健康检查："
    echo "  curl http://localhost:8082/api/actuator/health"
else
    echo "✗ 服务启动失败，请查看日志："
    echo "  tail -f logs/app.log"
fi
