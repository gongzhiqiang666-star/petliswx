export class ApiError extends Error {
  constructor(message, { status = 0, code = null, details = null, cause } = {}) {
    super(message, { cause });
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

function friendlyMessage(status, message) {
  if (status === 401) return '登录已过期，请重新登录';
  if (status === 403) return '没有权限执行此操作';
  return message || `请求失败（HTTP ${status}）`;
}

function withQuery(url, query) {
  if (!query) return url;
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') params.set(key, value);
  });
  const suffix = params.toString();
  return suffix ? `${url}${url.includes('?') ? '&' : '?'}${suffix}` : url;
}

export function createApi({ baseUrl = '/api', fetchImpl = globalThis.fetch, getToken = () => null, onUnauthorized = () => {} } = {}) {
  if (!fetchImpl) throw new Error('当前环境不支持 fetch');

  async function parseError(response) {
    let body = null;
    try {
      body = typeof response.json === 'function' ? await response.json() : JSON.parse(await response.text());
    } catch {
      body = null;
    }
    if (response.status === 401) onUnauthorized();
    return new ApiError(friendlyMessage(response.status, body?.message), {
      status: response.status,
      code: body?.code ?? response.status,
      details: body?.details
    });
  }

  async function request(path, { method = 'GET', data, query, headers = {}, auth = true } = {}) {
    const token = auth ? getToken() : null;
    const isForm = typeof FormData !== 'undefined' && data instanceof FormData;
    const requestHeaders = { Accept: 'application/json', ...headers };
    if (token) requestHeaders.Authorization = `Bearer ${token}`;
    if (!isForm) requestHeaders['Content-Type'] = 'application/json';
    let response;
    try {
      response = await fetchImpl(withQuery(`${baseUrl}${path}`, query), {
        method,
        headers: requestHeaders,
        body: data === undefined ? undefined : isForm ? data : JSON.stringify(data)
      });
    } catch (cause) {
      throw new ApiError('网络连接失败，请检查服务是否可用', { status: 0, cause });
    }
    if (!response.ok) throw await parseError(response);
    if (response.status === 204) return null;
    let body;
    try {
      body = typeof response.json === 'function' ? await response.json() : JSON.parse(await response.text());
    } catch (cause) {
      throw new ApiError('服务返回了无法识别的响应', { status: response.status, cause });
    }
    if (!body || body.code !== 200) {
      throw new ApiError(body?.message || '业务处理失败', {
        status: response.status,
        code: body?.code,
        details: body?.details
      });
    }
    return body.data;
  }

  async function download(path) {
    const headers = {};
    const token = getToken();
    if (token) headers.Authorization = `Bearer ${token}`;
    let response;
    try {
      response = await fetchImpl(`${baseUrl}${path}`, { headers });
    } catch (cause) {
      throw new ApiError('网络连接失败，请检查服务是否可用', { status: 0, cause });
    }
    if (!response.ok) throw await parseError(response);
    const disposition = response.headers.get('Content-Disposition') || '';
    const utf8 = disposition.match(/filename\*=UTF-8''([^;]+)/i);
    const plain = disposition.match(/filename="?([^";]+)"?/i);
    const filename = utf8 ? decodeURIComponent(utf8[1]) : plain?.[1] || 'download';
    return { blob: await response.blob(), filename };
  }

  return { request, download };
}
