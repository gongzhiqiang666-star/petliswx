import React, { useState } from 'react';
import { ArrowLeft, Plus, ChevronDown, CheckCircle, Calendar, Activity } from 'lucide-react';
import { GradientButton } from '../components/GradientButton';

interface PetHealthProps {
  petId: string;
  onBack: () => void;
}

type TabType = 'vaccines' | 'checkups' | 'medical';

export function PetHealth({ petId, onBack }: PetHealthProps) {
  const [activeTab, setActiveTab] = useState<TabType>('vaccines');
  const [selectedPet, setSelectedPet] = useState('小花');

  const vaccineRecords = [
    {
      id: 1,
      name: '狂犬疫苗',
      date: '2023-12-01',
      dose: '第3针',
      nextDate: '2024-12-01',
      validUntil: '2025-12-01',
      type: 'required',
    },
    {
      id: 2,
      name: '猫三联疫苗',
      date: '2023-11-15',
      dose: '第2针',
      nextDate: '2024-11-15',
      validUntil: '2025-11-15',
      type: 'required',
    },
    {
      id: 3,
      name: '猫瘟疫苗',
      date: '2023-10-20',
      dose: '第1针',
      nextDate: '2024-10-20',
      validUntil: '2025-10-20',
      type: 'recommended',
    },
  ];

  const checkupRecords = [
    {
      id: 1,
      date: '2023-06-15',
      weight: '4.5kg',
      weightChange: '+0.2kg',
      doctor: '李医生',
      summary: '各项指标正常',
    },
    {
      id: 2,
      date: '2023-03-10',
      weight: '4.3kg',
      weightChange: '+0.1kg',
      doctor: '王医生',
      summary: '健康状况良好',
    },
    {
      id: 3,
      date: '2022-12-01',
      weight: '4.2kg',
      weightChange: '-',
      doctor: '李医生',
      summary: '首次体检，状态良好',
    },
  ];

  const medicalRecords = [
    {
      id: 1,
      type: '就诊',
      date: '2023-08-20',
      doctor: '张医生',
      diagnosis: '轻微感冒',
      treatment: '开具消炎药，多休息，多喝水',
      followUp: '一周后复查',
    },
    {
      id: 2,
      type: '用药',
      date: '2023-05-10',
      doctor: '李医生',
      diagnosis: '驱虫治疗',
      treatment: '口服驱虫药',
      followUp: '三个月后再次驱虫',
    },
  ];

  const tabs = [
    { id: 'vaccines' as const, label: '疫苗记录' },
    { id: 'checkups' as const, label: '体检记录' },
    { id: 'medical' as const, label: '医疗记录' },
  ];

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      {/* Header */}
      <div className="bg-white px-4 py-4 flex items-center justify-between border-b border-gray-200">
        <div className="flex items-center gap-3">
          <button onClick={onBack} className="p-1">
            <ArrowLeft className="w-6 h-6 text-gray-700" />
          </button>
          <h1 className="text-gray-900">健康记录</h1>
        </div>

        {/* Pet Selector */}
        <button className="flex items-center gap-1 px-3 py-2 bg-gray-50 rounded-lg">
          <span className="text-sm text-gray-900">{selectedPet}</span>
          <ChevronDown className="w-4 h-4 text-gray-600" />
        </button>
      </div>

      {/* Tab Navigation */}
      <div className="bg-white border-b border-gray-200">
        <div className="flex">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex-1 py-4 relative ${
                activeTab === tab.id ? 'text-indigo-500' : 'text-gray-500'
              }`}
            >
              {tab.label}
              {activeTab === tab.id && (
                <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-500"></div>
              )}
            </button>
          ))}
        </div>
      </div>

      <div className="px-4 pt-4">
        {/* Vaccines Tab */}
        {activeTab === 'vaccines' && (
          <div className="space-y-3">
            {vaccineRecords.map((vaccine) => (
              <div key={vaccine.id} className="bg-white rounded-2xl p-5">
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <h3 className="text-gray-900 mb-1">{vaccine.name}</h3>
                    <div className="text-sm text-gray-500">
                      {vaccine.date} ({vaccine.dose})
                    </div>
                  </div>
                  <span
                    className={`text-xs px-2 py-1 rounded ${
                      vaccine.type === 'required'
                        ? 'bg-red-50 text-red-600'
                        : 'bg-blue-50 text-blue-600'
                    }`}
                  >
                    {vaccine.type === 'required' ? '必须疫苗' : '推荐疫苗'}
                  </span>
                </div>

                <div className="space-y-2 text-sm">
                  <div className="flex items-center gap-2 text-gray-600">
                    <Calendar className="w-4 h-4" />
                    <span>下次: {vaccine.nextDate}</span>
                  </div>
                  <div className="flex items-center gap-2 text-emerald-600">
                    <CheckCircle className="w-4 h-4" />
                    <span>有效期至 {vaccine.validUntil}</span>
                  </div>
                </div>
              </div>
            ))}

            <button className="w-full bg-white border-2 border-dashed border-gray-300 rounded-2xl py-4 flex items-center justify-center gap-2 text-indigo-500 hover:border-indigo-300 transition-colors">
              <Plus className="w-5 h-5" />
              <span>添加疫苗记录</span>
            </button>
          </div>
        )}

        {/* Checkups Tab */}
        {activeTab === 'checkups' && (
          <div className="space-y-4">
            {/* Timeline */}
            <div className="relative">
              {checkupRecords.map((checkup, index) => (
                <div key={checkup.id} className="relative pl-8 pb-6 last:pb-0">
                  {/* Timeline dot */}
                  <div className="absolute left-0 top-2 w-4 h-4 bg-indigo-500 rounded-full border-4 border-white shadow"></div>
                  
                  {/* Timeline line */}
                  {index < checkupRecords.length - 1 && (
                    <div className="absolute left-2 top-6 w-0.5 h-full bg-gray-200 -ml-px"></div>
                  )}

                  {/* Content */}
                  <div className="bg-white rounded-2xl p-4">
                    <div className="flex items-center justify-between mb-2">
                      <div className="text-sm text-gray-900">{checkup.date}</div>
                      <div className="text-sm text-gray-500">{checkup.doctor}</div>
                    </div>

                    <div className="space-y-2">
                      <div className="flex items-center gap-2 text-sm">
                        <span className="text-gray-500">体重:</span>
                        <span className="text-gray-900">{checkup.weight}</span>
                        {checkup.weightChange !== '-' && (
                          <span className="text-xs text-emerald-600">({checkup.weightChange})</span>
                        )}
                      </div>
                      <div className="text-sm text-gray-700">{checkup.summary}</div>
                    </div>

                    <button className="mt-3 text-sm text-indigo-500 flex items-center gap-1">
                      查看详情
                      <span>→</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>

            <button className="w-full bg-white border-2 border-dashed border-gray-300 rounded-2xl py-4 flex items-center justify-center gap-2 text-indigo-500 hover:border-indigo-300 transition-colors">
              <Plus className="w-5 h-5" />
              <span>添加体检记录</span>
            </button>
          </div>
        )}

        {/* Medical Tab */}
        {activeTab === 'medical' && (
          <div className="space-y-3">
            {medicalRecords.map((record) => (
              <div key={record.id} className="bg-white rounded-2xl p-5">
                <div className="flex items-start justify-between mb-3">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span
                        className={`text-xs px-2 py-1 rounded ${
                          record.type === '就诊'
                            ? 'bg-orange-50 text-orange-600'
                            : 'bg-blue-50 text-blue-600'
                        }`}
                      >
                        {record.type}
                      </span>
                      <span className="text-sm text-gray-500">{record.date}</span>
                    </div>
                    <h3 className="text-gray-900">{record.diagnosis}</h3>
                  </div>
                </div>

                <div className="space-y-2 text-sm">
                  <div>
                    <span className="text-gray-500">医生: </span>
                    <span className="text-gray-900">{record.doctor}</span>
                  </div>
                  <div>
                    <span className="text-gray-500">治疗: </span>
                    <span className="text-gray-900">{record.treatment}</span>
                  </div>
                  <div>
                    <span className="text-gray-500">复查: </span>
                    <span className="text-gray-900">{record.followUp}</span>
                  </div>
                </div>
              </div>
            ))}

            <button className="w-full bg-white border-2 border-dashed border-gray-300 rounded-2xl py-4 flex items-center justify-center gap-2 text-indigo-500 hover:border-indigo-300 transition-colors">
              <Plus className="w-5 h-5" />
              <span>添加医疗记录</span>
            </button>
          </div>
        )}
      </div>

      {/* Floating Action Button */}
      <button
        className="fixed bottom-24 right-6 w-14 h-14 bg-gradient-to-r from-indigo-500 to-purple-600 rounded-full flex items-center justify-center shadow-lg hover:shadow-xl transition-shadow z-40"
        style={{ boxShadow: '0 4px 12px rgba(99, 102, 241, 0.4)' }}
      >
        <Plus className="w-6 h-6 text-white" />
      </button>
    </div>
  );
}
