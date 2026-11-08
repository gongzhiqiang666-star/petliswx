export const routes = [
  { id: 'dashboard', path: '/dashboard', label: '数据看板', group: '工作台', icon: '⌂', roles: ['ADMIN', 'LAB_STAFF'] },
  { id: 'reviews', path: '/reviews', label: '资质与送检审核', group: '业务管理', icon: '✓', roles: ['ADMIN', 'REVIEWER'] },
  { id: 'hospitalDocuments', path: '/hospital-documents', label: '医院资料', group: '业务管理', icon: '▤', roles: ['ADMIN', 'HOSPITAL_ADMIN'] },
  { id: 'organizations', path: '/organizations', label: '用户与医院', group: '业务管理', icon: '◎', roles: ['ADMIN'] },
  { id: 'orders', path: '/orders', label: '送检管理', group: '业务管理', icon: '▤', roles: ['ADMIN', 'LAB_STAFF', 'HOSPITAL_ADMIN'] },
  { id: 'logistics', path: '/logistics', label: '物流配送', group: '业务管理', icon: '⇄', roles: ['ADMIN', 'LAB_STAFF', 'COURIER', 'HOSPITAL_ADMIN', 'DOCTOR'] },
  { id: 'laboratory', path: '/laboratory', label: '实验室任务', group: '检测报告', icon: '⌬', roles: ['ADMIN', 'LAB_STAFF'] },
  { id: 'reports', path: '/reports', label: '报告审核发布', group: '检测报告', icon: '▧', roles: ['ADMIN', 'LAB_STAFF', 'HOSPITAL_ADMIN', 'DOCTOR'] },
  { id: 'legacyDictionaries', path: '/legacy-dictionaries', label: '\u57fa\u7840\u6570\u636e\u7ba1\u7406', group: '\u8d44\u6e90\u7ba1\u7406', icon: '\u25c7', roles: ['ADMIN'], capability: 'dictionaries' },
  { id: 'legacyTestCatalog', path: '/legacy-test-catalog', label: '检测项目资料', group: '资源管理', icon: '◇', roles: ['ADMIN'], capability: 'testCatalog' },
  { id: 'legacyMailSettings', path: '/legacy-mail-settings', label: '邮件服务器', group: '资源管理', icon: '◇', roles: ['ADMIN'], capability: 'mailSettings' },
  { id: 'legacyTemplates', path: '/legacy-templates', label: '业务模板', group: '资源管理', icon: '◇', roles: ['ADMIN'], capability: 'templates' },
  { id: 'legacyTemplateBindings', path: '/legacy-template-bindings', label: '模板变量绑定', group: '资源管理', icon: '◇', roles: ['ADMIN'], capability: 'templateBindings' },
  { id: 'catalog', path: '/catalog', label: '项目与签约定价', group: '资源管理', icon: '◇', roles: ['ADMIN'] },
  { id: 'legacyFinance', path: '/legacy-finance', label: '原业务财务明细', group: '财务运营', icon: '¥', roles: ['ADMIN'], capability: 'legacyFinance' },
  { id: 'legacyCustomerViews', path: '/legacy-customer-views', label: '客户账单与报告核对', group: '财务运营', icon: '◇', roles: ['ADMIN'], capability: 'legacyCustomerViews' },
  { id: 'legacyBankPayments', path: '/legacy-bank-payments', label: '原网银付款核价', group: '财务运营', icon: '¥', roles: ['ADMIN'], capability: 'bankPayments' },
  { id: 'legacyPaymentLogs', path: '/legacy-payment-logs', label: '网银交易流水', group: '财务运营', icon: '¥', roles: ['ADMIN'], capability: 'paymentLogs' },
  { id: 'legacyMonthlyReports', path: '/legacy-monthly-reports', label: '原医院月报', group: '财务运营', icon: '¥', roles: ['ADMIN'], capability: 'legacyMonthlyReports' },
  { id: 'finance', path: '/finance', label: '财务处理', group: '财务运营', icon: '¥', roles: ['ADMIN', 'HOSPITAL_ADMIN', 'LAB_STAFF'] },
  { id: 'operations', path: '/operations', label: '运营管理', group: '财务运营', icon: '✦', roles: ['ADMIN'] },
  { id: 'messages', path: '/messages', label: '消息中心', group: '财务运营', icon: '✉', roles: ['ADMIN', 'LAB_STAFF', 'HOSPITAL_ADMIN', 'DOCTOR', 'COURIER'] },
  { id: 'system', path: '/system', label: '审计与系统配置', group: '系统', icon: '⚙', roles: ['ADMIN'] }
];

export function resolveRoute(hash = '') {
  const path = hash.replace(/^#/, '') || '/dashboard';
  const [pathname] = path.split('?');
  const segments = pathname.split('/').filter(Boolean);
  const route = routes.find((item) => item.path === `/${segments[0]}`) || routes[0];
  return { ...route, params: segments[1] ? { id: decodeURIComponent(segments[1]) } : {} };
}

export function isRouteAllowed(routeId, user, capabilities = {}) {
  const route = routes.find((item) => item.id === routeId);
  if (!route || !user) return false;
  return route.roles.some((role) => user.roles?.includes(role)) && (!route.capability || capabilities[route.capability] === true);
}
