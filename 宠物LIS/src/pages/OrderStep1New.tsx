import React, { useState } from 'react';
import { ArrowLeft, ChevronRight } from 'lucide-react';
import { GradientButton } from '../components/GradientButton';

interface OrderStep1NewProps {
  onBack: () => void;
  onNext: (formData: any) => void;
  initialData?: any;
}

export function OrderStep1New({ onBack, onNext, initialData }: OrderStep1NewProps) {
  const [formData, setFormData] = useState({
    // 动物信息
    InfoAnimalId: initialData?.InfoAnimalId || 4, // 默认4-乌龟
    AnimalTypeId: initialData?.AnimalTypeId || 0,
    AnimalName: initialData?.AnimalName || '',
    AnimalName_en: initialData?.AnimalName_en || '',
    DictSexId: initialData?.DictSexId || 0,
    Age: initialData?.Age || '',
    ChipNO: initialData?.ChipNO || '',
    Birthday: initialData?.Birthday || '',
    
    // 主人信息
    MasterName: initialData?.MasterName || '',
    MasterName_en: initialData?.MasterName_en || '',
    DoctorName: '',
    
    // 采样信息
    InfoSampleTypeId: 1, // 默认1
    SampleArea: '',
    CollectDt: new Date().toISOString().split('T')[0], // 默认当前日期
    
    // 其他信息
    CityId: 0,
    Remark: '', // 狂犬检测必填快递单号
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

  // 采样类型选项
  const sampleTypes = [
    { id: 1, name: '血清' },
    { id: 2, name: '全血' },
    { id: 3, name: '组织' },
    { id: 4, name: '其他' },
  ];

  // 城市选项（示例）
  const cities = [
    { id: 1, name: '北京' },
    { id: 2, name: '上海' },
    { id: 3, name: '广州' },
    { id: 4, name: '深圳' },
    { id: 5, name: '成都' },
  ];

  const handleSpeciesChange = (speciesId: number) => {
    const species = animalSpecies.find(s => s.id === speciesId);
    if (species) {
      setFormData({
        ...formData,
        InfoAnimalId: speciesId,
      });
    }
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
    if (!formData.MasterName || !formData.MasterName.trim()) {
      alert('请输入畜主中文名');
      return;
    }
    if (!formData.MasterName_en || !formData.MasterName_en.trim()) {
      alert('请输入畜主英文名');
      return;
    }
    if (!formData.DoctorName || !formData.DoctorName.trim()) {
      alert('请输入送检医师姓名');
      return;
    }
    if (!formData.SampleArea || !formData.SampleArea.trim()) {
      alert('请输入采样部位');
      return;
    }
    if (!formData.CollectDt) {
      alert('请选择采样日期');
      return;
    }

    onNext(formData);
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      {/* Header */}
      <div className="bg-white px-4 py-4 flex items-center justify-between border-b border-gray-200 sticky top-0 z-40">
        <button onClick={onBack} className="p-1">
          <ArrowLeft className="w-6 h-6 text-gray-700" />
        </button>
        <h1 className="text-gray-900">新建预约 - 步骤1/2</h1>
        <div className="w-8"></div>
      </div>

      {/* Progress Bar */}
      <div className="bg-white px-4 py-3 border-b border-gray-200">
        <div className="flex items-center gap-2">
          <div className="flex-1 h-2 bg-indigo-500 rounded-full"></div>
          <div className="flex-1 h-2 bg-gray-200 rounded-full"></div>
        </div>
        <div className="flex items-center justify-between mt-2 text-xs">
          <span className="text-indigo-500">基本信息</span>
          <span className="text-gray-400">选择项目</span>
        </div>
      </div>

      <div className="px-4 pt-4 space-y-4">
        {/* Animal Info Section */}
        <div className="bg-white rounded-2xl p-5">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-4 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">动物信息</h3>
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
                placeholder="请输入宠物英文名"
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

            {/* Breed ID */}
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                品种ID
              </label>
              <input
                type="number"
                value={formData.AnimalTypeId || ''}
                onChange={(e) => setFormData({ ...formData, AnimalTypeId: Number(e.target.value) || 0 })}
                placeholder="请输入品种ID"
                className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors"
              />
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
                出生日期
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

            {/* Age */}
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

            <div>
              <label className="block text-sm text-gray-700 mb-2">
                送检医师姓名 <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.DoctorName}
                onChange={(e) => setFormData({ ...formData, DoctorName: e.target.value })}
                placeholder="请输入送检医师姓名"
                className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors"
              />
            </div>
          </div>
        </div>

        {/* Sample Info Section */}
        <div className="bg-white rounded-2xl p-5">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-4 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">采样信息</h3>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                采样类型 <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <select
                  value={formData.InfoSampleTypeId}
                  onChange={(e) => setFormData({ ...formData, InfoSampleTypeId: Number(e.target.value) })}
                  className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors appearance-none"
                >
                  {sampleTypes.map(type => (
                    <option key={type.id} value={type.id}>
                      {type.name}
                    </option>
                  ))}
                </select>
                <ChevronRight className="w-5 h-5 text-gray-400 absolute right-4 top-1/2 -translate-y-1/2 rotate-90 pointer-events-none" />
              </div>
            </div>

            <div>
              <label className="block text-sm text-gray-700 mb-2">
                采样部位 <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.SampleArea}
                onChange={(e) => setFormData({ ...formData, SampleArea: e.target.value })}
                placeholder="如：前肢、后肢等"
                className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors"
              />
            </div>

            <div>
              <label className="block text-sm text-gray-700 mb-2">
                采样日期 <span className="text-red-500">*</span>
              </label>
              <input
                type="date"
                value={formData.CollectDt}
                onChange={(e) => setFormData({ ...formData, CollectDt: e.target.value })}
                className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors"
              />
            </div>
          </div>
        </div>

        {/* Other Info Section */}
        <div className="bg-white rounded-2xl p-5">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-4 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">其他信息</h3>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                城市
              </label>
              <div className="relative">
                <select
                  value={formData.CityId}
                  onChange={(e) => setFormData({ ...formData, CityId: Number(e.target.value) })}
                  className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors appearance-none"
                >
                  <option value={0}>请选择城市</option>
                  {cities.map(city => (
                    <option key={city.id} value={city.id}>
                      {city.name}
                    </option>
                  ))}
                </select>
                <ChevronRight className="w-5 h-5 text-gray-400 absolute right-4 top-1/2 -translate-y-1/2 rotate-90 pointer-events-none" />
              </div>
            </div>

            <div>
              <label className="block text-sm text-gray-700 mb-2">
                备注
              </label>
              <textarea
                value={formData.Remark}
                onChange={(e) => setFormData({ ...formData, Remark: e.target.value })}
                placeholder="如选择狂犬检测，请填写快递单号"
                rows={3}
                className="w-full px-4 py-3 border border-gray-200 rounded-2xl outline-none focus:border-indigo-500 transition-colors resize-none"
              />
              <div className="text-xs text-gray-500 mt-1">
                狂犬检测必填快递单号
              </div>
            </div>
          </div>
        </div>

        {/* Info Card */}
        <div className="bg-blue-50 border border-blue-200 rounded-2xl p-4 text-xs text-blue-800">
          <div className="mb-2">💡 温馨提示：</div>
          <ul className="space-y-1 ml-4 list-disc">
            <li>标有<span className="text-red-500">*</span>的字段为必填项</li>
            <li>选择狂犬检测时，必须填写芯片号、出生日期和快递单号</li>
            <li>采样日期默认为当前日期，可修改</li>
            <li>填写完成后，点击"下一步"选择检测项目</li>
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
          下一步：选择检测项目
        </GradientButton>
      </div>
    </div>
  );
}