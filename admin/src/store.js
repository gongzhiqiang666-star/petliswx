export const STORAGE_KEYS = {
  token: 'petlis.admin.token',
  user: 'petlis.admin.user'
};

export function createStore({ storage = globalThis.localStorage, api } = {}) {
  let state = { token: storage?.getItem(STORAGE_KEYS.token) || null, user: null };
  try {
    state.user = JSON.parse(storage?.getItem(STORAGE_KEYS.user) || 'null');
  } catch {
    state.user = null;
  }
  const listeners = new Set();
  const notify = () => listeners.forEach((listener) => listener(state));

  function setSession(token, user) {
    state = { token, user };
    if (token) {
      storage?.setItem(STORAGE_KEYS.token, token);
      storage?.setItem(STORAGE_KEYS.user, JSON.stringify(user));
    } else {
      storage?.removeItem(STORAGE_KEYS.token);
      storage?.removeItem(STORAGE_KEYS.user);
    }
    notify();
  }

  return {
    getState: () => state,
    hasRole(role) {
      return Boolean(state.user?.roles?.includes(role));
    },
    async login(username, password) {
      const result = await api.request('/auth/password/login', {
        method: 'POST',
        data: { account: username, password },
        auth: false
      });
      if (!result.user || !Array.isArray(result.user.roles)) throw new Error('登录响应缺少用户角色');
      setSession(result.token, result.user);
      return result.user;
    },
    logout() {
      setSession(null, null);
    },
    subscribe(listener) {
      listeners.add(listener);
      return () => listeners.delete(listener);
    }
  };
}
