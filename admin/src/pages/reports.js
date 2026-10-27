import { pageHeader, renderState } from '../components/ui.js';

export async function render(container) {
  container.innerHTML = `${pageHeader('报告审核发布', '报告生成、审核、发布与鉴权下载将在报告工作流合并后开放')}${renderState('pending')}`;
}
