import { createApi } from './api.js';
import { createStore } from './store.js';
import { routes, resolveRoute, isRouteAllowed } from './router.js';
import { escapeHtml } from './utils.js';
import { modal, renderState } from './components/ui.js';
import { bindRetry } from './components/retry.js';
import * as dashboard from './pages/dashboard.js';
import * as reviews from './pages/reviews.js';
import * as organizations from './pages/organizations.js';
import * as orders from './pages/orders.js';
import * as logistics from './pages/logistics.js';
import * as laboratory from './pages/laboratory.js';
import * as reports from './pages/reports.js';
import * as catalog from './pages/catalog.js';
import * as finance from './pages/finance.js';
import * as operations from './pages/operations.js';
import * as messages from './pages/messages.js';
import * as system from './pages/system.js';

const pages = { dashboard, reviews, organizations, orders, logistics, laboratory, reports, catalog, finance, operations, messages, system };
const root = document.querySelector('#app');
let api;
const store = createStore({
  api: { request: (...args) => api.request(...args) },
  storage: globalThis.localStorage
});
api = createApi({
  baseUrl: globalThis.PETLIS_API_BASE || '/api',
  getToken: () => store.getState().token,
  onUnauthorized: () => { store.logout(); renderLogin('登录已过期，请重新登录'); }
});

let currentRoute = null;

function renderLogin(message = '') {
  root.innerHTML = `<main class="login-page"><section class="login-brand"><div class="brand-mark">P</div><div><span class="eyebrow">PETLIS ADMIN</span><h1>让每一次送检<br>清晰、可靠、可追溯</h1><p>连接宠物医院、配送与实验室的一体化管理工作台。</p></div><div class="login-features"><span>送检全流程</span><span>实验室协同</span><span>财务与运营</span></div></section><section class="login-panel"><form id="login-form" class="login-card"><div class="mobile-brand"><span class="brand-mark">P</span> Petlis 管理后台</div><span class="eyebrow">欢迎回来</span><h2>登录管理后台</h2><p>使用平台分配的工作账号登录</p><label class="form-field"><span>用户名</span><input name="username" autocomplete="username" required autofocus placeholder="请输入用户名"></label><label class="form-field"><span>密码</span><input name="password" type="password" autocomplete="current-password" required placeholder="请输入密码"></label><div id="login-error" class="form-error" role="alert">${escapeHtml(message)}</div><button class="button login-button" type="submit">登录</button><small class="login-note">登录即代表您同意遵守平台数据安全与隐私规范</small></form></section></main>`;
  root.querySelector('#login-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const button = form.querySelector('button');
    const error = form.querySelector('#login-error');
    button.disabled = true; button.textContent = '正在登录…'; error.textContent = '';
    try {
      await store.login(form.elements.username.value.trim(), form.elements.password.value);
      renderShell();
    } catch (reason) {
      error.textContent = reason.message || '登录失败';
      button.disabled = false; button.textContent = '登录';
    }
  });
}

function renderShell() {
  const { user } = store.getState();
  if (!user) { store.logout(); renderLogin(); return; }
  const allowed = routes.filter((route) => isRouteAllowed(route.id, user));
  const groups = [...new Set(allowed.map((route) => route.group))];
  root.innerHTML = `<div class="app-shell"><aside class="sidebar" id="sidebar"><a class="sidebar-brand" href="#/dashboard"><span class="brand-mark">P</span><span><strong>Petlis</strong><small>实验室管理后台</small></span></a><nav class="sidebar-nav">${groups.map((group) => `<div class="nav-group"><span class="nav-group-label">${group}</span>${allowed.filter((route) => route.group === group).map((route) => `<a href="#${route.path}" data-route="${route.id}"><span class="nav-icon">${route.icon}</span>${route.label}</a>`).join('')}</div>`).join('')}</nav><div class="sidebar-foot"><span class="status-dot"></span> 管理服务运行中</div></aside><button class="sidebar-overlay" id="sidebar-overlay" aria-label="关闭导航"></button><div class="main-shell"><header class="topbar"><button class="icon-button menu-button" id="menu-button" aria-label="打开导航">☰</button><div class="breadcrumb"><span>Petlis</span><b>/</b><strong id="current-page">工作台</strong></div><div class="topbar-user"><span class="avatar">${escapeHtml((user.displayName || user.username || 'U').slice(0, 1))}</span><span class="user-copy"><strong>${escapeHtml(user.displayName || user.username)}</strong><small>${escapeHtml((user.roles || []).join(' · '))}</small></span><button class="button text" id="logout">退出</button></div></header><main id="page-content" class="page-content" tabindex="-1"></main></div></div><div id="modal-root"></div><div id="toast-root" class="toast-root" aria-live="polite"></div>`;
  root.querySelector('#logout').addEventListener('click', () => { store.logout(); location.hash = ''; renderLogin(); });
  const closeMenu = () => root.querySelector('.app-shell')?.classList.remove('menu-open');
  root.querySelector('#menu-button').addEventListener('click', () => root.querySelector('.app-shell').classList.toggle('menu-open'));
  root.querySelector('#sidebar-overlay').addEventListener('click', closeMenu);
  root.querySelectorAll('.sidebar-nav a').forEach((link) => link.addEventListener('click', closeMenu));
  bindRetry(root.querySelector('#page-content'), navigate);
  window.removeEventListener('hashchange', navigate);
  window.addEventListener('hashchange', navigate);
  navigate();
}

