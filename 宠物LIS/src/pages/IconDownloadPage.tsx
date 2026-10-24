import React, { useState } from 'react';
import { 
  Home, PawPrint, ShoppingBag, User, ArrowLeft, Share2, Calendar, List, Circle,
  Clock, MapPin, Phone, FileText, CheckCircle, Plus, Check, Search, Filter,
  AlertTriangle, ChevronRight, Download, Bell, Sparkles, ArrowRight, Edit2,
  Trash2, X, ChevronDown, Activity, Building2, Mail, LogOut, Settings,
  HelpCircle, Shield
} from 'lucide-react';
import { PageHeader } from '../components/PageHeader';
import { GradientButton } from '../components/GradientButton';

interface IconDownloadPageProps {
  onNavigate: (page: any) => void;
}

// 所有项目中使用的图标
const projectIcons = [
  { name: 'Home', component: Home, category: '导航' },
  { name: 'PawPrint', component: PawPrint, category: '导航' },
  { name: 'ShoppingBag', component: ShoppingBag, category: '导航' },
  { name: 'User', component: User, category: '导航' },
  { name: 'ArrowLeft', component: ArrowLeft, category: '导航' },
  { name: 'ArrowRight', component: ArrowRight, category: '导航' },
  { name: 'ChevronRight', component: ChevronRight, category: '导航' },
  { name: 'ChevronDown', component: ChevronDown, category: '导航' },
  { name: 'Share2', component: Share2, category: '操作' },
  { name: 'Calendar', component: Calendar, category: '功能' },
  { name: 'List', component: List, category: '功能' },
  { name: 'Circle', component: Circle, category: '状态' },
  { name: 'Clock', component: Clock, category: '功能' },
  { name: 'MapPin', component: MapPin, category: '功能' },
  { name: 'Phone', component: Phone, category: '联系' },
  { name: 'Mail', component: Mail, category: '联系' },
  { name: 'FileText', component: FileText, category: '文档' },
  { name: 'CheckCircle', component: CheckCircle, category: '状态' },
  { name: 'Plus', component: Plus, category: '操作' },
  { name: 'Check', component: Check, category: '状态' },
  { name: 'Search', component: Search, category: '功能' },
  { name: 'Filter', component: Filter, category: '功能' },
  { name: 'AlertTriangle', component: AlertTriangle, category: '状态' },
  { name: 'Download', component: Download, category: '操作' },
  { name: 'Bell', component: Bell, category: '功能' },
  { name: 'Sparkles', component: Sparkles, category: '装饰' },
  { name: 'Edit2', component: Edit2, category: '操作' },
  { name: 'Trash2', component: Trash2, category: '操作' },
  { name: 'X', component: X, category: '操作' },
  { name: 'Activity', component: Activity, category: '功能' },
  { name: 'Building2', component: Building2, category: '信息' },
  { name: 'LogOut', component: LogOut, category: '操作' },
  { name: 'Settings', component: Settings, category: '功能' },
  { name: 'HelpCircle', component: HelpCircle, category: '功能' },
  { name: 'Shield', component: Shield, category: '功能' },
];

