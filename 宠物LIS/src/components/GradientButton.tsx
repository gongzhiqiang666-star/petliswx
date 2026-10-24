import React from 'react';

interface GradientButtonProps {
  children: React.ReactNode;
  onClick?: () => void;
  variant?: 'purple' | 'green' | 'blue' | 'outline';
  size?: 'sm' | 'md' | 'lg' | 'full';
  className?: string;
}

export function GradientButton({ 
  children, 
  onClick, 
  variant = 'purple', 
  size = 'md',
  className = ''
}: GradientButtonProps) {
  const variants = {
    purple: 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white hover:from-indigo-600 hover:to-purple-700',
    green: 'bg-gradient-to-r from-emerald-500 to-emerald-600 text-white hover:from-emerald-600 hover:to-emerald-700',
    blue: 'bg-gradient-to-r from-blue-500 to-blue-600 text-white hover:from-blue-600 hover:to-blue-700',
    outline: 'border-2 border-gray-300 text-gray-700 hover:bg-gray-50',
  };

  const sizes = {
    sm: 'px-4 py-2 h-10',
    md: 'px-6 py-2.5 h-12',
    lg: 'px-8 py-3 h-14',
    full: 'w-full px-8 py-3 h-14',
  };

  return (
    <button
      onClick={onClick}
      className={`rounded-3xl transition-all duration-200 ${variants[variant]} ${sizes[size]} ${className}`}
    >
      {children}
    </button>
  );
}