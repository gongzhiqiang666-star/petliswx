import { pageHeader, renderState } from '../components/ui.js';

export async function render(container) {
  container.innerHTML = `${pageHeader('送检管理', '完整订单管理将在物流与报告工作流合并后开放')}${renderState('pending')}`;
}
