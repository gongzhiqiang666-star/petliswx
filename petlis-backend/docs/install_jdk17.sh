#!/bin/bash

  # PETLIS项目 - JDK 17 安装脚本
  # 适用于Mac mini (Intel和Apple Silicon)

  echo "=== PETLIS JDK 17 安装脚本 ==="
  echo ""

  # 检查Homebrew
  if ! command -v brew &> /dev/null
  then
      echo "📦 安装Homebrew..."
      /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  else
      echo "✅ Homebrew已安装: $(brew --version)"
  fi

  echo ""
  echo "📦 安装OpenJDK 17..."
  brew install openjdk@17

  echo ""
  echo "🔧 配置环境变量..."
  # 检测shell类型
  if [ -n "$ZSH_VERSION" ]; then
      PROFILE=~/.zshrc
  elif [ -n "$BASH_VERSION" ]; then
      PROFILE=~/.bash_profile
  else
      PROFILE=~/.profile
  fi

  # 添加环境变量（如果不存在）
  if ! grep -q "openjdk@17" "$PROFILE" 2>/dev/null; then
      echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> "$PROFILE"
      echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@17"' >> "$PROFILE"
      echo "✅ 环境变量已添加到 $PROFILE"
  else
      echo "✅ 环境变量已存在"
  fi

  # 设置系统默认JDK
  sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk 2>/dev/null
  echo "✅ 系统默认JDK已设置为Java 17"

  echo ""
  echo "📋 安装摘要:"
  echo "  - Java版本: OpenJDK 17"
  echo "  - 安装路径: /opt/homebrew/opt/openjdk@17"
  echo "  - JAVA_HOME: /opt/homebrew/opt/openjdk@17"
  echo "  - 配置文件: $PROFILE"
  echo ""
  echo "⚠️  请运行以下命令使环境变量生效:"
  echo "   source $PROFILE"
  echo ""
  echo "🔍 验证安装:"
  echo "   java -version"
  echo ""

  # 重新加载配置
  source "$PROFILE"

  # 验证安装
  echo "当前Java版本:"
  java -version

  echo ""
  echo "✅ JDK 17 安装完成！"


 #使用方法：
  # 1. 复制脚本内容
  # 2. 保存为 install_jdk17.sh
  # 3. 运行脚本
  # bash install_jdk17.sh


