import React, { useState } from 'react';
import { Calendar, List, Circle } from 'lucide-react';
import { StatusBadge } from '../components/StatusBadge';
import { GradientButton } from '../components/GradientButton';

interface OrderListProps {
  onViewDetail: (orderId: string) => void;
  onNewOrder: () => void;
}

type OrderStatus = 'all' | 'pending' | 'in-progress' | 'completed';

export function OrderList({ onViewDetail, onNewOrder }: OrderListProps) {
  const [activeTab, setActiveTab] = useState<OrderStatus>('all');

  const orders = [
    {
      id: 'ORD20240120001',
      petName: '小花',
      petBreed: '英国短毛猫',
      petAvatar: '🐱',
      date: '2024-01-20 14:00',
      items: ['血常规检查', '生化全项'],
      totalItems: 2,
      price: 200,
      status: 'completed' as const,
    },
    {
      id: 'ORD20240122002',
      petName: '小黑',
      petBreed: '金毛寻回犬',
      petAvatar: '🐕',
      date: '2024-01-25 10:00',
      items: ['基因检测', '过敏原检测'],
      totalItems: 3,
      price: 450,
      status: 'in-progress' as const,
    },
    {
      id: 'ORD20240123003',
      petName: '喵喵',
      petBreed: '布偶猫',
      petAvatar: '🐈',
      date: '2024-01-28 15:00',
      items: ['血常规检查'],
      totalItems: 1,
      price: 50,
      status: 'pending' as const,
    },
  ];

  const filteredOrders = activeTab === 'all' 
    ? orders 
    : orders.filter(order => order.status === activeTab);

  const tabs = [
    { id: 'all' as const, label: '全部' },
    { id: 'pending' as const, label: '待支付' },
    { id: 'in-progress' as const, label: '进行中' },
    { id: 'completed' as const, label: '已完成' },
  ];

  const getStatusLabel = (status: string) => {
    const labels = {
      pending: '待支付',
      'in-progress': '进行中',
      completed: '已完成',
      cancelled: '已取消',
    };
    return labels[status as keyof typeof labels];
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-20">
      {/* Header */}
      <div className="bg-white px-4 pt-8 pb-4 sticky top-0 z-40">
        <h1 className="text-gray-900 mb-4">我的订单</h1>
        
        {/* Tabs */}
        <div className="flex justify-between border-b border-gray-200">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`pb-3 px-2 relative transition-colors ${
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

      {/* Order List */}
      <div className="px-4 pt-4 space-y-3">
        {filteredOrders.length > 0 ? (
          filteredOrders.map((order) => (
            <div key={order.id} className="bg-white rounded-2xl p-5 shadow-sm">
              {/* Status Badge */}
              <div className="flex justify-end mb-3">
                <StatusBadge type={order.status}>
                  {getStatusLabel(order.status)}
                </StatusBadge>
              </div>

              {/* Pet Info */}
              <div className="flex items-center gap-3 mb-4">
                <div>
                  <div className="text-gray-900 mb-1">{order.petName}</div>
                  <div className="text-sm text-gray-500">{order.petBreed}</div>
                </div>
              </div>

              {/* Appointment Info */}
              <div className="space-y-2 mb-4">
                <div className="flex items-center gap-2 text-gray-600 text-sm">
                  <Calendar className="w-4 h-4" />
                  <span>预约时间：{order.date}</span>
                </div>
                <div className="flex items-center gap-2 text-gray-600 text-sm">
                  <List className="w-4 h-4" />
                  <span>检测项目：{order.items.join('、')}{order.totalItems > 2 ? ` 等${order.totalItems}项` : ''}</span>
                </div>
              </div>

              {/* Price and Actions */}
              <div className="flex items-center justify-between pt-3 border-t border-gray-100">
                <div>
                  <span className="text-indigo-500">¥{order.price}</span>
                </div>
                <div className="flex gap-2">
                  {order.status === 'pending' && (
                    <>
                      <GradientButton variant="outline" size="sm" className="text-sm">
                        取消订单
                      </GradientButton>
                      <GradientButton variant="green" size="sm" className="text-sm">
                        立即支付
                      </GradientButton>
                    </>
                  )}
                  <GradientButton 
                    variant="purple" 
                    size="sm" 
                    onClick={() => onViewDetail(order.id)}
                    className="text-sm"
                  >
                    查看详情
                  </GradientButton>
                </div>
              </div>
            </div>
          ))
        ) : (
          <div className="flex flex-col items-center justify-center py-16">
            <div className="w-32 h-32 bg-gray-100 rounded-full flex items-center justify-center mb-4">
              <Circle className="w-16 h-16 text-gray-300" />
            </div>
            <div className="text-gray-400 mb-6">暂无订单</div>
            <GradientButton variant="purple" size="md" onClick={onNewOrder}>
              去预约
            </GradientButton>
          </div>
        )}
      </div>
    </div>
  );
}