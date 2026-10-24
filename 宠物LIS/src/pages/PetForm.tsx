import React, { useState } from 'react';
import { ArrowLeft, ChevronRight } from 'lucide-react';
import { PageHeader } from '../components/PageHeader';
import { GradientButton } from '../components/GradientButton';

interface PetFormProps {
  petId?: string;
  onBack: () => void;
  onSave: () => void;
}

export function PetForm({ petId, onBack, onSave }: PetFormProps) {
  const isEdit = !!petId;
  const [formData, setFormData] = useState({
    // 基本信息
    InfoAnimalId: isEdit ? 1 : 4, // 默认值为4(乌龟)
    AnimalTypeId: isEdit ? 1 : 0, // 品种ID
    AnimalName: isEdit ? '小花' : '', // 中文名，必填
    AnimalName_en: isEdit ? 'Xiaohua' : '', // 英文名，必填
    AnimalType: isEdit ? 1 : 0, // 冗余字段
    AnimalType_en: isEdit ? 'Cat' : '', // 冗余字段
    AnimalWeight: isEdit ? '4.5' : '', // 体重，可选
    DictSexId: isEdit ? 2 : 0, // 性别ID: 0(未知), 1(公), 2(母), 3(其他)
    Age: isEdit ? '2岁3个月' : '', // 年龄，格式如"3岁2个月"
    ChipNO: isEdit ? 'CN123456789' : '', // 芯片号，狂犬检测必填
    Birthday: isEdit ? '2021-06-15' : '', // 出生日期，狂犬检测必填
    
    // 主人信息
    MasterName: isEdit ? '张三' : '', // 畜主中文名，必填
    MasterName_en: isEdit ? 'Zhang San' : '', // 畜主英文名，必填
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
    
    onSave();
  };

  const handlePhotoUpload = () => {
    // Mock photo upload
    alert('照片上传功能');
  };

  const handleSpeciesChange = (speciesId: number) => {
    const species = animalSpecies.find(s => s.id === speciesId);
    if (species) {
      setFormData({
        ...formData,
        InfoAnimalId: speciesId,
        AnimalType: speciesId,
        AnimalType_en: species.name_en,
      });
    }
  };

  // 根据出生日期计算年龄
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

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      <PageHeader title={isEdit ? '编辑宠物' : '添加宠物'} onBack={onBack} />

      <div className="px-4 pt-4 space-y-4">
        {/* Basic Info Section */}
        <div className="bg-white rounded-2xl p-5">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-4 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">基本信息</h3>
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
                placeholder="请输入宠物中文名"
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
                placeholder="请输入宠物英文名（国际订单使用）"
                className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors"
              />
            </div>

            {/* Species (InfoAnimalId) */}
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
                  <option value={0}>请选择物种</option>
                  {animalSpecies.map(species => (
                    <option key={species.id} value={species.id}>
                      {species.name} ({species.name_en})
                    </option>
                  ))}
                </select>
                <ChevronRight className="w-5 h-5 text-gray-400 absolute right-4 top-1/2 -translate-y-1/2 rotate-90 pointer-events-none" />
              </div>
            </div>

            {/* Breed */}
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                品种
              </label>
              <input
                type="text"
                value={formData.AnimalTypeId}
                onChange={(e) => setFormData({ ...formData, AnimalTypeId: Number(e.target.value) || 0 })}
                placeholder="请输入品种ID（关联品种表）"
                className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors"
              />
              <div className="text-xs text-gray-400 mt-1">
                输入品种ID，关联lb_infovariety表
              </div>
            </div>

            {/* Gender (DictSexId) */}
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
          </div>
        </div>

        {/* Physical Info Section */}
        <div className="bg-white rounded-2xl p-5">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-4 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">体征信息</h3>
          </div>

          <div className="space-y-4">
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
              <div className="text-xs text-gray-500 mt-1">
                狂犬检测必填
              </div>
              {formData.Age && (
                <div className="text-xs text-gray-600 mt-2 bg-gray-50 px-3 py-2 rounded-lg">
                  当前年龄: {formData.Age}
                </div>
              )}
            </div>

            {/* Age (Auto-calculated or Manual) */}
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                年龄
              </label>
              <input
                type="text"
                value={formData.Age}
                onChange={(e) => setFormData({ ...formData, Age: e.target.value })}
                placeholder="格式如: 3岁2个月"
                className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors"
              />
              <div className="text-xs text-gray-400 mt-1">
                根据出生日期自动计算，也可手动填写
              </div>
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
                placeholder="请输入芯片号"
                className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors"
              />
              <div className="text-xs text-gray-500 mt-1">
                狂犬检测必填
              </div>
            </div>
          </div>
        </div>

        {/* Master Info Section */}
        <div className="bg-white rounded-2xl p-5">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-4 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">主人信息</h3>
          </div>

          <div className="space-y-4">
            {/* Master Name (Chinese) */}
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

            {/* Master Name (English) */}
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

        {/* Field Mapping Info */}
        <div className="bg-blue-50 border border-blue-200 rounded-2xl p-4 text-xs text-blue-800">
          <div className="mb-2">📋 字段说明：</div>
          <ul className="space-y-1 ml-4 list-disc">
            <li>InfoAnimalId: 物种ID (默认4-乌龟)</li>
            <li>AnimalTypeId: 品种ID (关联品种表)</li>
            <li>DictSexId: 性别 (0-未知/1-公/2-母/3-其他)</li>
            <li>ChipNO、Birthday: 狂犬检测必填</li>
            <li>MasterName、MasterName_en: 畜主信息必填</li>
          </ul>
        </div>
      </div>

      {/* Save Section */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-4 py-4 flex gap-3 max-w-md mx-auto z-40">
        <GradientButton variant="outline" size="md" onClick={onBack} className="flex-1">
          取消
        </GradientButton>
        <GradientButton variant="purple" size="md" onClick={handleSubmit} className="flex-1">
          保存
        </GradientButton>
      </div>
    </div>
  );
}