import React from 'react';
import { Calendar, Clock, MapPin, Phone, FileText, CheckCircle } from 'lucide-react';
import { PageHeader } from '../components/PageHeader';
import { GradientButton } from '../components/GradientButton';

interface OrderDetailProps {
  orderId: string;
  onBack: () => void;
  onRebook: () => void;
  onViewReport?: (reportId: string) => void;
}

export function OrderDetail({ orderId, onBack, onRebook, onViewReport }: OrderDetailProps) {
  const orderData = {
    id: orderId,
    status: '已确认',
    petName: '小花',
    petBreed: '英国短毛猫',
    petGender: '母',
    petAge: '2岁3个月',
    petWeight: '4.5kg',
    petAvatar: '🐱',
    appointmentDate: '2024年01月20日',
    appointmentTime: '14:00-15:00',
    location: 'Laboklin检测中心',
    phone: '400-123-4567',
    note: '宠物比较敏感，请温柔处理',
    items: [
      {
        name: '血常规检查',
        price: 50,
        description: '检查血液各项指标，评估整体健康状况',
      },
      {
        name: '生化全项',
        price: 150,
        description: '全面检查肝肾功能、电解质等',
      },
    ],
    totalPrice: 200,
    paymentMethod: '微信支付',
    paymentStatus: '已支付',
    paymentTime: '2024-01-20 13:45:32',
    transactionId: 'wx20240120134523456789',
    reportId: 'R001', // 关联的报告ID
    hasReport: true, // 是否有报告
  };

  const handleViewReport = () => {
    if (onViewReport && orderData.reportId) {
      onViewReport(orderData.reportId);
    } else {
      alert('报告暂未生成');
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      <PageHeader title="订单详情" onBack={onBack} />

      {/* Status Section */}
      <div className="bg-gradient-to-r from-indigo-500 to-purple-600 px-6 py-10 text-center">
        <div className="flex justify-center mb-3">
          <CheckCircle className="w-12 h-12 text-white" />
        </div>
        <div className="text-white mb-2">{orderData.status}</div>
        <div className="text-white/80 text-sm">订单号: {orderData.id}</div>
      </div>

      <div className="px-4 pt-4 space-y-4">
        {/* Pet Information */}
        <div className="bg-white rounded-2xl p-5">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-5 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">宠物信息</h3>
          </div>
          <div className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
            <div>
              <span className="text-gray-500">名字：</span>
              <span className="text-gray-900">{orderData.petName}</span>
            </div>
            <div>
              <span className="text-gray-500">品种：</span>
              <span className="text-gray-900">{orderData.petBreed}</span>
            </div>
            <div>
              <span className="text-gray-500">性别：</span>
              <span className="text-gray-900">{orderData.petGender}</span>
            </div>
            <div>
              <span className="text-gray-500">年龄：</span>
              <span className="text-gray-900">{orderData.petAge}</span>
            </div>
            <div className="col-span-2">
              <span className="text-gray-500">体重：</span>
              <span className="text-gray-900">{orderData.petWeight}</span>
            </div>
          </div>
        </div>

        {/* Appointment Information */}
        <div className="bg-white rounded-2xl p-5">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-5 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">预约信息</h3>
          </div>
          <div className="space-y-3 text-sm">
            <div className="flex items-center gap-3">
              <Calendar className="w-5 h-5 text-indigo-500" />
              <span className="text-gray-500">预约日期：</span>
              <span className="text-gray-900">{orderData.appointmentDate}</span>
            </div>
            <div className="flex items-center gap-3">
              <Clock className="w-5 h-5 text-indigo-500" />
              <span className="text-gray-500">预约时间：</span>
              <span className="text-gray-900">{orderData.appointmentTime}</span>
            </div>
            <div className="flex items-center gap-3">
              <MapPin className="w-5 h-5 text-indigo-500" />
              <span className="text-gray-500">采样地址：</span>
              <span className="text-gray-900">{orderData.location}</span>
            </div>
            <div className="flex items-center gap-3">
              <Phone className="w-5 h-5 text-indigo-500" />
              <span className="text-gray-500">联系电话：</span>
              <span className="text-gray-900">{orderData.phone}</span>
            </div>
            <div className="flex items-start gap-3">
              <FileText className="w-5 h-5 text-indigo-500 mt-0.5" />
              <span className="text-gray-500">备注：</span>
              <span className="text-gray-900 flex-1">{orderData.note}</span>
            </div>
          </div>
        </div>

        {/* Test Items */}
        <div className="bg-white rounded-2xl p-5">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-5 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">检测项目</h3>
          </div>
          <div className="space-y-3">
            {orderData.items.map((item, index) => (
              <div key={index} className="pb-3 border-b border-gray-100 last:border-0 last:pb-0">
                <div className="flex justify-between items-start mb-1">
                  <div className="text-gray-900">☑ {item.name}</div>
                  <div className="text-indigo-500">¥{item.price}</div>
                </div>
                <div className="text-xs text-gray-500">{item.description}</div>
              </div>
            ))}
            <div className="flex justify-between items-center pt-3 border-t border-gray-200">
              <span className="text-gray-700">检测费用</span>
              <span className="text-indigo-500">¥{orderData.totalPrice}</span>
            </div>
          </div>
        </div>

        {/* Payment Information */}
        <div className="bg-white rounded-2xl p-5">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-5 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">支付信息</h3>
          </div>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-500">支付方式：</span>
              <span className="text-emerald-600">{orderData.paymentMethod}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">支付状态：</span>
              <span className="text-emerald-600">{orderData.paymentStatus}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">支付时间：</span>
              <span className="text-gray-900">{orderData.paymentTime}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">交易单号：</span>
              <span className="text-gray-900 text-xs">{orderData.transactionId}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-4 py-4 max-w-md mx-auto">
        {orderData.hasReport && (
          <GradientButton variant="blue" size="md" onClick={handleViewReport} className="w-full">
            查看报告
          </GradientButton>
        )}
      </div>
    </div>
  );
}