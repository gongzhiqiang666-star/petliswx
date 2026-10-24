import React, { useState } from 'react';
import { ArrowLeft, Plus, Check } from 'lucide-react';
import { GradientButton } from '../components/GradientButton';

interface OrderStep1Props {
  onBack: () => void;
  onNext: () => void;
}

export function OrderStep1({ onBack, onNext }: OrderStep1Props) {
  const [selectedPet, setSelectedPet] = useState('pet1');
  const [selectedDate, setSelectedDate] = useState('2024-01-25');
  const [selectedTime, setSelectedTime] = useState('14:00');

  const pets = [
    {
      id: 'pet1',
      name: '小花',
      breed: '英国短毛猫',
      age: '2岁',
      avatar: '🐱',
      lastTest: '2023-06-15',
    },
    {
      id: 'pet2',
      name: '小黑',
      breed: '金毛寻回犬',
      age: '3岁',
      avatar: '🐕',
      lastTest: '2023-08-20',
    },
  ];

  const dates = [
    { date: '2024-01-23', day: '周二', available: true },
    { date: '2024-01-24', day: '周三', available: true },
    { date: '2024-01-25', day: '周四', available: true, isToday: true },
    { date: '2024-01-26', day: '周五', available: true },
    { date: '2024-01-27', day: '周六', available: false },
    { date: '2024-01-28', day: '周日', available: true },
  ];

  const timeSlots = [
    { time: '09:00', available: true },
    { time: '10:00', available: true },
    { time: '11:00', available: false },
    { time: '14:00', available: true },
    { time: '15:00', available: true },
    { time: '16:00', available: true },
  ];

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      {/* Header */}
      <div className="bg-white px-4 py-4 flex items-center justify-between border-b border-gray-200 sticky top-0 z-40">
        <button onClick={onBack} className="p-1">
          <ArrowLeft className="w-6 h-6 text-gray-700" />
        </button>
        <h1 className="text-gray-900">预约检测</h1>
        <div className="w-6"></div>
      </div>

      {/* Step Indicator */}
      <div className="bg-white px-4 py-4 flex items-center justify-center gap-2">
        <div className="w-8 h-8 rounded-full bg-indigo-500 flex items-center justify-center text-white text-sm">1</div>
        <div className="w-12 h-0.5 bg-gray-300"></div>
        <div className="w-8 h-8 rounded-full bg-gray-300 flex items-center justify-center text-white text-sm">2</div>
        <div className="w-12 h-0.5 bg-gray-300"></div>
        <div className="w-8 h-8 rounded-full bg-gray-300 flex items-center justify-center text-white text-sm">3</div>
      </div>

      <div className="px-4 pt-4 space-y-6">
        {/* Select Pet */}
        <div>
          <h3 className="text-gray-900 mb-3">选择宠物</h3>
          <div className="space-y-3">
            {pets.map((pet) => (
              <button
                key={pet.id}
                onClick={() => setSelectedPet(pet.id)}
                className={`w-full bg-white rounded-2xl p-4 flex items-center gap-3 transition-all ${
                  selectedPet === pet.id
                    ? 'border-2 border-indigo-500 shadow-md'
                    : 'border-2 border-gray-200'
                }`}
              >
                <div className="w-20 h-20 rounded-full bg-gradient-to-br from-indigo-100 to-purple-100 flex items-center justify-center flex-shrink-0">
                  <span className="text-3xl">{pet.avatar}</span>
                </div>
                <div className="flex-1 text-left">
                  <div className="text-gray-900 mb-1">{pet.name}</div>
                  <div className="text-sm text-gray-500">{pet.breed} · {pet.age}</div>
                  <div className="text-xs text-gray-400 mt-1">上次检测: {pet.lastTest}</div>
                </div>
                {selectedPet === pet.id && (
                  <div className="w-6 h-6 rounded-full bg-indigo-500 flex items-center justify-center flex-shrink-0">
                    <Check className="w-4 h-4 text-white" />
                  </div>
                )}
              </button>
            ))}

            {/* Add New Pet */}
            <button className="w-full h-24 border-2 border-dashed border-gray-300 rounded-2xl flex items-center justify-center gap-2 text-indigo-500 hover:border-indigo-300 hover:bg-indigo-50/50 transition-colors">
              <Plus className="w-5 h-5" />
              <span>添加新宠物</span>
            </button>
          </div>
        </div>

        {/* Select Date */}
        <div>
          <h3 className="text-gray-900 mb-3">选择预约日期</h3>
          <div className="bg-white rounded-2xl p-4">
            <div className="text-center mb-3 text-gray-900">2024年1月</div>
            <div className="grid grid-cols-3 gap-2">
              {dates.map((dateItem) => (
                <button
                  key={dateItem.date}
                  onClick={() => dateItem.available && setSelectedDate(dateItem.date)}
                  disabled={!dateItem.available}
                  className={`p-3 rounded-xl text-sm transition-all ${
                    selectedDate === dateItem.date
                      ? 'bg-indigo-500 text-white'
                      : dateItem.available
                      ? 'bg-gray-50 text-gray-900 hover:bg-gray-100'
                      : 'bg-gray-100 text-gray-400 cursor-not-allowed'
                  }`}
                >
                  <div className="mb-1">{dateItem.date.split('-')[2]}日</div>
                  <div className="text-xs opacity-80">{dateItem.day}</div>
                  {dateItem.isToday && (
                    <div className="text-xs mt-1 opacity-90">今天</div>
                  )}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Select Time */}
        <div>
          <h3 className="text-gray-900 mb-3">选择预约时间</h3>
          <div className="grid grid-cols-3 gap-2">
            {timeSlots.map((slot) => (
              <button
                key={slot.time}
                onClick={() => slot.available && setSelectedTime(slot.time)}
                disabled={!slot.available}
                className={`p-3 rounded-xl transition-all ${
                  selectedTime === slot.time
                    ? 'bg-indigo-500 text-white'
                    : slot.available
                    ? 'bg-white border border-gray-200 text-gray-700 hover:border-indigo-300'
                    : 'bg-gray-100 text-gray-400 cursor-not-allowed'
                }`}
              >
                {slot.time}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Bottom Action */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-4 py-4 max-w-md mx-auto">
        <GradientButton variant="purple" size="full" onClick={onNext}>
          下一步
        </GradientButton>
      </div>
    </div>
  );
}
