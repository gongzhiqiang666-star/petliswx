export const routes = [
  { id: 'dashboard', path: '/dashboard', label: '数据看板', group: '工作台', icon: '⌂', roles: ['ADMIN', 'LAB_STAFF'] },
  { id: 'reviews', path: '/reviews', label: '资质与送检审核', group: '业务管理', icon: '✓', roles: ['ADMIN', 'REVIEWER'] },
  { id: 'organizations', path: '/organizations', label: '用户与医院', group: '业务管理', icon: '◎', roles: ['ADMIN'] },
  { id: 'orders', path: '/orders', label: '送检管理', group: '业务管理', icon: '▤', roles: ['ADMIN', 'LAB_STAFF', 'HOSPITAL_ADMIN'] },
  { id: 'logistics', path: '/logistics', label: '物流配送', group: '业务管理', icon: '⇄', roles: ['ADMIN', 'LAB_STAFF', 'COURIER', 'HOSPITAL_ADMIN', 'DOCTOR'] },
  { id: 'laboratory', path: '/laboratory', label: '实验室任务', group: '检测报告', icon: '⌬', roles: ['ADMIN', 'LAB_STAFF'] },
  { id: 'reports', path: '/reports', label: '报告审核发布', group: '检测报告', icon: '▧', roles: ['ADMIN', 'LAB_STAFF', 'HOSPITAL_ADMIN', 'DOCTOR'] },
  { id: 'catalog', path: '/catalog', label: '项目与签约定价', group: '资源管理', icon: '◇', roles: ['ADMIN'] },
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

export function isRouteAllowed(routeId, user) {
  const route = routes.find((item) => item.id === routeId);
  if (!route || !user) return false;
  return route.roles.some((role) => user.roles?.includes(role));
}
