import { pageHeader, renderState } from '../components/ui.js';

export async function render(container) {
  container.innerHTML = `${pageHeader('物流配送', '快递分配、取件、异常与实验室签收将在物流工作流合并后开放')}${renderState('pending')}`;
}
