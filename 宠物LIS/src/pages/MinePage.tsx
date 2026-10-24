import React from 'react';
import { 
  ChevronRight, 
  Building2, 
  User, 
  Phone, 
  Mail, 
  MapPin, 
  LogOut,
  Settings,
  Bell,
  HelpCircle,
  Shield,
  Sparkles
} from 'lucide-react';

interface MinePageProps {
  onNavigate: (page: string) => void;
  onLogout: () => void;
}

export function MinePage({ onNavigate, onLogout }: MinePageProps) {
  // Mock user data
  const userData = {
    name: '张三',
    name_en: 'Zhang San',
    phone: '138****8888',
    email: 'zhangsan@example.com',
    userId: 'U20240001',
  };

  // Mock hospital data
  const hospitalData = {
    name: '爱宠宠物医院',
    name_en: 'iPet Animal Hospital',
    address: '北京市朝阳区宠物大道123号',
    phone: '010-12345678',
    businessHours: '09:00 - 21:00',
    hospitalId: 'H001',
  };

  const handleLogout = () => {
    if (window.confirm('确定要退出登录吗？')) {
      onLogout();
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-20">
      {/* Header with Gradient */}
      <div className="bg-gradient-to-br from-indigo-500 via-purple-600 to-indigo-700 pt-12 pb-20 px-6 relative overflow-hidden">
        {/* Decorative Elements */}
        <div className="absolute top-10 right-10 w-32 h-32 bg-white/10 rounded-full blur-3xl"></div>
        <div className="absolute bottom-10 left-10 w-40 h-40 bg-white/10 rounded-full blur-3xl"></div>

        <div className="relative max-w-md mx-auto">
          <h1 className="text-white text-2xl mb-2">我的</h1>
          <p className="text-white/70 text-sm">个人信息与系统设置</p>
        </div>
      </div>

      {/* User Info Card - Floating */}
      <div className="px-6 -mt-12 relative z-10 max-w-md mx-auto mb-6">
        <div className="bg-white rounded-3xl p-6 shadow-xl">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-full bg-gradient-to-br from-indigo-100 to-purple-100 flex items-center justify-center flex-shrink-0">
              <User className="w-8 h-8 text-indigo-500" />
            </div>
            <div className="flex-1">
              <div className="text-gray-900 mb-1">{userData.name}</div>
              <div className="text-sm text-gray-500 mb-1">{userData.name_en}</div>
              <div className="text-xs text-gray-400">ID: {userData.userId}</div>
            </div>
            <ChevronRight className="w-5 h-5 text-gray-400" />
          </div>

          <div className="mt-4 pt-4 border-t border-gray-100 grid grid-cols-2 gap-3 text-sm">
            <div className="flex items-center gap-2 text-gray-600">
              <Phone className="w-4 h-4 text-gray-400" />
              <span>{userData.phone}</span>
            </div>
            <div className="flex items-center gap-2 text-gray-600">
              <Mail className="w-4 h-4 text-gray-400" />
              <span className="truncate">{userData.email}</span>
            </div>
          </div>
        </div>
      </div>

      <div className="px-6 max-w-md mx-auto space-y-6">
        {/* Hospital Info Section */}
        <div className="bg-white rounded-2xl overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100">
            <div className="flex items-center gap-2">
              <Building2 className="w-5 h-5 text-indigo-500" />
              <h2 className="text-gray-900">医院信息</h2>
            </div>
          </div>
          
          <div className="px-5 py-4 space-y-3">
            <div>
              <div className="text-sm text-gray-500 mb-1">医院名称</div>
              <div className="text-gray-900">{hospitalData.name}</div>
              <div className="text-sm text-gray-500 mt-0.5">{hospitalData.name_en}</div>
            </div>
            
            <div className="flex items-start gap-2">
              <MapPin className="w-4 h-4 text-gray-400 mt-0.5 flex-shrink-0" />
              <div>
                <div className="text-sm text-gray-500 mb-1">地址</div>
                <div className="text-sm text-gray-900">{hospitalData.address}</div>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <div className="text-sm text-gray-500 mb-1">联系电话</div>
                <div className="text-sm text-gray-900">{hospitalData.phone}</div>
              </div>
              <div>
                <div className="text-sm text-gray-500 mb-1">营业时间</div>
                <div className="text-sm text-gray-900">{hospitalData.businessHours}</div>
              </div>
            </div>

            <div className="pt-2">
              <div className="text-xs text-gray-400">医院ID: {hospitalData.hospitalId}</div>
            </div>
          </div>
        </div>

        {/* Menu Items Section */}
        <div className="bg-white rounded-2xl overflow-hidden">
          <button
            onClick={() => onNavigate('icon-download')}
            className="w-full px-5 py-4 flex items-center justify-between hover:bg-gray-50 transition-colors border-b border-gray-100"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-indigo-100 flex items-center justify-center">
                <Sparkles className="w-5 h-5 text-indigo-600" />
              </div>
              <span className="text-gray-900">图标下载</span>
            </div>
            <ChevronRight className="w-5 h-5 text-gray-400" />
          </button>

          <button
            className="w-full px-5 py-4 flex items-center justify-between hover:bg-gray-50 transition-colors border-b border-gray-100"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-blue-100 flex items-center justify-center">
                <Bell className="w-5 h-5 text-blue-600" />
              </div>
              <span className="text-gray-900">消息通知</span>
            </div>
            <ChevronRight className="w-5 h-5 text-gray-400" />
          </button>

          <button
            className="w-full px-5 py-4 flex items-center justify-between hover:bg-gray-50 transition-colors border-b border-gray-100"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-purple-100 flex items-center justify-center">
                <Settings className="w-5 h-5 text-purple-600" />
              </div>
              <span className="text-gray-900">系统设置</span>
            </div>
            <ChevronRight className="w-5 h-5 text-gray-400" />
          </button>

          <button
            className="w-full px-5 py-4 flex items-center justify-between hover:bg-gray-50 transition-colors border-b border-gray-100"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-orange-100 flex items-center justify-center">
                <HelpCircle className="w-5 h-5 text-orange-600" />
              </div>
              <span className="text-gray-900">帮助中心</span>
            </div>
            <ChevronRight className="w-5 h-5 text-gray-400" />
          </button>

          <button
            className="w-full px-5 py-4 flex items-center justify-between hover:bg-gray-50 transition-colors"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-gray-100 flex items-center justify-center">
                <Shield className="w-5 h-5 text-gray-600" />
              </div>
              <span className="text-gray-900">隐私政策</span>
            </div>
            <ChevronRight className="w-5 h-5 text-gray-400" />
          </button>
        </div>

        {/* Logout Button */}
        <button
          onClick={handleLogout}
          className="w-full bg-white rounded-2xl px-5 py-4 flex items-center justify-center gap-2 hover:bg-red-50 transition-colors group"
        >
          <LogOut className="w-5 h-5 text-red-500 group-hover:text-red-600" />
          <span className="text-red-500 group-hover:text-red-600">退出登录</span>
        </button>

        {/* Version Info */}
        <div className="text-center text-xs text-gray-400 py-4">
          <div>宠物检测管理系统 v1.0.0</div>
          <div className="mt-1">© 2024 All Rights Reserved</div>
        </div>
      </div>
    </div>
  );
}