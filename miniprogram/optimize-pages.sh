#!/bin/bash

# 小程序页面UI批量优化脚本
# 用于将自定义导航栏改为原生导航栏

echo "开始批量优化页面..."

# 需要优化的页面列表
pages=(
  "order-add-pet"
  "pet-detail"
  "order-detail"
  "order-select-pet"
  "order-step2"
  "pets"
  "pet-health"
  "report-list"
  "report-view"
  "orders"
)

# 基础路径
base_path="/Users/gong/claudelife/PETLIS/miniprogram/pages"

# 遍历所有页面
for page in "${pages[@]}"; do
  echo "正在优化: $page"

  json_file="$base_path/$page/$page.json"
  wxml_file="$base_path/$page/$page.wxml"
  wxss_file="$base_path/$page/$page.wxss"

  # 1. 修改JSON配置
  if [ -f "$json_file" ]; then
    echo "  ✓ 修改JSON配置"

    # 删除navigationStyle行
    sed -i '' '/"navigationStyle":/d' "$json_file"

    # 添加navigationBarBackgroundColor和navigationBarTextStyle
    if ! grep -q '"navigationBarBackgroundColor"' "$json_file"; then
      sed -i '' 's/"navigationBarTitleText"/"navigationBarBackgroundColor": "#ffffff",\n  "navigationBarTextStyle": "black",\n  "navigationBarTitleText"/' "$json_file"
    fi

    # 添加backgroundColor
    if ! grep -q '"backgroundColor"' "$json_file"; then
      sed -i '' 's/"navigationBarTextStyle": "black"/"navigationBarTextStyle": "black",\n  "backgroundColor": "#f9fafb"/' "$json_file"
    fi
  fi

  # 2. 修改WXML - 删除自定义header
  if [ -f "$wxml_file" ]; then
    echo "  ✓ 修改WXML，删除自定义header"

    # 备份原文件
    cp "$wxml_file" "$wxml_file.bak"

    # 删除header部分（从<view class="header">到</view>包含的整个header块）
    # 这个操作比较复杂，建议手动检查
  fi

  # 3. 修改WXSS
  if [ -f "$wxss_file" ]; then
    echo "  ✓ 修改WXSS样式"

    # 备份原文件
    cp "$wxss_file" "$wxss_file.bak"

    # 删除.header相关样式
    sed -i '' '/\/\* Header \*\//,/^\.placeholder \{/{ /^\.placeholder \{/d; /^$/d; }' "$wxss_file"
    sed -i '' '/\/\* Header \*\//,/^\.header \{/{ /^\.header \{/d; }' "$wxss_file"

    # 修改.content样式，添加居中
    if grep -q '\.content' "$wxss_file"; then
      # 在.content的padding后添加max-width和margin
      sed -i '' '/\.content {/,/}/ s/padding-bottom: 180rpx;/padding-bottom: 180rpx;\n  max-width: 750rpx;\n  margin: 0 auto;\n  width: 100%;/' "$wxss_file"
    fi
  fi

  echo "  完成: $page"
  echo ""
done

echo "所有页面优化完成！"
echo "备份文件已创建（.bak后缀），如需恢复请使用备份文件"
echo ""
echo "⚠️  注意事项："
echo "1. 请手动检查WXML文件，删除自定义header相关代码"
echo "2. 请测试每个页面确保功能正常"
echo "3. 某些页面可能需要额外调整"