async function navigate() {
  if (!store.getState().token) return;
  const content = root.querySelector('#page-content');
  if (!content) return;
  let route = resolveRoute(location.hash);
  const user = store.getState().user;
  if (!isRouteAllowed(route.id, user)) {
    const fallback = routes.find((item) => isRouteAllowed(item.id, user));
    if (!fallback) { content.innerHTML = renderState('forbidden'); return; }
    route = fallback;
    history.replaceState(null, '', `#${fallback.path}`);
  }
  currentRoute = route;
  root.querySelectorAll('[data-route]').forEach((link) => {
    link.classList.toggle('active', link.dataset.route === route.id);
    if (link.dataset.route === route.id) link.setAttribute('aria-current', 'page'); else link.removeAttribute('aria-current');
  });
  root.querySelector('#current-page').textContent = route.label;
  content.innerHTML = renderState('loading');
  try {
    await pages[route.id].render(content, { api, store, route, dialog: openDialog, toast: showToast });
  } catch (error) {
    content.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error);
    content.querySelector('[data-action="retry"]')?.addEventListener('click', navigate);
  }
}

function showToast(message, type = 'success') {
  const target = root.querySelector('#toast-root');
  if (!target) return;
  const item = document.createElement('div');
  item.className = `toast toast-${type}`;
  item.setAttribute('role', type === 'error' ? 'alert' : 'status');
  item.textContent = message;
  target.append(item);
  setTimeout(() => item.remove(), type === 'error' ? 6000 : 3500);
}

function openDialog({ title, body, submitLabel, onSubmit, wide = false }) {
  const target = root.querySelector('#modal-root');
  const footer = submitLabel ? `<button type="button" class="button secondary" data-action="close-modal">取消</button><button type="submit" form="active-dialog-form" class="button">${escapeHtml(submitLabel)}</button>` : '<button type="button" class="button secondary" data-action="close-modal">关闭</button>';
  target.innerHTML = modal({ title, body: onSubmit ? `<form id="active-dialog-form">${body}<div class="form-error" id="dialog-error" role="alert"></div></form>` : body, footer, wide });
  const close = () => { target.innerHTML = ''; };
  target.querySelectorAll('[data-action="close-modal"]').forEach((button) => button.addEventListener('click', (event) => {
    if (button.classList.contains('modal-backdrop') && event.target !== button) return;
    close();
  }));
  const onKey = (event) => { if (event.key === 'Escape') { close(); document.removeEventListener('keydown', onKey); } };
  document.addEventListener('keydown', onKey);
  const form = target.querySelector('#active-dialog-form');
  if (form) form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const submit = target.querySelector('[type="submit"]');
    const error = target.querySelector('#dialog-error');
    submit.disabled = true; submit.textContent = '正在提交…'; error.textContent = '';
    try { await onSubmit(form); close(); document.removeEventListener('keydown', onKey); }
    catch (reason) { error.textContent = reason.message || '操作失败'; submit.disabled = false; submit.textContent = submitLabel; }
  });
  target.querySelector('input:not([type="hidden"]), select, textarea, button')?.focus();
}

if (store.getState().token) renderShell(); else renderLogin();

export { navigate, currentRoute };
