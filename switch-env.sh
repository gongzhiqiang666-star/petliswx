#!/bin/bash

# 快速切换微信小程序的网络环境配置
# 用法: ./switch-env.sh [simulator|device]

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
CONFIG_FILE="$PROJECT_ROOT/miniprogram/config/config.js"

# 获取当前局域网IP
LOCAL_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null)

# 检查是否提供了参数
if [ -z "$1" ]; then
    echo "❌ 请指定目标环境"
    echo ""
    echo "用法: $0 [simulator|device]"
    echo ""
    echo "示例:"
    echo "  $0 simulator   # 切换到模拟器环境 (localhost)"
    echo "  $0 device      # 切换到真机环境 (局域网IP: $LOCAL_IP)"
    echo ""
    exit 1
fi

# 切换到模拟器环境
if [ "$1" = "simulator" ]; then
    echo "🔄 切换到模拟器环境..."
    sed -i '' "s|return 'http://.*:8082/api'|return 'http://localhost:8082/api'|" "$CONFIG_FILE"

    if grep -q "localhost:8082" "$CONFIG_FILE"; then
        echo "✅ 已切换到模拟器环境"
        echo "   baseURL: http://localhost:8082/api"
    else
        echo "❌ 切换失败"
        exit 1
    fi

# 切换到真机环境
elif [ "$1" = "device" ]; then
    echo "🔄 切换到真机环境..."
    sed -i '' "s|return 'http://.*:8082/api'|return 'http://$LOCAL_IP:8082/api'|" "$CONFIG_FILE"

    if grep -q "$LOCAL_IP:8082" "$CONFIG_FILE"; then
        echo "✅ 已切换到真机环境"
        echo "   baseURL: http://$LOCAL_IP:8082/api"
        echo ""
        echo "📱 请确保："
        echo "   1. 手机和电脑在同一Wi-Fi"
        echo "   2. 后端服务正在运行"
        echo "   3. 在手机浏览器测试: http://$LOCAL_IP:8082/api/auth/hospital/login"
    else
        echo "❌ 切换失败"
        exit 1
    fi

else
    echo "❌ 未知的环境: $1"
    echo "   支持的环境: simulator, device"
    exit 1
fi

echo ""
echo "📋 当前配置:"
grep "return 'http://" "$CONFIG_FILE" | grep -v "^//"