export function IconDownloadPage({ onNavigate }: IconDownloadPageProps) {
  const [selectedCategory, setSelectedCategory] = useState<string>('全部');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedIcons, setSelectedIcons] = useState<Set<string>>(new Set());

  // 获取所有分类
  const categories = ['全部', ...Array.from(new Set(projectIcons.map(icon => icon.category)))];

  // 筛选图标
  const filteredIcons = projectIcons.filter(icon => {
    const matchCategory = selectedCategory === '全部' || icon.category === selectedCategory;
    const matchSearch = icon.name.toLowerCase().includes(searchQuery.toLowerCase());
    return matchCategory && matchSearch;
  });

  // 切换图标选择
  const toggleIconSelection = (iconName: string) => {
    const newSelected = new Set(selectedIcons);
    if (newSelected.has(iconName)) {
      newSelected.delete(iconName);
    } else {
      newSelected.add(iconName);
    }
    setSelectedIcons(newSelected);
  };

  // 全选/取消全选
  const toggleSelectAll = () => {
    if (selectedIcons.size === filteredIcons.length) {
      setSelectedIcons(new Set());
    } else {
      setSelectedIcons(new Set(filteredIcons.map(icon => icon.name)));
    }
  };

  // 下载单个图标为PNG
  const downloadIconAsPNG = async (iconName: string, IconComponent: any, size: number = 81) => {
    try {
      // 创建临时容器
      const container = document.createElement('div');
      container.style.position = 'absolute';
      container.style.left = '-9999px';
      document.body.appendChild(container);

      // 使用React渲染图标以获取实际的SVG路径
      const iconDiv = document.createElement('div');
      iconDiv.style.width = `${size}px`;
      iconDiv.style.height = `${size}px`;
      iconDiv.style.color = '#000000';
      container.appendChild(iconDiv);

      // 渲染React组件到DOM
      const root = (window as any).createRoot ? (window as any).createRoot(iconDiv) : null;
      if (root) {
        await new Promise(resolve => {
          root.render(React.createElement(IconComponent, { size, strokeWidth: 2 }));
          setTimeout(resolve, 100);
        });
      } else {
        // Fallback for older React versions
        iconDiv.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-${iconName.toLowerCase()}"></svg>`;
      }

      const svgElement = iconDiv.querySelector('svg');
      if (!svgElement) {
        throw new Error('Failed to render SVG');
      }

      // 获取SVG的序列化字符串
      const svgData = new XMLSerializer().serializeToString(svgElement);
      const svgBlob = new Blob([svgData], { type: 'image/svg+xml;charset=utf-8' });
      const svgUrl = URL.createObjectURL(svgBlob);

      // 创建canvas并绘制
      const canvas = document.createElement('canvas');
      canvas.width = size;
      canvas.height = size;
      const ctx = canvas.getContext('2d');
      
      if (!ctx) {
        throw new Error('Failed to get canvas context');
      }

      const img = new Image();
      img.onload = () => {
        // 使用透明背景
        ctx.clearRect(0, 0, size, size);
        ctx.drawImage(img, 0, 0, size, size);

        // 转换为PNG并下载
        canvas.toBlob((blob) => {
          if (blob) {
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `${iconName.toLowerCase().replace(/([A-Z])/g, '_$1').replace(/^_/, '').toLowerCase()}.png`;
            link.click();
            URL.revokeObjectURL(url);
          }
          URL.revokeObjectURL(svgUrl);
          document.body.removeChild(container);
        }, 'image/png');
      };

      img.onerror = () => {
        console.error('Failed to load SVG image');
        URL.revokeObjectURL(svgUrl);
        document.body.removeChild(container);
      };

      img.src = svgUrl;
    } catch (error) {
      console.error('Error downloading icon:', error);
      alert(`下载图标 ${iconName} 时出错，请重试`);
    }
  };

  // 下载带颜色的图标（用于底部导航栏选中/未选中状态）
  const downloadIconWithColor = async (iconName: string, IconComponent: any, color: string, size: number, fileName: string) => {
    try {
      const container = document.createElement('div');
      container.style.position = 'absolute';
      container.style.left = '-9999px';
      document.body.appendChild(container);

      const iconDiv = document.createElement('div');
      iconDiv.style.width = `${size}px`;
      iconDiv.style.height = `${size}px`;
      iconDiv.style.color = color;
      container.appendChild(iconDiv);

      const root = (window as any).createRoot ? (window as any).createRoot(iconDiv) : null;
      if (root) {
        await new Promise(resolve => {
          root.render(React.createElement(IconComponent, { size, strokeWidth: 2.5 }));
          setTimeout(resolve, 100);
        });
      }

      const svgElement = iconDiv.querySelector('svg');
      if (!svgElement) {
        throw new Error('Failed to render SVG');
      }

      const svgData = new XMLSerializer().serializeToString(svgElement);
      const svgBlob = new Blob([svgData], { type: 'image/svg+xml;charset=utf-8' });
      const svgUrl = URL.createObjectURL(svgBlob);

      const canvas = document.createElement('canvas');
      canvas.width = size;
      canvas.height = size;
      const ctx = canvas.getContext('2d');
      
      if (!ctx) {
        throw new Error('Failed to get canvas context');
      }

      const img = new Image();
      img.onload = () => {
        ctx.clearRect(0, 0, size, size);
        ctx.drawImage(img, 0, 0, size, size);

        canvas.toBlob((blob) => {
          if (blob) {
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = fileName;
            link.click();
            URL.revokeObjectURL(url);
          }
          URL.revokeObjectURL(svgUrl);
          document.body.removeChild(container);
        }, 'image/png');
      };

      img.src = svgUrl;
    } catch (error) {
      console.error('Error downloading icon with color:', error);
    }
  };

  // 批量下载选中的图标（PNG格式）
  const downloadSelectedIcons = async () => {
    if (selectedIcons.size === 0) {
      alert('请至少选择一个图标');
      return;
    }

    const confirmed = window.confirm(
      `即将下载 ${selectedIcons.size} 个图标\n` +
      `尺寸：81px × 81px（微信小程序规范）\n` +
      `格式：PNG 透明背景\n\n` +
      `是否继续？`
    );

    if (!confirmed) return;

    const selectedIconsList = projectIcons.filter(icon => selectedIcons.has(icon.name));
    
    for (let i = 0; i < selectedIconsList.length; i++) {
      const icon = selectedIconsList[i];
      
      // 下载 81x81
      await downloadIconAsPNG(icon.name, icon.component, 81);
      await new Promise(resolve => setTimeout(resolve, 200)); // 延迟避免浏览器阻止多次下载
    }

    alert(`已完成 ${selectedIcons.size} 个图标的下载！`);
  };

  // 下载微信小程序TabBar图标（选中/未选中状态）
  const downloadNavIcons = async () => {
    const navIcons = [
      { name: 'Home', component: Home, fileName: 'home' },
      { name: 'PawPrint', component: PawPrint, fileName: 'pet' },
      { name: 'ShoppingBag', component: ShoppingBag, fileName: 'order' },
      { name: 'User', component: User, fileName: 'profile' },
    ];

    const confirmed = window.confirm(
      `即将下载微信小程序TabBar图标\n\n` +
      `规格说明：\n` +
      `• 尺寸：81px × 81px\n` +
      `• 格式：PNG 透明背景\n` +
      `• 描边：2.5px\n` +
      `• 未选中色：#8C8C8D（微信标准灰）\n` +
      `• 选中色：#615FFF（主题紫色）\n\n` +
      `将下载 8 个文件：\n` +
      `home.png / home-active.png\n` +
      `pet.png / pet-active.png\n` +
      `order.png / order-active.png\n` +
      `profile.png / profile-active.png\n\n` +
      `是否继续？`
    );

    if (!confirmed) return;

    const unselectedColor = '#8C8C8D'; // 微信标准灰色
    const selectedColor = '#615FFF';   // 主题紫色
    const size = 81; // 微信小程序TabBar标准尺寸

    for (const icon of navIcons) {
      // 未选中状态
      await downloadIconWithColor(icon.name, icon.component, unselectedColor, size, `${icon.fileName}.png`);
      await new Promise(resolve => setTimeout(resolve, 300));
      
      // 选中状态
      await downloadIconWithColor(icon.name, icon.component, selectedColor, size, `${icon.fileName}-active.png`);
      await new Promise(resolve => setTimeout(resolve, 300));
    }

    alert('微信小程序TabBar图标下载完成！\n\n已下载 8 个 PNG 文件，可直接用于小程序开发。');
  };

  // 下载所有图标（PNG格式）
  const downloadAllIcons = async () => {
    const confirmed = window.confirm(
      `即将下载所有 ${projectIcons.length} 个图标\n` +
      `尺寸：81px × 81px（微信小程序规范）\n` +
      `格式：PNG 透明背景\n\n` +
      `下载可能需要几分钟时间，是否继续？`
    );

    if (!confirmed) return;

    for (let i = 0; i < projectIcons.length; i++) {
      const icon = projectIcons[i];
      
      // 下载 81x81
      await downloadIconAsPNG(icon.name, icon.component, 81);
      await new Promise(resolve => setTimeout(resolve, 200));
    }

    alert(`已完成所有 ${projectIcons.length} 个图标的下载！`);
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      <PageHeader title="图标下载中心" onBack={() => onNavigate('home')} />

      {/* Stats Section */}
      <div className="px-4 pt-4">
        <div className="bg-gradient-to-br from-indigo-500 to-purple-600 rounded-2xl p-5 text-white">
          <div className="flex items-center justify-between mb-4">
            <div>
              <div className="text-sm opacity-90">项目使用图标</div>
              <div className="text-3xl mt-1">{projectIcons.length}</div>
            </div>
            <Sparkles className="w-12 h-12 opacity-80" />
          </div>
          <div className="grid grid-cols-3 gap-3 text-sm">
            <div className="bg-white/20 rounded-xl p-2 text-center">
              <div className="opacity-90">已选择</div>
              <div className="text-lg mt-1">{selectedIcons.size}</div>
            </div>
            <div className="bg-white/20 rounded-xl p-2 text-center">
              <div className="opacity-90">分类</div>
              <div className="text-lg mt-1">{categories.length - 1}</div>
            </div>
            <div className="bg-white/20 rounded-xl p-2 text-center">
              <div className="opacity-90">当前显示</div>
              <div className="text-lg mt-1">{filteredIcons.length}</div>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Download Section - Navigation Icons */}
      <div className="px-4 pt-4">
        <button
          onClick={() => onNavigate('icon-export')}
          className="w-full bg-gradient-to-r from-indigo-500 to-purple-600 text-white rounded-2xl p-4 flex items-center justify-between hover:from-indigo-600 hover:to-purple-700 transition-all shadow-lg shadow-indigo-200"
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-white/20 rounded-xl flex items-center justify-center">
              <Download className="w-5 h-5" />
            </div>
            <div className="text-left">
              <div>导出微信小程序TabBar图标</div>
              <div className="text-xs opacity-80 mt-0.5">81px × 81px · 8个PNG文件 · 可预览</div>
            </div>
          </div>
          <ChevronRight className="w-5 h-5 opacity-80" />
        </button>
      </div>

      {/* Search Section */}
      <div className="px-4 pt-4">
        <div className="relative">
          <Search className="w-5 h-5 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="搜索图标名称..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-12 pr-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-4 top-1/2 -translate-y-1/2"
            >
              <X className="w-5 h-5 text-gray-400 hover:text-gray-600" />
            </button>
          )}
        </div>
      </div>

      {/* Category Filter */}
      <div className="px-4 pt-4">
        <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-hide">
          {categories.map(category => (
            <button
              key={category}
              onClick={() => setSelectedCategory(category)}
              className={`px-4 py-2 rounded-full whitespace-nowrap transition-colors ${
                selectedCategory === category
                  ? 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white'
                  : 'bg-white text-gray-700 border border-gray-200 hover:border-indigo-300'
              }`}
            >
              {category}
            </button>
          ))}
        </div>
      </div>

      {/* Select All Button */}
      <div className="px-4 pt-4">
        <button
          onClick={toggleSelectAll}
          className="w-full py-3 bg-white border border-gray-200 rounded-2xl text-gray-700 hover:border-indigo-300 transition-colors flex items-center justify-center gap-2"
        >
          <Check className="w-5 h-5" />
          {selectedIcons.size === filteredIcons.length ? '取消全选' : '全选当前'}
        </button>
      </div>

      {/* Icons Grid */}
      <div className="px-4 pt-4 pb-4">
        <div className="grid grid-cols-3 gap-3">
          {filteredIcons.map(({ name, component: IconComponent, category }) => {
            const isSelected = selectedIcons.has(name);
            return (
              <button
                key={name}
                onClick={() => toggleIconSelection(name)}
                className={`bg-white rounded-2xl p-4 border-2 transition-all ${
                  isSelected
                    ? 'border-indigo-500 bg-indigo-50'
                    : 'border-gray-200 hover:border-indigo-300'
                }`}
              >
                <div className="flex flex-col items-center gap-2">
                  <div className={`p-3 rounded-xl ${
                    isSelected ? 'bg-indigo-100' : 'bg-gray-50'
                  }`}>
                    <IconComponent className={`w-6 h-6 ${
                      isSelected ? 'text-indigo-600' : 'text-gray-700'
                    }`} />
                  </div>
                  <div className="text-xs text-gray-600 text-center truncate w-full">
                    {name}
                  </div>
                  <div className="text-xs text-gray-400">
                    {category}
                  </div>
                  {isSelected && (
                    <div className="absolute top-2 right-2">
                      <CheckCircle className="w-5 h-5 text-indigo-600 fill-white" />
                    </div>
                  )}
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Empty State */}
      {filteredIcons.length === 0 && (
        <div className="px-4 pt-12 text-center">
          <AlertTriangle className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <div className="text-gray-500">未找到匹配的图标</div>
          <div className="text-sm text-gray-400 mt-2">请尝试其他搜索词或分类</div>
        </div>
      )}

      {/* Bottom Action Buttons */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-4 py-4 max-w-md mx-auto z-40">
        <div className="space-y-3">
          <GradientButton
            variant="purple"
            size="md"
            onClick={downloadSelectedIcons}
            disabled={selectedIcons.size === 0}
            className="w-full flex items-center justify-center gap-2"
          >
            <Download className="w-5 h-5" />
            下载选中图标 ({selectedIcons.size})
          </GradientButton>
          
          <GradientButton
            variant="outline"
            size="md"
            onClick={downloadAllIcons}
            className="w-full flex items-center justify-center gap-2"
          >
            <List className="w-5 h-5" />
            下载完整列表 ({projectIcons.length})
          </GradientButton>
        </div>
      </div>
    </div>
  );
}