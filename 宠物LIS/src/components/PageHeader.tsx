import React from 'react';
import { ArrowLeft, Share2 } from 'lucide-react';

interface PageHeaderProps {
  title: string;
  onBack?: () => void;
  onShare?: () => void;
}

export function PageHeader({ title, onBack, onShare }: PageHeaderProps) {
  return (
    <div className="bg-white border-b border-gray-200 px-4 py-4 flex items-center justify-between sticky top-0 z-40">
      <div className="flex items-center gap-3">
        {onBack && (
          <button onClick={onBack} className="p-1">
            <ArrowLeft className="w-6 h-6 text-gray-700" />
          </button>
        )}
        <h1 className="text-gray-900">{title}</h1>
      </div>
      {onShare && (
        <button onClick={onShare} className="p-1">
          <Share2 className="w-5 h-5 text-gray-700" />
        </button>
      )}
    </div>
  );
}
