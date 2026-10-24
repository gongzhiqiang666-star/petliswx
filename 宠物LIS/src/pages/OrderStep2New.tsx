import React, { useState } from 'react';
import { ArrowLeft, Search, X, Check } from 'lucide-react';
import { GradientButton } from '../components/GradientButton';

interface OrderStep2NewProps {
  onBack: () => void;
  onNext: (selectedTests: any[]) => void;
  step1Data: any;
}

export function OrderStep2New({ onBack, onNext, step1Data }: OrderStep2NewProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [selectedTests, setSelectedTests] = useState<number[]>([]);

  // 检测项目列表
  const testItems = [
    {
      InfoTestTypeId: 1225,
      name: '狂犬病毒抗体检测',
      category: '基础检查',
      price: 680,
      originalPrice: 800,
      duration: '5-7工作日',
      description: '检测狂犬病毒抗体水平，用于出入境检疫',
      isHot: true,
      requiresChipAndBirthday: true,
    },
    {
      InfoTestTypeId: 1,
      name: '血常规',
      category: '基础检查',
      price: 120,
      originalPrice: 150,
      duration: '1-2工作日',
      description: '检测血液各项指标，评估整体健康状况',
      isHot: true,
      requiresChipAndBirthday: false,
    },
    {
      InfoTestTypeId: 2,
      name: '生化全项',
      category: '生化检查',
      price: 280,
      originalPrice: 350,
      duration: '2-3工作日',
      description: '检测肝肾功能、血糖、血脂等生化指标',
      isHot: false,
      requiresChipAndBirthday: false,
    },
    {
      InfoTestTypeId: 3,
      name: 'DNA检测',
      category: '基因检测',
      price: 1200,
      originalPrice: 1500,
      duration: '10-14工作日',
      description: '检测遗传疾病风险、血统鉴定',
      isHot: false,
      requiresChipAndBirthday: false,
    },
    {
      InfoTestTypeId: 4,
      name: '过敏原检测',
      category: '其他',
      price: 450,
      originalPrice: 550,
      duration: '3-5工作日',
      description: '检测常见过敏原，帮助制定饮食计划',
      isHot: false,
      requiresChipAndBirthday: false,
    },
  ];

  const categories = ['全部', '基础检查', '生化检查', '基因检测', '其他'];

  const filteredTests = testItems.filter(test => {
    const matchesSearch = test.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         test.description.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory = selectedCategory === 'all' || test.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  const handleToggleTest = (testId: number) => {
    if (selectedTests.includes(testId)) {
      setSelectedTests(selectedTests.filter(id => id !== testId));
    } else {
      // 如果选择狂犬检测，验证芯片号和出生日期
      const test = testItems.find(t => t.InfoTestTypeId === testId);
      if (test?.requiresChipAndBirthday) {
        if (!step1Data.ChipNO || !step1Data.Birthday) {
          alert('选择狂犬检测需要填写芯片号和出生日期，请返回步骤1完善信息');
          return;
        }
        if (!step1Data.Remark) {
          alert('选择狂犬检测需要在备注中填写快递单号，请返回步骤1完善信息');
          return;
        }
      }
      setSelectedTests([...selectedTests, testId]);
    }
  };

  const calculateTotal = () => {
    return selectedTests.reduce((total, testId) => {
      const test = testItems.find(t => t.InfoTestTypeId === testId);
      return total + (test?.price || 0);
    }, 0);
  };

  const calculateOriginalTotal = () => {
    return selectedTests.reduce((total, testId) => {
      const test = testItems.find(t => t.InfoTestTypeId === testId);
      return total + (test?.originalPrice || 0);
    }, 0);
  };

  const handleSubmit = () => {
    if (selectedTests.length === 0) {
      alert('请至少选择一个检测项目');
      return;
    }

    const selectedTestDetails = selectedTests.map(testId => {
      const test = testItems.find(t => t.InfoTestTypeId === testId);
      return {
        InfoTestTypeId: testId,
        OrginalPrice: test?.originalPrice || 0,
        TestPrice: test?.price || 0,
        OrginalDiscount: 10, // 默认10（原价）
        IsActive: 1,
        IsCheckPass: 0,
        name: test?.name,
      };
    });

    onNext(selectedTestDetails);
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-32">
      {/* Header */}
      <div className="bg-white px-4 py-4 flex items-center justify-between border-b border-gray-200 sticky top-0 z-40">
        <button onClick={onBack} className="p-1">
          <ArrowLeft className="w-6 h-6 text-gray-700" />
        </button>
        <h1 className="text-gray-900">新建预约 - 步骤2/2</h1>
        <div className="w-8"></div>
      </div>

      {/* Progress Bar */}
      <div className="bg-white px-4 py-3 border-b border-gray-200">
        <div className="flex items-center gap-2">
          <div className="flex-1 h-2 bg-indigo-500 rounded-full"></div>
          <div className="flex-1 h-2 bg-indigo-500 rounded-full"></div>
        </div>
        <div className="flex items-center justify-between mt-2 text-xs">
          <span className="text-indigo-500">基本信息</span>
          <span className="text-indigo-500">选择项目</span>
        </div>
      </div>

      {/* Search Bar */}
      <div className="bg-white px-4 py-3 border-b border-gray-200">
        <div className="bg-gray-50 rounded-3xl px-4 py-3 flex items-center gap-2 border border-gray-200">
          <Search className="w-5 h-5 text-gray-400" />
          <input
            type="text"
            placeholder="搜索检测项目"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="flex-1 bg-transparent outline-none text-gray-900 placeholder-gray-400"
          />
          {searchQuery && (
            <button onClick={() => setSearchQuery('')} className="p-1">
              <X className="w-4 h-4 text-gray-400" />
            </button>
          )}
        </div>
      </div>

      {/* Category Tabs */}
      <div className="bg-white px-4 py-3 border-b border-gray-200 overflow-x-auto">
        <div className="flex gap-2">
          {categories.map((category) => (
            <button
              key={category}
              onClick={() => setSelectedCategory(category === '全部' ? 'all' : category)}
              className={`px-4 py-2 rounded-full whitespace-nowrap transition-colors ${
                (category === '全部' && selectedCategory === 'all') || category === selectedCategory
                  ? 'bg-indigo-500 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              {category}
            </button>
          ))}
        </div>
      </div>

      {/* Test Items List */}
      <div className="px-4 pt-4 space-y-3">
        {filteredTests.map((test) => {
          const isSelected = selectedTests.includes(test.InfoTestTypeId);
          return (
            <button
              key={test.InfoTestTypeId}
              onClick={() => handleToggleTest(test.InfoTestTypeId)}
              className={`w-full bg-white rounded-2xl p-4 text-left transition-all ${
                isSelected
                  ? 'ring-2 ring-indigo-500 shadow-lg'
                  : 'shadow-sm hover:shadow-md'
              }`}
            >
              <div className="flex items-start gap-3">
                {/* Checkbox */}
                <div
                  className={`w-6 h-6 rounded-full border-2 flex items-center justify-center flex-shrink-0 mt-1 transition-colors ${
                    isSelected
                      ? 'border-indigo-500 bg-indigo-500'
                      : 'border-gray-300'
                  }`}
                >
                  {isSelected && <Check className="w-4 h-4 text-white" />}
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <h3 className="text-gray-900">{test.name}</h3>
                        {test.isHot && (
                          <span className="text-xs px-2 py-0.5 bg-orange-100 text-orange-600 rounded">
                            热门
                          </span>
                        )}
                      </div>
                      <div className="text-xs text-gray-500 mb-1">
                        {test.category} · {test.duration}
                      </div>
                    </div>
                    <div className="text-right flex-shrink-0 ml-2">
                      <div className="text-indigo-600 mb-1">
                        ¥{test.price}
                      </div>
                      {test.price < test.originalPrice && (
                        <div className="text-xs text-gray-400 line-through">
                          ¥{test.originalPrice}
                        </div>
                      )}
                    </div>
                  </div>

                  <p className="text-sm text-gray-600 mb-2">
                    {test.description}
                  </p>

                  {test.requiresChipAndBirthday && (
                    <div className="text-xs text-orange-600 bg-orange-50 px-2 py-1 rounded">
                      ⚠️ 需要芯片号、出生日期和快递单号
                    </div>
                  )}
                </div>
              </div>
            </button>
          );
        })}

        {filteredTests.length === 0 && (
          <div className="text-center py-16">
            <div className="text-gray-400 mb-2">未找到相关检测项目</div>
            <div className="text-sm text-gray-400">尝试使用其他关键词搜索</div>
          </div>
        )}
      </div>

      {/* Bottom Fixed Bar */}
      {selectedTests.length > 0 && (
        <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-4 py-4 max-w-md mx-auto">
          <div className="flex items-center justify-between mb-3">
            <div>
              <div className="text-sm text-gray-600">
                已选 <span className="text-indigo-600">{selectedTests.length}</span> 项
              </div>
              <div className="flex items-center gap-2 mt-1">
                <span className="text-xs text-gray-400 line-through">
                  ¥{calculateOriginalTotal()}
                </span>
                <span className="text-indigo-600">
                  ¥{calculateTotal()}
                </span>
              </div>
            </div>
            <GradientButton
              variant="purple"
              size="md"
              onClick={handleSubmit}
              className="px-8"
            >
              提交预约
            </GradientButton>
          </div>
        </div>
      )}
    </div>
  );
}
