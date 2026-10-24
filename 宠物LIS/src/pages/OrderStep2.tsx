import React, { useState } from 'react';
import { ArrowLeft, Search } from 'lucide-react';
import { GradientButton } from '../components/GradientButton';

interface OrderStep2Props {
  onBack: () => void;
  onNext: () => void;
}

export function OrderStep2({ onBack, onNext }: OrderStep2Props) {
  const [selectedItems, setSelectedItems] = useState<string[]>(['item1', 'item2']);
  const [activeCategory, setActiveCategory] = useState('all');

  const categories = [
    { id: 'all', label: '全部' },
    { id: 'basic', label: '基础检查' },
    { id: 'biochemistry', label: '生化检查' },
    { id: 'gene', label: '基因检测' },
    { id: 'other', label: '其他' },
  ];

  const testItems = [
    {
      id: 'item1',
      name: '血常规检查',
      category: 'basic',
      duration: '30分钟',
      description: '检查血液各项指标，评估整体健康状况',
      rating: 5,
      price: 50,
      popular: true,
    },
    {
      id: 'item2',
      name: '生化全项',
      category: 'biochemistry',
      duration: '45分钟',
      description: '全面检查肝肾功能、电解质等',
      rating: 5,
      price: 150,
      popular: true,
    },
    {
      id: 'item3',
      name: '基因检测',
      category: 'gene',
      duration: '7-10天',
      description: '检测遗传疾病风险，品种鉴定',
      rating: 4,
      price: 300,
      popular: false,
    },
    {
      id: 'item4',
      name: '过敏原检测',
      category: 'other',
      duration: '3-5天',
      description: '检测食物和环境过敏原',
      rating: 4,
      price: 200,
      popular: false,
    },
  ];

  const filteredItems = activeCategory === 'all'
    ? testItems
    : testItems.filter(item => item.category === activeCategory);

  const toggleItem = (id: string) => {
    setSelectedItems(prev =>
      prev.includes(id)
        ? prev.filter(item => item !== id)
        : [...prev, id]
    );
  };

  const totalPrice = testItems
    .filter(item => selectedItems.includes(item.id))
    .reduce((sum, item) => sum + item.price, 0);

  return (
    <div className="min-h-screen bg-gray-50 pb-32">
      {/* Header */}
      <div className="bg-white px-4 py-4 flex items-center justify-between border-b border-gray-200 sticky top-0 z-40">
        <button onClick={onBack} className="p-1">
          <ArrowLeft className="w-6 h-6 text-gray-700" />
        </button>
        <h1 className="text-gray-900">选择检测项目</h1>
        <div className="w-6"></div>
      </div>

      {/* Step Indicator */}
      <div className="bg-white px-4 py-4 flex items-center justify-center gap-2 border-b border-gray-200">
        <div className="w-8 h-8 rounded-full bg-indigo-500 flex items-center justify-center text-white text-sm">1</div>
        <div className="w-12 h-0.5 bg-indigo-500"></div>
        <div className="w-8 h-8 rounded-full bg-indigo-500 flex items-center justify-center text-white text-sm">2</div>
        <div className="w-12 h-0.5 bg-gray-300"></div>
        <div className="w-8 h-8 rounded-full bg-gray-300 flex items-center justify-center text-white text-sm">3</div>
      </div>

      {/* Search Bar */}
      <div className="px-4 pt-4">
        <div className="bg-gray-50 rounded-3xl px-4 py-3 flex items-center gap-2 border border-gray-200">
          <Search className="w-5 h-5 text-gray-400" />
          <input
            type="text"
            placeholder="搜索检测项目"
            className="flex-1 bg-transparent outline-none text-gray-900 placeholder-gray-400"
          />
        </div>
      </div>

      {/* Category Tabs */}
      <div className="px-4 pt-4 overflow-x-auto">
        <div className="flex gap-2 pb-2">
          {categories.map((category) => (
            <button
              key={category.id}
              onClick={() => setActiveCategory(category.id)}
              className={`px-4 py-2 rounded-full whitespace-nowrap transition-all ${
                activeCategory === category.id
                  ? 'bg-indigo-500 text-white'
                  : 'bg-gray-100 text-gray-600'
              }`}
            >
              {category.label}
            </button>
          ))}
        </div>
      </div>

      {/* Test Items List */}
      <div className="px-4 pt-4 space-y-3">
        {filteredItems.map((item) => (
          <div key={item.id} className="bg-white rounded-2xl p-4 relative">
            {item.popular && (
              <div className="absolute top-3 right-3 bg-orange-500 text-white text-xs px-2 py-1 rounded">
                热门
              </div>
            )}
            
            <div className="flex items-start gap-3">
              <button
                onClick={() => toggleItem(item.id)}
                className={`w-5 h-5 rounded flex items-center justify-center flex-shrink-0 mt-1 transition-colors ${
                  selectedItems.includes(item.id)
                    ? 'bg-indigo-500 text-white'
                    : 'border-2 border-gray-300'
                }`}
              >
                {selectedItems.includes(item.id) && (
                  <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" />
                  </svg>
                )}
              </button>

              <div className="flex-1">
                <div className="flex justify-between items-start mb-1">
                  <h4 className="text-gray-900 pr-12">{item.name}</h4>
                  <div className="text-indigo-500">¥{item.price}</div>
                </div>
                
                <div className="text-xs text-gray-500 mb-2">
                  {item.category === 'basic' && '基础检查'} 
                  {item.category === 'biochemistry' && '生化检查'}
                  {item.category === 'gene' && '基因检测'}
                  {item.category === 'other' && '其他'}
                  {' · 预计'}{item.duration}
                </div>
                
                <div className="text-xs text-gray-600 mb-2">{item.description}</div>
                
                <div className="flex items-center gap-1">
                  <span className="text-xs text-gray-500">推荐指数:</span>
                  {Array.from({ length: 5 }).map((_, i) => (
                    <span key={i} className={i < item.rating ? 'text-yellow-400' : 'text-gray-300'}>
                      ★
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Bottom Fixed Bar */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-4 py-4 max-w-md mx-auto">
        <div className="flex items-center justify-between mb-3">
          <div className="text-sm text-gray-500">
            已选择 <span className="text-indigo-500">{selectedItems.length}</span> 项
          </div>
          <div className="text-indigo-500">¥{totalPrice}</div>
        </div>
        <GradientButton
          variant="purple"
          size="full"
          onClick={onNext}
          className={selectedItems.length === 0 ? 'opacity-50 cursor-not-allowed' : ''}
        >
          下一步
        </GradientButton>
      </div>
    </div>
  );
}
