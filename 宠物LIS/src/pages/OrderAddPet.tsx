import React, { useState } from 'react';
import { ArrowLeft, ChevronRight } from 'lucide-react';
import { GradientButton } from '../components/GradientButton';

interface OrderAddPetProps {
  onBack: () => void;
  onSave: (petData: any) => void;
}

export function OrderAddPet({ onBack, onSave }: OrderAddPetProps) {
  const [formData, setFormData] = useState({
    // 基本信息
    InfoAnimalId: 4, // 默认值为4(乌龟)
    AnimalTypeId: 0,
    AnimalName: '',
    AnimalName_en: '',
    AnimalWeight: '',
    DictSexId: 0,
    Age: '',
    ChipNO: '',
    Birthday: '',
    
    // 主人信息
    MasterName: '',
    MasterName_en: '',
  });

  // 物种选项
  const animalSpecies = [
    { id: 1, name: '猫', name_en: 'Cat' },
    { id: 2, name: '狗', name_en: 'Dog' },
    { id: 3, name: '兔', name_en: 'Rabbit' },
    { id: 4, name: '乌龟', name_en: 'Turtle' },
    { id: 5, name: '其他', name_en: 'Other' },
  ];

  // 性别选项
  const genderOptions = [
    { id: 0, name: '未知' },
    { id: 1, name: '公' },
    { id: 2, name: '母' },
    { id: 3, name: '其他' },
  ];

  const handleSpeciesChange = (speciesId: number) => {
    setFormData({
      ...formData,
      InfoAnimalId: speciesId,
    });
  };

  const calculateAgeFromBirthday = (birthday: string): string => {
    if (!birthday) return '';
    
    const birth = new Date(birthday);
    const today = new Date();
    const years = today.getFullYear() - birth.getFullYear();
    const months = today.getMonth() - birth.getMonth();
    
    let ageYears = years;
    let ageMonths = months;
    
    if (months < 0) {
      ageYears--;
      ageMonths = 12 + months;
    }
    
    if (ageYears === 0 && ageMonths === 0) {
      return '';
    }
    
    if (ageYears > 0 && ageMonths > 0) {
      return `${ageYears}岁${ageMonths}个月`;
    } else if (ageYears > 0) {
      return `${ageYears}岁`;
    } else {
      return `${ageMonths}个月`;
    }
  };

  const handleBirthdayChange = (birthday: string) => {
    const age = calculateAgeFromBirthday(birthday);
    setFormData({
      ...formData,
      Birthday: birthday,
      Age: age,
    });
  };

  const handleSubmit = () => {
    // 验证必填项
    if (!formData.AnimalName || !formData.AnimalName.trim()) {
      alert('请输入宠物中文名');
      return;
    }
    if (!formData.AnimalName_en || !formData.AnimalName_en.trim()) {
      alert('请输入宠物英文名');
      return;
    }
    if (!formData.InfoAnimalId) {
      alert('请选择物种');
      return;
    }
    if (!formData.Birthday) {
      alert('请选择出生日期');
      return;
    }
    if (!formData.MasterName || !formData.MasterName.trim()) {
      alert('请输入畜主中文名');
      return;
    }
    if (!formData.MasterName_en || !formData.MasterName_en.trim()) {
      alert('请输入畜主英文名');
      return;
    }
    
    // 生成临时ID
    const petData = {
      ...formData,
      id: 'temp-' + Date.now(),
    };
    
    onSave(petData);
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      {/* Header */}
      <div className="bg-white px-4 py-4 flex items-center justify-between border-b border-gray-200 sticky top-0 z-40">
        <button onClick={onBack} className="p-1">
          <ArrowLeft className="w-6 h-6 text-gray-700" />
        </button>
        <h1 className="text-gray-900">添加宠物</h1>
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
          <span className="text-indigo-500">添加宠物</span>
          <span className="text-gray-400">基本信息</span>
          <span className="text-gray-400">选择项目</span>
        </div>
      </div>

      <div className="px-4 pt-4 space-y-4">
        {/* Master Info Section - Moved to Top */}
        <div className="bg-white rounded-2xl p-5">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-4 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">主人信息</h3>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                畜主中文名 <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.MasterName}
                onChange={(e) => setFormData({ ...formData, MasterName: e.target.value })}
                placeholder="请输入畜主中文名"
                className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors"
              />
            </div>

            <div>
              <label className="block text-sm text-gray-700 mb-2">
                畜主英文名 <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.MasterName_en}
                onChange={(e) => setFormData({ ...formData, MasterName_en: e.target.value })}
                placeholder="请输入畜主英文名"
                className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors"
              />
            </div>
          </div>
        </div>

        {/* Basic Info Section */}
        <div className="bg-white rounded-2xl p-5">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-4 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">宠物信息</h3>
          </div>

          <div className="space-y-4">
            {/* Animal Name (Chinese) */}
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                宠物中文名 <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.AnimalName}
                onChange={(e) => setFormData({ ...formData, AnimalName: e.target.value })}
                placeholder="如：小花"
                className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors"
              />
            </div>

            {/* Animal Name (English) */}
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                宠物英文名 <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.AnimalName_en}
                onChange={(e) => setFormData({ ...formData, AnimalName_en: e.target.value })}
                placeholder="如：Xiaohua"
                className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors"
              />
            </div>

            {/* Species */}
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                物种 <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <select
                  value={formData.InfoAnimalId}
                  onChange={(e) => handleSpeciesChange(Number(e.target.value))}
                  className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors appearance-none"
                >
                  {animalSpecies.map(species => (
                    <option key={species.id} value={species.id}>
                      {species.name} ({species.name_en})
                    </option>
                  ))}
                </select>
                <ChevronRight className="w-5 h-5 text-gray-400 absolute right-4 top-1/2 -translate-y-1/2 rotate-90 pointer-events-none" />
              </div>
            </div>

            {/* Gender */}
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                性别 <span className="text-red-500">*</span>
              </label>
              <div className="grid grid-cols-4 gap-2">
                {genderOptions.map((gender) => (
                  <button
                    key={gender.id}
                    onClick={() => setFormData({ ...formData, DictSexId: gender.id })}
                    className={`py-3 border border-gray-200 rounded-2xl transition-colors ${
                      formData.DictSexId === gender.id
                        ? 'border-indigo-500 bg-indigo-50 text-indigo-600'
                        : 'hover:border-indigo-300'
                    }`}
                  >
                    <div className="text-sm">{gender.name}</div>
                  </button>
                ))}
              </div>
            </div>

            {/* Birthday */}
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                出生日期 <span className="text-red-500">*</span>
              </label>
              <input
                type="date"
                value={formData.Birthday}
                onChange={(e) => handleBirthdayChange(e.target.value)}
                className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors"
              />
              {formData.Age && (
                <div className="text-xs text-gray-600 mt-2 bg-gray-50 px-3 py-2 rounded-lg">
                  当前年龄: {formData.Age}
                </div>
              )}
            </div>

            {/* Weight */}
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                体重
              </label>
              <div className="relative">
                <input
                  type="text"
                  value={formData.AnimalWeight}
                  onChange={(e) => setFormData({ ...formData, AnimalWeight: e.target.value })}
                  placeholder="如: 4.5"
                  className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors pr-12"
                />
                <span className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-500">
                  kg
                </span>
              </div>
            </div>

            {/* Chip Number */}
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                芯片号
              </label>
              <input
                type="text"
                value={formData.ChipNO}
                onChange={(e) => setFormData({ ...formData, ChipNO: e.target.value })}
                placeholder="狂犬检测必填"
                className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors"
              />
            </div>
          </div>
        </div>

        {/* Info Card */}
        <div className="bg-blue-50 border border-blue-200 rounded-2xl p-4 text-xs text-blue-800">
          <div className="mb-2">💡 温馨提示：</div>
          <ul className="space-y-1 ml-4 list-disc">
            <li>添加宠物后将自动进入预约流程</li>
            <li>标有<span className="text-red-500">*</span>的字段为必填项</li>
            <li>宠物信息保存后可在"我的宠物"中查看和编辑</li>
          </ul>
        </div>
      </div>

      {/* Bottom Action */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-4 py-4 max-w-md mx-auto">
        <GradientButton
          variant="purple"
          size="lg"
          onClick={handleSubmit}
          className="w-full"
        >
          保存并继续预约
        </GradientButton>
      </div>
    </div>
  );
}