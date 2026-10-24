import React from 'react';

type BadgeType = 'pending' | 'in-progress' | 'completed' | 'cancelled' | 'normal' | 'warning' | 'abnormal' | 'waiting';

interface StatusBadgeProps {
  type: BadgeType;
  children: React.ReactNode;
}

export function StatusBadge({ type, children }: StatusBadgeProps) {
  const styles = {
    'pending': 'bg-amber-50 text-amber-900 border-amber-200',
    'in-progress': 'bg-indigo-50 text-indigo-900 border-indigo-200',
    'completed': 'bg-emerald-50 text-emerald-900 border-emerald-200',
    'cancelled': 'bg-gray-50 text-gray-900 border-gray-200',
    'normal': 'bg-emerald-500 text-white',
    'warning': 'bg-orange-500 text-white',
    'abnormal': 'bg-red-500 text-white',
    'waiting': 'bg-gray-400 text-white',
  };

  return (
    <span className={`inline-flex items-center px-3 py-1 rounded-lg border ${styles[type]}`}>
      {children}
    </span>
  );
}
