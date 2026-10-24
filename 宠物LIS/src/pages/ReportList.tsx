import React, { useState } from 'react';
import { Search, Filter, CheckCircle, AlertTriangle, Clock, ChevronRight } from 'lucide-react';
import { GradientButton } from '../components/GradientButton';

interface ReportListProps {
  onViewReport: (reportId: string) => void;
  onNewAppointment: () => void;
}

export function ReportList({ onViewReport, onNewAppointment }: ReportListProps) {
  const [activeCategory, setActiveCategory] = useState('all');

  const categories = [
    { id: 'all', label: '全部报告', count: 5 },
    { id: 'pending', label: '待出', count: 1 },
    { id: 'completed', label: '已完成', count: 4 },
  ];

  const reports = [
    {
      id: 'RPT001',
      type: '血常规检查报告',
      petName: '小花',
      petBreed: '英短',
      petAvatar: '🐱',
      date: '2024-01-25',
      status: 'normal' as const,
      statusLabel: '正常',
    },
    {
      id: 'RPT002',
      type: '生化全项报告',
      petName: '小黑',
      petBreed: '金毛',
      petAvatar: '🐕',
      date: '2024-01-23',
      status: 'warning' as const,
      statusLabel: '需注意',
    },
    {
      id: 'RPT003',
      type: '基因检测报告',
      petName: '喵喵',
      petBreed: '布偶',
      petAvatar: '🐈',
      date: '2024-01-20',
      status: 'normal' as const,
      statusLabel: '正常',
    },
    {
      id: 'RPT004',
      type: '过敏原检测报告',
      petName: '小白',
      petBreed: '萨摩耶',
      petAvatar: '🐕',
      date: '2024-01-18',
      status: 'warning' as const,
      statusLabel: '需注意',
    },
    {
      id: 'RPT005',
      type: '血常规检查',
      petName: '小花',
      petBreed: '英短',
      petAvatar: '🐱',
      date: '预计2024-01-30出报告',
      status: 'waiting' as const,
      statusLabel: '检测中',
    },
  ];

  const filteredReports = activeCategory === 'all'
    ? reports
    : activeCategory === 'pending'
    ? reports.filter(r => r.status === 'waiting')
    : reports.filter(r => r.status !== 'waiting');

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'normal':
        return <CheckCircle className="w-5 h-5 text-emerald-500" />;
      case 'warning':
        return <AlertTriangle className="w-5 h-5 text-orange-500" />;
      case 'waiting':
        return <Clock className="w-5 h-5 text-gray-400" />;
      default:
        return null;
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'normal':
        return <span className="text-sm px-3 py-1 rounded-full bg-emerald-50 text-emerald-600">正常</span>;
      case 'warning':
        return <span className="text-sm px-3 py-1 rounded-full bg-orange-50 text-orange-600">需注意</span>;
      case 'waiting':
        return <span className="text-sm px-3 py-1 rounded-full bg-gray-100 text-gray-600">检测中</span>;
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-20">
      {/* Header */}
      <div className="bg-white px-4 pt-8 pb-4">
        <h1 className="text-gray-900 mb-1">检测报告</h1>
        <p className="text-sm text-gray-500">查看您的宠物检测报告</p>
      </div>

      {/* Search and Filter */}
      <div className="px-4 pt-4 flex gap-2">
        <div className="flex-1 bg-white rounded-2xl px-4 py-3 flex items-center gap-2 border border-gray-200">
          <Search className="w-5 h-5 text-gray-400" />
          <input
            type="text"
            placeholder="搜索报告"
            className="flex-1 bg-transparent outline-none text-gray-900 placeholder-gray-400"
          />
        </div>
        <button className="bg-white rounded-2xl px-4 py-3 border border-gray-200">
          <Filter className="w-5 h-5 text-gray-600" />
        </button>
      </div>

      {/* Category Pills */}
      <div className="px-4 pt-4 flex gap-2 overflow-x-auto pb-2">
        {categories.map((category) => (
          <button
            key={category.id}
            onClick={() => setActiveCategory(category.id)}
            className={`px-4 py-2 rounded-full whitespace-nowrap transition-all ${
              activeCategory === category.id
                ? 'bg-indigo-500 text-white'
                : 'bg-white text-gray-600 border border-gray-200'
            }`}
          >
            {category.label} ({category.count})
          </button>
        ))}
      </div>

      {/* Report List */}
      <div className="px-4 pt-4 space-y-3">
        {filteredReports.length > 0 ? (
          filteredReports.map((report) => (
            <div key={report.id} className="bg-white rounded-2xl p-5">
              <div className="flex justify-between items-start mb-3">
                <h3 className="text-gray-900 flex-1">{report.type}</h3>
                {getStatusIcon(report.status)}
              </div>

              <div className="flex items-center gap-3 mb-3">
                <div className="flex-1">
                  <div className="text-gray-900 mb-1">{report.petName}</div>
                  <div className="text-xs text-gray-500">{report.petBreed}</div>
                </div>
                {getStatusBadge(report.status)}
              </div>

              <div className="flex items-center justify-between text-sm mb-3">
                <span className="text-gray-500">{report.date}</span>
                {report.status !== 'waiting' && (
                  <button
                    onClick={() => onViewReport(report.id)}
                    className="text-indigo-500 flex items-center gap-1"
                  >
                    查看详情
                    <ChevronRight className="w-4 h-4" />
                  </button>
                )}
              </div>

              <div className="flex items-center justify-between pt-3 border-t border-gray-100">
                <div className="flex items-center gap-2">
                  <span className={`text-sm px-3 py-1 rounded-full ${
                    report.status === 'normal' ? 'bg-emerald-50 text-emerald-600' :
                    report.status === 'warning' ? 'bg-orange-50 text-orange-600' :
                    'bg-gray-100 text-gray-600'
                  }`}>
                    {report.statusLabel}
                  </span>
                </div>
                {report.status !== 'waiting' && (
                  <GradientButton
                    variant="purple"
                    size="sm"
                    onClick={() => onViewReport(report.id)}
                  >
                    查看完整报告
                  </GradientButton>
                )}
              </div>
            </div>
          ))
        ) : (
          <div className="flex flex-col items-center justify-center py-16">
            <div className="w-32 h-32 bg-gray-100 rounded-full flex items-center justify-center mb-4">
              <Clock className="w-16 h-16 text-gray-300" />
            </div>
            <div className="text-gray-400 mb-6">暂无检测报告</div>
            <GradientButton variant="purple" size="md" onClick={onNewAppointment}>
              去预约
            </GradientButton>
          </div>
        )}
      </div>
    </div>
  );
}