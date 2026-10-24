import React, { useState } from 'react';
import { Search, Plus, Edit2, Trash2, X } from 'lucide-react';

interface PetListProps {
  onViewDetail: (petId: string) => void;
  onAddPet: () => void;
  onEditPet: (petId: string) => void;
}

export function PetList({ onViewDetail, onAddPet, onEditPet }: PetListProps) {
  const [searchQuery, setSearchQuery] = useState('');

  const pets = [
    {
      id: 'pet1',
      AnimalName: '小花',
      AnimalName_en: 'Xiaohua',
      InfoAnimalId: 1, // 猫
      AnimalTypeId: 1,
      breed: '英国短毛猫',
      Age: '2岁3个月',
      DictSexId: 2, // 母
      AnimalWeight: '4.5',
      MasterName: '张三',
      MasterName_en: 'Zhang San',
      avatar: '🐱',
    },
    {
      id: 'pet2',
      AnimalName: '小黑',
      AnimalName_en: 'Xiaohei',
      InfoAnimalId: 2, // 狗
      AnimalTypeId: 2,
      breed: '金毛寻回犬',
      Age: '3岁',
      DictSexId: 1, // 公
      AnimalWeight: '28',
      MasterName: '李四',
      MasterName_en: 'Li Si',
      avatar: '🐕',
    },
    {
      id: 'pet3',
      AnimalName: '喵喵',
      AnimalName_en: 'Miaomiao',
      InfoAnimalId: 1, // 猫
      AnimalTypeId: 3,
      breed: '布偶猫',
      Age: '1岁6个月',
      DictSexId: 2, // 母
      AnimalWeight: '3.8',
      MasterName: '王五',
      MasterName_en: 'Wang Wu',
      avatar: '🐈',
    },
  ];

  const filteredPets = pets.filter(pet =>
    pet.AnimalName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    pet.AnimalName_en.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleDelete = (petId: string, petName: string) => {
    if (confirm(`确定要删除 ${petName} 吗？`)) {
      // Handle delete
      alert('宠物已删除');
    }
  };

  const getGenderText = (genderId: number) => {
    const genderMap: { [key: number]: string } = {
      0: '未知',
      1: '公',
      2: '母',
      3: '其他',
    };
    return genderMap[genderId] || '未知';
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-20">
      {/* Header */}
      <div className="bg-white px-4 pt-8 pb-4">
        <div className="flex items-center justify-between mb-2">
          <div>
            <h1 className="text-gray-900 mb-1">我的宠物</h1>
            <p className="text-sm text-gray-500">共 {pets.length} 只宠物</p>
          </div>
          <button
            onClick={onAddPet}
            className="text-indigo-500 flex items-center gap-1 px-3 py-2"
          >
            <Plus className="w-5 h-5" />
            <span>添加</span>
          </button>
        </div>
      </div>

      {/* Search Bar */}
      <div className="px-4 pt-4">
        <div className="bg-gray-50 rounded-3xl px-4 py-3 flex items-center gap-2 border border-gray-200">
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

      {/* Pet List */}
      <div className="px-4 pt-4 space-y-3">
        {filteredPets.length > 0 ? (
          filteredPets.map((pet) => (
            <div
              key={pet.id}
              className="bg-white rounded-2xl p-4 shadow-sm"
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
                    <div>年龄：{pet.Age}</div>
                    <div>主人：{pet.MasterName}</div>
                  </div>
                </div>

                {/* Edit Button */}
                <button
                  onClick={() => onEditPet(pet.id)}
                  className="px-4 py-2 text-sm text-indigo-500 hover:bg-indigo-50 rounded-lg transition-colors"
                >
                  编辑
                </button>
              </div>
            </div>
          ))
        ) : (
          <div className="flex flex-col items-center justify-center py-16">
            <div className="w-32 h-32 bg-gray-100 rounded-full flex items-center justify-center mb-4">
              <span className="text-6xl">🐾</span>
            </div>
            <div className="text-gray-500 mb-2">还没有添加宠物</div>
            <div className="text-sm text-gray-400 mb-6">点击下方按钮添加您的第一只宠物</div>
            <button
              onClick={onAddPet}
              className="bg-gradient-to-r from-indigo-500 to-purple-600 text-white px-8 py-3 rounded-3xl hover:from-indigo-600 hover:to-purple-700 transition-all"
            >
              + 添加宠物
            </button>
          </div>
        )}
      </div>

      {/* Floating Action Button */}
      {filteredPets.length > 0 && (
        <button
          onClick={onAddPet}
          className="fixed bottom-24 right-6 w-14 h-14 bg-gradient-to-r from-indigo-500 to-purple-600 rounded-full flex items-center justify-center shadow-lg hover:shadow-xl transition-shadow z-40"
          style={{ boxShadow: '0 4px 12px rgba(99, 102, 241, 0.4)' }}
        >
          <Plus className="w-6 h-6 text-white" />
        </button>
      )}
    </div>
  );
}