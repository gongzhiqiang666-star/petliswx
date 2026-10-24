import React, { useState, useRef } from 'react';
import { 
  Home, PawPrint, ShoppingBag, User, Download, CheckCircle, AlertCircle
} from 'lucide-react';
import { PageHeader } from '../components/PageHeader';
import { GradientButton } from '../components/GradientButton';

interface IconExportPageProps {
  onNavigate: (page: any) => void;
}

interface TabBarIcon {
  name: string;
  fileName: string;
  component: React.ComponentType<any>;
}

const tabBarIcons: TabBarIcon[] = [
  { name: '首页', fileName: 'home', component: Home },
  { name: '宠物', fileName: 'pet', component: PawPrint },
  { name: '订单', fileName: 'order', component: ShoppingBag },
  { name: '我的', fileName: 'profile', component: User },
];

export function IconExportPage({ onNavigate }: IconExportPageProps) {
  const [downloadStatus, setDownloadStatus] = useState<{[key: string]: 'idle' | 'downloading' | 'success' | 'error'}>({});
  const canvasRef = useRef<HTMLCanvasElement>(null);

  // 将SVG转换为PNG并下载
  const downloadIconAsPNG = async (
    iconName: string,
    fileName: string,
    color: string,
    suffix: string
  ) => {
    const statusKey = `${fileName}-${suffix}`;
    setDownloadStatus(prev => ({ ...prev, [statusKey]: 'downloading' }));

    try {
      const canvas = canvasRef.current;
      if (!canvas) throw new Error('Canvas not found');

      const ctx = canvas.getContext('2d');
      if (!ctx) throw new Error('Canvas context not found');

      const size = 81;
      canvas.width = size;
      canvas.height = size;

      // 清空画布
      ctx.clearRect(0, 0, size, size);

      // 获取对应的SVG元素
      const svgElement = document.getElementById(`icon-${fileName}-${suffix}`) as unknown as SVGSVGElement;
      if (!svgElement) throw new Error('SVG element not found');

      // 克隆SVG并设置正确的尺寸
      const clonedSvg = svgElement.cloneNode(true) as SVGSVGElement;
      clonedSvg.setAttribute('width', size.toString());
      clonedSvg.setAttribute('height', size.toString());
      clonedSvg.style.color = color;

      // 序列化SVG
      const svgData = new XMLSerializer().serializeToString(clonedSvg);
      const svgBlob = new Blob([svgData], { type: 'image/svg+xml;charset=utf-8' });
      const url = URL.createObjectURL(svgBlob);

      // 加载图片
      const img = new Image();
      
      await new Promise<void>((resolve, reject) => {
        img.onload = () => {
          // 绘制到canvas
          ctx.clearRect(0, 0, size, size);
          ctx.drawImage(img, 0, 0, size, size);
          
          // 转换为PNG并下载
          canvas.toBlob((blob) => {
            if (blob) {
              const downloadUrl = URL.createObjectURL(blob);
              const link = document.createElement('a');
              link.href = downloadUrl;
              link.download = `${fileName}${suffix === 'active' ? '-active' : ''}.png`;
              link.click();
              URL.revokeObjectURL(downloadUrl);
              setDownloadStatus(prev => ({ ...prev, [statusKey]: 'success' }));
              resolve();
            } else {
              reject(new Error('Failed to create blob'));
            }
          }, 'image/png');
          
          URL.revokeObjectURL(url);
        };

        img.onerror = () => {
          URL.revokeObjectURL(url);
          reject(new Error('Failed to load image'));
        };

        img.src = url;
      });
    } catch (error) {
      console.error('Error downloading icon:', error);
      setDownloadStatus(prev => ({ ...prev, [statusKey]: 'error' }));
    }
  };

  // 下载单个图标的两种状态
  const downloadSingleIcon = async (icon: TabBarIcon) => {
    const unselectedColor = '#8C8C8D';
    const selectedColor = '#615FFF';

    // 下载未选中状态
    await downloadIconAsPNG(icon.name, icon.fileName, unselectedColor, 'normal');
    await new Promise(resolve => setTimeout(resolve, 500));

    // 下载选中状态
    await downloadIconAsPNG(icon.name, icon.fileName, selectedColor, 'active');
    await new Promise(resolve => setTimeout(resolve, 500));
  };

  // 下载所有TabBar图标
  const downloadAllIcons = async () => {
    const confirmed = window.confirm(
      '即将下载微信小程序TabBar图标\n\n' +
      '规格说明：\n' +
      '• 尺寸：81px × 81px\n' +
      '• 格式：PNG 透明背景\n' +
      '• 描边：2.5px\n' +
      '• 未选中色：#8C8C8D（微信标准灰）\n' +
      '• 选中色：#615FFF（主题紫色）\n\n' +
      '将下载 8 个文件\n\n' +
      '是否继续？'
    );

    if (!confirmed) return;

    for (const icon of tabBarIcons) {
      await downloadSingleIcon(icon);
    }

    alert('所有图标下载完成！\n\n已下载 8 个 PNG 文件，可直接用于小程序开发。');
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      <PageHeader title="图标导出" onBack={() => onNavigate('home')} />

      {/* 隐藏的Canvas用于渲染 */}
      <canvas ref={canvasRef} style={{ display: 'none' }} />

      {/* 说明区域 */}
      <div className="px-4 pt-4">
        <div className="bg-gradient-to-br from-indigo-500 to-purple-600 rounded-2xl p-5 text-white">
          <div className="text-lg mb-2">微信小程序TabBar图标</div>
          <div className="text-sm opacity-90 space-y-1">
            <div>✓ 尺寸：81px × 81px</div>
            <div>✓ 格式：PNG 透明背景</div>
            <div>✓ 描边：2.5px</div>
            <div>✓ 未选中：#8C8C8D（微信标准灰）</div>
            <div>✓ 选中：#615FFF（主题紫色）</div>
          </div>
        </div>
      </div>

      {/* 一键下载按钮 */}
      <div className="px-4 pt-4">
        <GradientButton
          variant="purple"
          size="lg"
          onClick={downloadAllIcons}
          className="w-full flex items-center justify-center gap-2"
        >
          <Download className="w-5 h-5" />
          一键下载全部 (8个PNG文件)
        </GradientButton>
      </div>

      {/* 图标预览和单独下载 */}
      <div className="px-4 pt-6">
        <div className="text-sm text-gray-500 mb-3">图标预览（点击单独下载）</div>
        
        <div className="space-y-4">
          {tabBarIcons.map((icon) => (
            <div key={icon.fileName} className="bg-white rounded-2xl p-4">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <div className="text-base">{icon.name}</div>
                  <div className="text-xs text-gray-500 mt-1">
                    {icon.fileName}.png / {icon.fileName}-active.png
                  </div>
                </div>
                <button
                  onClick={() => downloadSingleIcon(icon)}
                  className="px-4 py-2 bg-indigo-50 text-indigo-600 rounded-xl hover:bg-indigo-100 transition-colors text-sm flex items-center gap-2"
                >
                  <Download className="w-4 h-4" />
                  下载
                </button>
              </div>

              <div className="grid grid-cols-2 gap-4">
                {/* 未选中状态预览 */}
                <div className="border border-gray-200 rounded-xl p-6">
                  <div className="flex flex-col items-center gap-3">
                    {/* 隐藏的SVG用于导出 */}
                    <div style={{ display: 'none' }}>
                      <icon.component
                        id={`icon-${icon.fileName}-normal`}
                        size={81}
                        strokeWidth={2.5}
                        style={{ color: '#8C8C8D' }}
                      />
                    </div>
                    
                    {/* 可见的预览SVG */}
                    <div className="w-20 h-20 flex items-center justify-center bg-gray-50 rounded-xl">
                      <icon.component
                        size={48}
                        strokeWidth={2.5}
                        style={{ color: '#8C8C8D' }}
                      />
                    </div>
                    
                    <div className="text-center">
                      <div className="text-xs text-gray-500">未选中</div>
                      <div className="text-xs text-gray-400 mt-1">#8C8C8D</div>
                    </div>
                    
                    {downloadStatus[`${icon.fileName}-normal`] === 'success' && (
                      <CheckCircle className="w-4 h-4 text-green-500" />
                    )}
                    {downloadStatus[`${icon.fileName}-normal`] === 'error' && (
                      <AlertCircle className="w-4 h-4 text-red-500" />
                    )}
                  </div>
                </div>

                {/* 选中状态预览 */}
                <div className="border border-gray-200 rounded-xl p-6">
                  <div className="flex flex-col items-center gap-3">
                    {/* 隐藏的SVG用于导出 */}
                    <div style={{ display: 'none' }}>
                      <icon.component
                        id={`icon-${icon.fileName}-active`}
                        size={81}
                        strokeWidth={2.5}
                        style={{ color: '#615FFF' }}
                      />
                    </div>
                    
                    {/* 可见的预览SVG */}
                    <div className="w-20 h-20 flex items-center justify-center bg-indigo-50 rounded-xl">
                      <icon.component
                        size={48}
                        strokeWidth={2.5}
                        style={{ color: '#615FFF' }}
                      />
                    </div>
                    
                    <div className="text-center">
                      <div className="text-xs text-gray-500">选中</div>
                      <div className="text-xs text-gray-400 mt-1">#615FFF</div>
                    </div>
                    
                    {downloadStatus[`${icon.fileName}-active`] === 'success' && (
                      <CheckCircle className="w-4 h-4 text-green-500" />
                    )}
                    {downloadStatus[`${icon.fileName}-active`] === 'error' && (
                      <AlertCircle className="w-4 h-4 text-red-500" />
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 使用说明 */}
      <div className="px-4 pt-6 pb-6">
        <div className="bg-blue-50 border border-blue-200 rounded-2xl p-4">
          <div className="text-sm text-blue-900 mb-2">📌 使用说明</div>
          <div className="text-xs text-blue-700 space-y-2">
            <div>1. 点击"一键下载全部"下载所有TabBar图标</div>
            <div>2. 或点击单个图标的"下载"按钮单独下载</div>
            <div>3. 下载的PNG文件可直接用于微信小程序</div>
            <div>4. 将文件放入 images/tabbar/ 目录</div>
            <div>5. 在 app.json 中配置 tabBar</div>
          </div>
        </div>
      </div>

      {/* 配置示例 */}
      <div className="px-4 pb-8">
        <div className="bg-gray-800 rounded-2xl p-4 overflow-x-auto">
          <div className="text-xs text-gray-400 mb-2">app.json 配置示例：</div>
          <pre className="text-xs text-green-400 whitespace-pre">
{`{
  "tabBar": {
    "color": "#8C8C8D",
    "selectedColor": "#615FFF",
    "list": [
      {
        "pagePath": "pages/home/index",
        "text": "首页",
        "iconPath": "images/tabbar/home.png",
        "selectedIconPath": "images/tabbar/home-active.png"
      },
      {
        "pagePath": "pages/pet/index",
        "text": "宠物",
        "iconPath": "images/tabbar/pet.png",
        "selectedIconPath": "images/tabbar/pet-active.png"
      }
    ]
  }
}`}
          </pre>
        </div>
      </div>
    </div>
  );
}