import React from 'react';
import { ArrowLeft, Download, FileText } from 'lucide-react';
import { GradientButton } from '../components/GradientButton';

interface ReportViewProps {
  reportId: string;
  onBack: () => void;
}

export function ReportView({ reportId, onBack }: ReportViewProps) {
  const report = {
    id: reportId,
    type: '血常规检查',
    petName: '小花',
    petBreed: '英国短毛猫',
    testDate: '2023-12-15',
    reportDate: '2023-12-16',
    status: 'normal',
    pdfUrl: '/reports/sample-report.pdf', // PDF文件URL
    fileName: '小花-血常规检查-20231215.pdf',
  };

  const handleDownload = () => {
    // 实际项目中，这里会触发PDF下载
    alert(`正在下载报告: ${report.fileName}`);
    // window.open(report.pdfUrl, '_blank');
  };

  const handleViewPDF = () => {
    // 实际项目中，这里会在新窗口打开PDF
    alert('在新窗口中打开PDF报告');
    // window.open(report.pdfUrl, '_blank');
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      {/* Header with Gradient Background */}
      <div className="bg-gradient-to-r from-indigo-500 to-purple-600 pt-12 pb-8 px-4 relative">
        <button
          onClick={onBack}
          className="absolute top-4 left-4 w-8 h-8 bg-white/20 rounded-full flex items-center justify-center"
        >
          <ArrowLeft className="w-5 h-5 text-white" />
        </button>

        <div className="text-center">
          <h1 className="text-white mb-2">{report.type}</h1>
          <div className="text-white/80 text-sm mb-3">
            {report.petName} · {report.petBreed}
          </div>
          <div className="text-white/80 text-sm">
            检测日期: {report.testDate}
          </div>
        </div>
      </div>

      {/* Report Info Card */}
      <div className="px-4 pt-4">
        <div className="bg-white rounded-2xl p-5 mb-4">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-5 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">报告信息</h3>
          </div>

          <div className="space-y-3 text-sm">
            <div className="flex items-center justify-between py-2 border-b border-gray-100">
              <span className="text-gray-500">报告编号</span>
              <span className="text-gray-900 font-mono text-xs">{report.id}</span>
            </div>
            <div className="flex items-center justify-between py-2 border-b border-gray-100">
              <span className="text-gray-500">宠物名称</span>
              <span className="text-gray-900">{report.petName}</span>
            </div>
            <div className="flex items-center justify-between py-2 border-b border-gray-100">
              <span className="text-gray-500">检测日期</span>
              <span className="text-gray-900">{report.testDate}</span>
            </div>
            <div className="flex items-center justify-between py-2 border-b border-gray-100">
              <span className="text-gray-500">报告日期</span>
              <span className="text-gray-900">{report.reportDate}</span>
            </div>
            <div className="flex items-center justify-between py-2">
              <span className="text-gray-500">检测结果</span>
              <span
                className={`text-sm px-3 py-1 rounded-full ${
                  report.status === 'normal'
                    ? 'bg-emerald-50 text-emerald-600'
                    : 'bg-orange-50 text-orange-600'
                }`}
              >
                {report.status === 'normal' ? '正常' : '需注意'}
              </span>
            </div>
          </div>
        </div>

        {/* PDF Preview Card */}
        <div className="bg-white rounded-2xl p-5 mb-4">
          <div className="flex items-center gap-2 mb-4">
            <div className="w-1 h-5 bg-indigo-500 rounded"></div>
            <h3 className="text-gray-900">PDF报告</h3>
          </div>

          <div className="border-2 border-dashed border-gray-200 rounded-2xl p-8 text-center">
            <div className="w-20 h-20 bg-gradient-to-br from-indigo-100 to-purple-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <FileText className="w-10 h-10 text-indigo-600" />
            </div>
            <div className="text-gray-900 mb-2">{report.fileName}</div>
            <div className="text-sm text-gray-500 mb-4">PDF格式 · 详细检测报告</div>

            <button
              onClick={handleViewPDF}
              className="text-indigo-500 hover:text-indigo-600 transition-colors"
            >
              点击查看完整报告 →
            </button>
          </div>
        </div>

        {/* Instructions Card */}
        <div className="bg-blue-50 border border-blue-200 rounded-2xl p-4">
          <div className="text-sm text-blue-900 mb-2">📋 使用说明</div>
          <ul className="text-xs text-blue-800 space-y-1 ml-4 list-disc">
            <li>点击下方"查看PDF"按钮在新窗口中打开报告</li>
            <li>点击"下载报告"按钮保存PDF到本地</li>
            <li>如有疑问，请联系客服或咨询医生</li>
          </ul>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-4 py-4 flex gap-3 max-w-md mx-auto">
        <GradientButton
          variant="outline"
          size="md"
          onClick={handleDownload}
          className="flex-1 flex items-center justify-center gap-2"
        >
          <Download className="w-5 h-5" />
          <span>下载报告</span>
        </GradientButton>
        <GradientButton
          variant="purple"
          size="md"
          onClick={handleViewPDF}
          className="flex-1 flex items-center justify-center gap-2"
        >
          <FileText className="w-5 h-5" />
          <span>查看PDF</span>
        </GradientButton>
      </div>
    </div>
  );
}