import React, { useState } from 'react';
import { ArrowLeft, Plus, Search, X, ChevronRight } from 'lucide-react';
import { GradientButton } from '../components/GradientButton';

interface OrderSelectPetProps {
  onBack: () => void;
  onSelectPet: (petData: any) => void;
  onAddNewPet: () => void;
}

export function OrderSelectPet({ onBack, onSelectPet, onAddNewPet }: OrderSelectPetProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedPetId, setSelectedPetId] = useState<string>('');

  const pets = [
    {
      id: 'pet1',
      AnimalName: '小花',
      AnimalName_en: 'Xiaohua',
      InfoAnimalId: 1,
      AnimalTypeId: 1,
      breed: '英国短毛猫',
      Age: '2岁3个月',
      DictSexId: 2,
      AnimalWeight: '4.5',
      ChipNO: 'CN123456789',
      Birthday: '2021-06-15',
      MasterName: '张三',
      MasterName_en: 'Zhang San',
      avatar: '🐱',
    },
    {
      id: 'pet2',
      AnimalName: '小黑',
      AnimalName_en: 'Xiaohei',
      InfoAnimalId: 2,
      AnimalTypeId: 2,
      breed: '金毛寻回犬',
      Age: '3岁',
      DictSexId: 1,
      AnimalWeight: '28',
      ChipNO: 'CN987654321',
      Birthday: '2020-03-10',
      MasterName: '李四',
      MasterName_en: 'Li Si',
      avatar: '🐕',
    },
    {
      id: 'pet3',
      AnimalName: '喵喵',
      AnimalName_en: 'Miaomiao',
      InfoAnimalId: 1,
      AnimalTypeId: 3,
      breed: '布偶猫',
      Age: '1岁6个月',
      DictSexId: 2,
      AnimalWeight: '3.8',
      ChipNO: '',
      Birthday: '2022-07-20',
      MasterName: '王五',
      MasterName_en: 'Wang Wu',
      avatar: '🐈',
    },
  ];

  const filteredPets = pets.filter(pet =>
    pet.AnimalName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    pet.AnimalName_en.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleContinue = () => {
    const selectedPet = pets.find(p => p.id === selectedPetId);
    if (!selectedPet) {
      alert('请选择一只宠物');
      return;
    }
    onSelectPet(selectedPet);
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      {/* Header */}
      <div className="bg-white px-4 py-4 flex items-center justify-between border-b border-gray-200 sticky top-0 z-40">
        <button onClick={onBack} className="p-1">
          <ArrowLeft className="w-6 h-6 text-gray-700" />
        </button>
        <h1 className="text-gray-900">选择宠物</h1>
        <div className="w-8"></div>
      </div>

      {/* Progress Indicator */}
      <div className="bg-white px-4 py-3 border-b border-gray-200">
        <div className="flex items-center gap-2">
          <div className="flex-1 h-2 bg-indigo-500 rounded-full"></div>
          <div className="flex-1 h-2 bg-gray-200 rounded-full"></div>
          <div className="flex-1 h-2 bg-gray-200 rounded-full"></div>
        </div>
        <div className="flex items-center justify-between mt-2 text-xs">
          <span className="text-indigo-500">选择宠物</span>
          <span className="text-gray-400">基本信息</span>
          <span className="text-gray-400">选择项目</span>
        </div>
      </div>

      {/* Search Bar */}
      <div className="px-4 pt-4">
        <div className="bg-white rounded-2xl px-4 py-3 flex items-center gap-2 border border-gray-200">
          <Search className="w-5 h-5 text-gray-400" />
          <input
            type="text"
            placeholder="搜索宠物名字"
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

      {/* Add New Pet Card */}
      <div className="px-4 pt-4">
        <button
          onClick={onAddNewPet}
          className="w-full bg-gradient-to-r from-indigo-500 to-purple-600 rounded-2xl p-5 text-white flex items-center justify-between hover:from-indigo-600 hover:to-purple-700 transition-all shadow-lg"
          style={{ boxShadow: '0 4px 12px rgba(99, 102, 241, 0.3)' }}
        >
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center">
              <Plus className="w-6 h-6 text-white" />
            </div>
            <div className="text-left">
              <div className="mb-1">添加新宠物</div>
              <div className="text-sm text-white/80">创建新的宠物档案并预约</div>
            </div>
          </div>
          <ChevronRight className="w-6 h-6 text-white/80" />
        </button>
      </div>

      {/* Existing Pets List */}
      <div className="px-4 pt-4">
        <div className="text-sm text-gray-500 mb-3">或选择已有宠物</div>
        <div className="space-y-3">
          {filteredPets.length > 0 ? (
            filteredPets.map((pet) => (
              <button
                key={pet.id}
                onClick={() => setSelectedPetId(pet.id)}
                className={`w-full bg-white rounded-2xl p-4 text-left transition-all ${
                  selectedPetId === pet.id
                    ? 'ring-2 ring-indigo-500 shadow-lg'
                    : 'shadow-sm hover:shadow-md'
                }`}
              >
                <div className="flex items-start gap-4">
                  {/* Pet Info */}
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <h3 className="text-gray-900">{pet.AnimalName}</h3>
                      <span
                        className={`text-xs px-2 py-1 rounded-full ${
                          pet.DictSexId === 1 ? 'bg-blue-50 text-blue-600' : 'bg-pink-50 text-pink-600'
                        }`}
                      >
                        {pet.DictSexId === 1 ? '♂' : '♀'}
                      </span>
                    </div>
                    <div className="text-sm text-gray-500 space-y-1">
                      <div>{pet.breed}</div>
                      <div>出生日期：{pet.Birthday}</div>
                      <div>芯片号：{pet.ChipNO || '未录入'}</div>
                    </div>
                  </div>
                </div>
              </button>
            ))
          ) : (
            <div className="text-center py-8 text-gray-400">
              没有找到相关宠物
            </div>
          )}
        </div>
      </div>

      {/* Info Card */}
      {filteredPets.length === 0 && !searchQuery && (
        <div className="px-4 pt-4">
          <div className="bg-blue-50 border border-blue-200 rounded-2xl p-4 text-sm text-blue-800">
            <div className="mb-2">💡 温馨提示：</div>
            <div>您还没有添加任何宠物，点击上方"添加新宠物"按钮创建宠物档案</div>
          </div>
        </div>
      )}

      {/* Bottom Action */}
      {selectedPetId && (
        <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-4 py-4 max-w-md mx-auto">
          <GradientButton
            variant="purple"
            size="lg"
            onClick={handleContinue}
            className="w-full"
          >
            继续预约
          </GradientButton>
        </div>
      )}
    </div>
  );
}