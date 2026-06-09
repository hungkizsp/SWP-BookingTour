(() => {
  const LOGIN_PATH = '/pages/auth/login.html';
  let authWatchStarted = false;
  let sessionEventSource = null;
  let sessionEventToken = '';

  function getToken() {
    return sessionStorage.getItem('token') || '';
  }

  function clearAuthState() {
    stopSessionStream();
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
  }

  function setAuthNotice(message) {
    if (!message) return;
    sessionStorage.setItem('authNotice', message);
  }

  function getAuthNotice() {
    return sessionStorage.getItem('authNotice') || '';
  }

  function clearAuthNotice() {
    sessionStorage.removeItem('authNotice');
  }

  function goToLogin(message) {
    if (message) setAuthNotice(message);
    clearAuthState();
    if (window.location.pathname !== LOGIN_PATH) {
      window.location.href = LOGIN_PATH;
    }
  }

  function stopSessionStream() {
    if (sessionEventSource) {
      sessionEventSource.close();
      sessionEventSource = null;
      sessionEventToken = '';
    }
  }

  function connectSessionStream() {
    const token = getToken();
    if (!token || window.location.pathname === LOGIN_PATH) {
      stopSessionStream();
      return;
    }

    if (sessionEventSource && sessionEventToken === token) {
      return;
    }

    stopSessionStream();
    sessionEventToken = token;
    const BACKEND_URL = 'http://localhost:8080';
    const url = `${BACKEND_URL}/api/v1/auth/events?token=${encodeURIComponent(token)}`;
    sessionEventSource = new EventSource(url);

    sessionEventSource.onmessage = (event) => {
      if (!event?.data) return;
      goToLogin(event.data || 'Tài khoản đã được đăng nhập ở nơi khác. Vui lòng đăng nhập lại.');
    };

    sessionEventSource.onerror = () => {
      if (!getToken()) {
        stopSessionStream();
      }
    };
  }

  async function apiFetch(path, options = {}) {
    const BACKEND_URL = 'http://localhost:8080';
    const isDev = window.location.port === '3000' || window.location.port === '5500';
    const fullPath = (isDev && path.startsWith('/')) ? BACKEND_URL + path : path;

    const headers = new Headers(options.headers || {});
    if (!headers.has('Content-Type') && options.body) {
      headers.set('Content-Type', 'application/json');
    }
    const token = getToken();
    if (token) headers.set('Authorization', `Bearer ${token}`);

    const res = await fetch(fullPath, { ...options, headers });
    const text = await res.text();
    let json = null;
    try { json = text ? JSON.parse(text) : null; } catch (_) {}
    if (!res.ok) {
      const msg = (json && json.message) ? json.message : `HTTP ${res.status}`;
      if ((res.status === 401 || res.status === 403) && token) {
        goToLogin('Phiên đăng nhập hết hạn, vui lòng đăng nhập lại!');
      }
      const err = new Error(msg);
      err.status = res.status;
      err.body = json;
      throw err;
    }
    return json;
  }

  window.TB = window.TB || {};
  window.TB.apiFetch = apiFetch;
  window.TB.clearAuthState = clearAuthState;
  window.TB.getAuthNotice = getAuthNotice;
  window.TB.clearAuthNotice = clearAuthNotice;
  window.TB.goToLogin = goToLogin;
  window.TB.connectSessionStream = connectSessionStream;
  window.TB.stopSessionStream = stopSessionStream;
  window.TB.getToken = getToken;
  window.TB.logout = async () => {
    try {
      await apiFetch('/api/v1/auth/logout', { method: 'POST' });
    } catch (_) {
      // ignore errors, still clear locally
    }
    goToLogin('Logout successful.');
  };

  async function validateCurrentSession() {
    const token = getToken();
    if (!token) return;
    try {
      await apiFetch('/api/v1/auth/me', { method: 'GET' });
    } catch (err) {
      if (err.status === 401) {
        goToLogin(err.message || 'Tài khoản đã được đăng nhập ở nơi khác.');
      }
    }
  }

  function startAuthWatch() {
    if (authWatchStarted) return;
    authWatchStarted = true;
    if (window.location.pathname === LOGIN_PATH) return;
    connectSessionStream();
    validateCurrentSession();
    setInterval(() => {
      if (getToken()) {
        connectSessionStream();
        validateCurrentSession();
      }
    }, 15000);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', startAuthWatch, { once: true });
  } else {
    startAuthWatch();
  }
})();
