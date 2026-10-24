import React, { useState } from 'react';
import { BottomNav } from './components/BottomNav';
import { HomePage } from './pages/HomePage';
import { OrderList } from './pages/OrderList';
import { OrderDetail } from './pages/OrderDetail';
import { OrderSelectPet } from './pages/OrderSelectPet';
import { OrderAddPet } from './pages/OrderAddPet';
import { OrderStep1New } from './pages/OrderStep1New';
import { OrderStep2New } from './pages/OrderStep2New';
import { ReportList } from './pages/ReportList';
import { ReportView } from './pages/ReportView';
import { PetList } from './pages/PetList';
import { PetForm } from './pages/PetForm';
import { PetDetail } from './pages/PetDetail';
import { PetHealth } from './pages/PetHealth';
import { MinePage } from './pages/MinePage';
import { IconDownloadPage } from './pages/IconDownloadPage';
import { IconExportPage } from './pages/IconExportPage';

type Page = 
  | 'home' 
  | 'pets' 
  | 'orders' 
  | 'reports'
  | 'mine'
  | 'order-detail'
  | 'order-select-pet'
  | 'order-add-pet'
  | 'order-step1' 
  | 'order-step2'
  | 'report-view'
  | 'pet-add'
  | 'pet-add-from-home'
  | 'pet-edit'
  | 'pet-detail'
  | 'pet-health'
  | 'icon-download'
  | 'icon-export';

