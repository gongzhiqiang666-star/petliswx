import React from 'react';
import { Calendar, FileText, Bell, Clock, Sparkles, ArrowRight } from 'lucide-react';
import { GradientButton } from '../components/GradientButton';

interface HomePageProps {
  onNavigate: (page: string) => void;
}

export function HomePage({ onNavigate }: HomePageProps) {
  const recentActivities = [
    { id: '1', type: 'report', title: '小花的血常规检查报告已出', description: '详细报告请查看', time: '2小时前' },
    { id: '2', type: 'order', title: '小黑的检测预约已确认', description: '预约详情请查看', time: '1天前' },
    { id: '3', type: 'appointment', title: '喵喵的年度体检即将到期', description: '请尽快预约', time: '3天前' },
  ];

  return (
    <div className="min-h-screen bg-gray-50 pb-20">
      {/* Minimal Hero Section */}
      <div className="bg-gradient-to-br from-indigo-500 via-purple-600 to-indigo-700 px-6 pt-20 pb-40 relative overflow-hidden">
        {/* Decorative Elements */}
        <div className="absolute top-20 right-10 w-40 h-40 bg-white/10 rounded-full blur-3xl"></div>
        <div className="absolute bottom-32 left-10 w-48 h-48 bg-white/10 rounded-full blur-3xl"></div>
        <div className="absolute top-40 left-20 w-32 h-32 bg-white/5 rounded-full blur-2xl"></div>
        
        <div className="relative max-w-md mx-auto">
          {/* Top Bar */}
          <div className="flex items-center justify-between mb-20">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 bg-white/20 backdrop-blur-sm rounded-2xl flex items-center justify-center">
                <Sparkles className="w-5 h-5 text-white" />
              </div>
              <div>
                <div className="text-white/80 text-xs mb-0.5">欢迎回来</div>
                <div className="text-white">宠物主人</div>
              </div>
            </div>
            <button className="w-11 h-11 bg-white/20 backdrop-blur-sm rounded-2xl flex items-center justify-center relative">
              <Bell className="w-5 h-5 text-white" />
              <span className="absolute top-2 right-2 w-2 h-2 bg-orange-400 rounded-full border-2 border-white/50"></span>
            </button>
          </div>

          {/* Hero Text */}
          <div className="text-center space-y-4">
            <h1 className="text-white text-3xl leading-tight">
              关爱宠物健康
            </h1>
            <p className="text-white/70 text-sm leading-relaxed">
              科学检测，专业呵护每一天
            </p>
          </div>
        </div>
      </div>

      {/* Quick Actions - Floating Card */}
      <div className="px-6 -mt-28 relative z-10 max-w-md mx-auto">
        <div className="bg-white rounded-3xl p-8 shadow-2xl">
          <div className="grid grid-cols-1 gap-4">
            <button
              onClick={() => onNavigate('order-select-pet')}
              className="bg-gradient-to-br from-indigo-500 to-purple-600 rounded-2xl p-6 hover:shadow-lg transition-all group text-left relative overflow-hidden"
            >
              <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full blur-2xl"></div>
              <div className="relative flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="w-14 h-14 bg-white/20 backdrop-blur-sm rounded-2xl flex items-center justify-center group-hover:scale-110 transition-transform">
                    <Calendar className="w-7 h-7 text-white" />
                  </div>
                  <div className="text-white">预约检测</div>
                </div>
                <ArrowRight className="w-5 h-5 text-white/60 group-hover:text-white group-hover:translate-x-1 transition-all" />
              </div>
            </button>

            <button
              onClick={() => onNavigate('pet-add-from-home')}
              className="bg-gradient-to-br from-violet-500 to-fuchsia-600 rounded-2xl p-6 hover:shadow-lg transition-all group text-left relative overflow-hidden"
            >
              <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full blur-2xl"></div>
              <div className="relative flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="w-14 h-14 bg-white/20 backdrop-blur-sm rounded-2xl flex items-center justify-center group-hover:scale-110 transition-transform">
                    <Sparkles className="w-7 h-7 text-white" />
                  </div>
                  <div className="text-white">宠物建档</div>
                </div>
                <ArrowRight className="w-5 h-5 text-white/60 group-hover:text-white group-hover:translate-x-1 transition-all" />
              </div>
            </button>

            <button
              onClick={() => onNavigate('reports')}
              className="bg-gradient-to-br from-emerald-500 to-teal-600 rounded-2xl p-6 hover:shadow-lg transition-all group text-left relative overflow-hidden"
            >
              <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full blur-2xl"></div>
              <div className="relative flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="w-14 h-14 bg-white/20 backdrop-blur-sm rounded-2xl flex items-center justify-center group-hover:scale-110 transition-transform">
                    <FileText className="w-7 h-7 text-white" />
                  </div>
                  <div className="text-white">查看报告</div>
                </div>
                <ArrowRight className="w-5 h-5 text-white/60 group-hover:text-white group-hover:translate-x-1 transition-all" />
              </div>
            </button>
          </div>
        </div>
      </div>

      {/* Recent Activities */}
      <div className="px-6 pt-12 max-w-md mx-auto">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-gray-900">最近动态</h2>
          <button className="text-sm text-indigo-500 hover:text-indigo-600 transition-colors">查看全部</button>
        </div>

        <div className="space-y-3">
          {recentActivities.map((activity) => (
            <div
              key={activity.id}
              className="bg-white rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow"
            >
              <div className="flex items-start gap-4">
                <div className={`w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0 ${
                  activity.type === 'report' 
                    ? 'bg-emerald-100' 
                    : activity.type === 'order' 
                    ? 'bg-indigo-100' 
                    : 'bg-orange-100'
                }`}>
                  {activity.type === 'report' && (
                    <FileText className="w-5 h-5 text-emerald-600" />
                  )}
                  {activity.type === 'order' && (
                    <Calendar className="w-5 h-5 text-indigo-600" />
                  )}
                  {activity.type === 'appointment' && (
                    <Clock className="w-5 h-5 text-orange-600" />
                  )}
                </div>
                
                <div className="flex-1 min-w-0">
                  <div className="text-sm text-gray-900 mb-1 leading-relaxed">
                    {activity.title}
                  </div>
                  <div className="text-xs text-gray-500 mb-2">
                    {activity.description}
                  </div>
                  <div className="text-xs text-gray-400">
                    {activity.time}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}