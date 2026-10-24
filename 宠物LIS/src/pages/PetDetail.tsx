import React from 'react';
import { Edit2, ChevronRight, FileText } from 'lucide-react';
import { GradientButton } from '../components/GradientButton';

interface PetDetailProps {
  petId: string;
  onBack: () => void;
  onEdit: () => void;
  onBookTest: () => void;
  onViewReport?: (reportId: string) => void;
}

export function PetDetail({ petId, onBack, onEdit, onBookTest, onViewReport }: PetDetailProps) {
  const petData = {
    id: petId,
    AnimalName: '小花',
    AnimalName_en: 'Xiaohua',
    InfoAnimalId: 1, // 猫
    AnimalTypeId: 1,
    breed: '英国短毛猫',
    Age: '2岁3个月',
    DictSexId: 2, // 母
    AnimalWeight: '4.5',
    ChipNO: 'CN123456789',
    Birthday: '2021-06-15',
    MasterName: '张三',
    MasterName_en: 'Zhang San',
    avatar: '🐱',
    testHistory: [
      { id: 'report1', reportId: 'R001', type: '血常规', date: '2023-06-15', result: '正常' },
      { id: 'report2', reportId: 'R002', type: '生化全项', date: '2023-03-20', result: '正常' },
      { id: 'report3', reportId: 'R003', type: 'DNA检测', date: '2023-01-10', result: '健康' },
    ],
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

  const getSpeciesText = (speciesId: number) => {
    const speciesMap: { [key: number]: string } = {
      1: '猫',
      2: '狗',
      3: '兔',
      4: '乌龟',
      5: '其他',
    };
    return speciesMap[speciesId] || '未知';
  };

  const handleViewReport = (reportId: string) => {
    if (onViewReport) {
      onViewReport(reportId);
    } else {
      alert(`查看报告: ${reportId}`);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      {/* Header with Gradient Background */}
      <div className="bg-gradient-to-r from-indigo-500 to-purple-600 pt-12 pb-8 px-4 relative">
        <button
          onClick={onBack}
          className="absolute top-4 left-4 w-8 h-8 bg-white/20 rounded-full flex items-center justify-center"
        >
          <span className="text-white text-xl">←</span>
        </button>
        <button
          onClick={onEdit}
          className="absolute top-4 right-4 w-8 h-8 bg-white/20 rounded-full flex items-center justify-center"
        >
          <Edit2 className="w-4 h-4 text-white" />
        </button>

        <div className="flex flex-col items-center pt-6">
          <h1 className="text-white mb-1 text-2xl">{petData.AnimalName}</h1>
          <div className="text-sm text-white/80">{petData.AnimalName_en}</div>
        </div>
      </div>

      {/* Quick Stats Bar */}
      <div className="bg-white border-b border-gray-200">
        <div className="grid grid-cols-4 divide-x divide-gray-200">
          <div className="py-4 text-center">
            <div className="text-xs text-gray-500 mb-1">物种</div>
            <div className="text-sm text-gray-900">{getSpeciesText(petData.InfoAnimalId)}</div>
          </div>
          <div className="py-4 text-center">
            <div className="text-xs text-gray-500 mb-1">年龄</div>
            <div className="text-sm text-gray-900">{petData.Age.split('个月')[0]}</div>
          </div>
          <div className="py-4 text-center">
            <div className="text-xs text-gray-500 mb-1">性别</div>
            <div className="text-sm text-gray-900">{getGenderText(petData.DictSexId)}</div>
          </div>
          <div className="py-4 text-center">
            <div className="text-xs text-gray-500 mb-1">体重</div>
            <div className="text-sm text-gray-900">{petData.AnimalWeight}kg</div>
          </div>
        </div>
      </div>

      <div className="px-4 pt-4 space-y-3">
        {/* Basic Info Card */}
        <div className="bg-white rounded-2xl p-5">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-5 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">基本信息</h3>
          </div>

          <div className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
            <div className="col-span-2">
              <span className="text-gray-500">中文名：</span>
              <span className="text-gray-900">{petData.AnimalName}</span>
            </div>
            <div className="col-span-2">
              <span className="text-gray-500">英文名：</span>
              <span className="text-gray-900">{petData.AnimalName_en}</span>
            </div>
            <div>
              <span className="text-gray-500">物种：</span>
              <span className="text-gray-900">{getSpeciesText(petData.InfoAnimalId)}</span>
            </div>
            <div>
              <span className="text-gray-500">品种：</span>
              <span className="text-gray-900">{petData.breed}</span>
            </div>
            <div className="col-span-2">
              <span className="text-gray-500">出生日期：</span>
              <span className="text-gray-900">{petData.Birthday} ({petData.Age})</span>
            </div>
            <div>
              <span className="text-gray-500">性别：</span>
              <span className="text-gray-900">{getGenderText(petData.DictSexId)}</span>
            </div>
            <div>
              <span className="text-gray-500">体重：</span>
              <span className="text-gray-900">{petData.AnimalWeight}kg</span>
            </div>
            <div className="col-span-2">
              <span className="text-gray-500">芯片号：</span>
              <span className="text-gray-900 font-mono text-xs">{petData.ChipNO}</span>
            </div>
            <div className="col-span-2">
              <span className="text-gray-500">主人名：</span>
              <span className="text-gray-900">{petData.MasterName} ({petData.MasterName_en})</span>
            </div>
          </div>
        </div>

        {/* Testing History Card */}
        <div className="bg-white rounded-2xl p-5">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-5 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">检测报告</h3>
          </div>

          <div className="space-y-3">
            {petData.testHistory.map((test) => (
              <button
                key={test.id}
                onClick={() => handleViewReport(test.reportId)}
                className="w-full flex items-center justify-between py-3 px-3 border border-gray-100 rounded-xl hover:bg-gray-50 hover:border-indigo-200 transition-all"
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-gradient-to-br from-indigo-100 to-purple-100 rounded-lg flex items-center justify-center flex-shrink-0">
                    <FileText className="w-5 h-5 text-indigo-600" />
                  </div>
                  <div className="text-left">
                    <div className="text-sm text-gray-900 mb-1">{test.type}</div>
                    <div className="text-xs text-gray-500">
                      {test.date} · 结果: <span className="text-emerald-600">{test.result}</span>
                    </div>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-gray-400" />
              </button>
            ))}
          </div>

          {petData.testHistory.length === 0 && (
            <div className="text-center py-8">
              <div className="text-gray-400 mb-2">暂无检测报告</div>
              <div className="text-xs text-gray-400">完成检测后，报告将显示在这里</div>
            </div>
          )}
        </div>
      </div>

      {/* Action Buttons */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-4 py-4 flex gap-3 max-w-md mx-auto">
        <GradientButton variant="outline" size="md" onClick={onEdit} className="flex-1">
          编辑信息
        </GradientButton>
        <GradientButton variant="purple" size="md" onClick={onBookTest} className="flex-1">
          预约检测
        </GradientButton>
      </div>
    </div>
  );
}