export default function App() {
  const [currentPage, setCurrentPage] = useState<Page>('home');
  const [selectedOrderId, setSelectedOrderId] = useState<string>('');
  const [selectedReportId, setSelectedReportId] = useState<string>('');
  const [selectedPetId, setSelectedPetId] = useState<string>('');
  const [selectedPetData, setSelectedPetData] = useState<any>(null);
  const [orderStep1Data, setOrderStep1Data] = useState<any>(null);

  const navigate = (page: Page) => {
    setCurrentPage(page);
  };

  const handleViewOrderDetail = (orderId: string) => {
    setSelectedOrderId(orderId);
    navigate('order-detail');
  };

  const handleViewReport = (reportId: string) => {
    setSelectedReportId(reportId);
    navigate('report-view');
  };

  const handleNewOrder = () => {
    setOrderStep1Data(null);
    setSelectedPetData(null);
    navigate('order-select-pet');
  };

  const handleViewPetDetail = (petId: string) => {
    setSelectedPetId(petId);
    navigate('pet-detail');
  };

  const handleEditPet = (petId: string) => {
    setSelectedPetId(petId);
    navigate('pet-edit');
  };

  const handleBottomNavClick = (tab: 'home' | 'pets' | 'orders' | 'mine') => {
    navigate(tab);
  };

  const handleLogout = () => {
    // Logout logic - clear data and return to home
    setOrderStep1Data(null);
    setSelectedPetData(null);
    setSelectedOrderId('');
    setSelectedReportId('');
    setSelectedPetId('');
    alert('已成功退出登录');
    navigate('home');
  };

  const renderPage = () => {
    switch (currentPage) {
      case 'home':
        return <HomePage onNavigate={navigate} />;
      
      case 'pets':
        return (
          <PetList
            onViewDetail={handleViewPetDetail}
            onAddPet={() => navigate('pet-add')}
            onEditPet={handleEditPet}
          />
        );
      
      case 'pet-add':
        return (
          <PetForm
            onBack={() => navigate('pets')}
            onSave={() => {
              alert('宠物信息已保存');
              navigate('pets');
            }}
          />
        );
      
      case 'pet-add-from-home':
        return (
          <PetForm
            onBack={() => navigate('home')}
            onSave={() => {
              alert('宠物信息已保存');
              navigate('pets');
            }}
          />
        );
      
      case 'pet-edit':
        return (
          <PetForm
            petId={selectedPetId}
            onBack={() => navigate('pet-detail')}
            onSave={() => {
              alert('宠物信息已更新');
              navigate('pet-detail');
            }}
          />
        );
      
      case 'pet-detail':
        return (
          <PetDetail
            petId={selectedPetId}
            onBack={() => navigate('pets')}
            onEdit={() => handleEditPet(selectedPetId)}
            onBookTest={() => navigate('order-step1')}
            onViewReport={(reportId) => {
              setSelectedReportId(reportId);
              navigate('report-view');
            }}
          />
        );
      
      case 'pet-health':
        return (
          <PetHealth
            petId={selectedPetId}
            onBack={() => navigate('pet-detail')}
          />
        );
      
      case 'orders':
        return (
          <OrderList
            onViewDetail={handleViewOrderDetail}
            onNewOrder={handleNewOrder}
          />
        );
      
      case 'order-detail':
        return (
          <OrderDetail
            orderId={selectedOrderId}
            onBack={() => navigate('orders')}
            onRebook={() => navigate('order-step1')}
            onViewReport={(reportId) => {
              setSelectedReportId(reportId);
              navigate('report-view');
            }}
          />
        );
      
      case 'order-select-pet':
        return (
          <OrderSelectPet
            onBack={() => navigate('orders')}
            onSelectPet={(petData) => {
              setSelectedPetData(petData);
              navigate('order-step1');
            }}
            onAddNewPet={() => navigate('order-add-pet')}
          />
        );
      
      case 'order-add-pet':
        return (
          <OrderAddPet
            onBack={() => navigate('order-select-pet')}
            onSave={(petData) => {
              setSelectedPetData(petData);
              alert('宠物信息已保存');
              navigate('order-step1');
            }}
          />
        );
      
      case 'order-step1':
        return (
          <OrderStep1New
            onBack={() => navigate('order-select-pet')}
            onNext={(formData) => {
              setOrderStep1Data(formData);
              navigate('order-step2');
            }}
            initialData={selectedPetData}
          />
        );
      
      case 'order-step2':
        return (
          <OrderStep2New
            onBack={() => navigate('order-step1')}
            onNext={(selectedTests) => {
              // 创建订单逻辑
              console.log('Step 1 Data:', orderStep1Data);
              console.log('Selected Tests:', selectedTests);
              alert('预约创建成功！订单状态：草稿(-2)');
              setOrderStep1Data(null);
              navigate('orders');
            }}
            step1Data={orderStep1Data || {}}
          />
        );
      
      case 'reports':
        return (
          <ReportList
            onViewReport={handleViewReport}
            onNewAppointment={handleNewOrder}
          />
        );
      
      case 'report-view':
        return (
          <ReportView
            reportId={selectedReportId}
            onBack={() => navigate('reports')}
          />
        );
      
      case 'mine':
        return <MinePage onNavigate={navigate} onLogout={handleLogout} />;
      
      case 'icon-download':
        return <IconDownloadPage onNavigate={navigate} />;
      
      case 'icon-export':
        return <IconExportPage onNavigate={navigate} />;
      
      default:
        return <HomePage onNavigate={navigate} />;
    }
  };

  // Pages that should hide the bottom navigation
  const hideBottomNav = [
    'order-detail',
    'order-select-pet',
    'order-add-pet',
    'order-step1',
    'order-step2',
    'report-view',
    'pet-add',
    'pet-add-from-home',
    'pet-edit',
    'pet-detail',
    'pet-health',
    'icon-download',
    'icon-export',
  ].includes(currentPage);

  const getActiveTab = (): 'home' | 'pets' | 'orders' | 'mine' => {
    if (currentPage === 'home') return 'home';
    if (currentPage.startsWith('order')) return 'orders';
    if (currentPage.startsWith('report')) return 'mine'; // 报告现在在我的页面访问
    if (currentPage.startsWith('pet')) return 'pets';
    if (currentPage === 'mine') return 'mine';
    return 'home';
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Main Content */}
      <div className="max-w-md mx-auto bg-white min-h-screen relative">
        {renderPage()}
      </div>

      {/* Bottom Navigation */}
      {!hideBottomNav && (
        <BottomNav
          active={getActiveTab()}
          onNavigate={handleBottomNavClick}
        />
      )}
    </div>
  );
}