import { pageItems, value } from '../utils.js';
import { pagination, renderState } from './ui.js';

export async function loadList(container, { api, path, query = {}, render, onLoaded }) {
  container.innerHTML = renderState('loading');
  try {
    const data = await api.request(path, { query });
    container.innerHTML = `${render(data)}${data?.items ? pagination({ page: data.page || query.page || 1, size: data.size || query.size || 20, total: data.total || 0 }) : ''}`;
    onLoaded?.(data);
    return data;
  } catch (error) {
    container.innerHTML = renderState(error.status === 403 ? 'forbidden' : 'error', error);
    throw error;
  }
}

export function rows(data) {
  return pageItems(data);
}

export function map(row, key, fallback) {
  return value(row, key, fallback);
}
