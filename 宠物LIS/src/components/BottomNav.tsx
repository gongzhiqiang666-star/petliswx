import React from 'react';
import { Home, PawPrint, ShoppingBag, User } from 'lucide-react';

interface BottomNavProps {
  active: 'home' | 'pets' | 'orders' | 'mine';
  onNavigate: (tab: 'home' | 'pets' | 'orders' | 'mine') => void;
}

export function BottomNav({ active, onNavigate }: BottomNavProps) {
  const tabs = [
    { id: 'home' as const, icon: Home, label: '首页' },
    { id: 'pets' as const, icon: PawPrint, label: '宠物' },
    { id: 'orders' as const, icon: ShoppingBag, label: '订单' },
    { id: 'mine' as const, icon: User, label: '我的' },
  ];

  return (
    <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 h-16 flex items-center justify-around px-4 z-50 max-w-md mx-auto">
      {tabs.map((tab) => {
        const Icon = tab.icon;
        const isActive = active === tab.id;
        return (
          <button
            key={tab.id}
            onClick={() => onNavigate(tab.id)}
            className="flex flex-col items-center justify-center gap-1 flex-1"
          >
            <Icon 
              className={`w-6 h-6 ${isActive ? 'text-indigo-500' : 'text-gray-400'}`}
              strokeWidth={isActive ? 2.5 : 2}
            />
            <span className={`text-xs ${isActive ? 'text-indigo-500' : 'text-gray-400'}`}>
              {tab.label}
            </span>
          </button>
        );
      })}
    </div>
  );
}