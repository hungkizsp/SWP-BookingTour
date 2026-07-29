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
    if (!message && getToken()) return;
    if (message) setAuthNotice(message);
    clearAuthState();
    if (window.location.pathname !== LOGIN_PATH) {
      window.location.href = LOGIN_PATH;
    }
  }

  // ── Account Blocked Overlay ────────────────────────────────────────────────
  let blockOverlayShown = false;

  function showBlockedOverlay(message) {
    if (blockOverlayShown) return;
    blockOverlayShown = true;

    const overlay = document.createElement('div');
    overlay.id = 'tb-blocked-overlay';
    overlay.style.cssText = [
      'position:fixed', 'inset:0', 'z-index:99999',
      'display:flex', 'align-items:center', 'justify-content:center',
      'background:rgba(0,0,0,0.65)', 'backdrop-filter:blur(6px)',
      'animation:tbFadeIn .3s ease'
    ].join(';');

    const card = document.createElement('div');
    card.style.cssText = [
      'background:#fff', 'border-radius:1.25rem',
      'padding:2.5rem 2rem', 'max-width:420px', 'width:90%',
      'text-align:center', 'box-shadow:0 24px 60px rgba(0,0,0,0.25)',
      'animation:tbSlideUp .35s ease'
    ].join(';');

    let countdown = 5;
    card.innerHTML = `
      <div style="font-size:3.5rem;margin-bottom:1rem">🔒</div>
      <h2 style="font-size:1.2rem;font-weight:800;color:#dc2626;margin:0 0 .75rem">
        Tài khoản tạm thời bị khóa
      </h2>
      <p style="font-size:.9rem;color:#4b5563;margin:0 0 1.5rem;line-height:1.6">
        ${message || 'Tài khoản của bạn đã bị khóa tạm thời do gửi yêu cầu quá nhanh.'}
      </p>
      <p style="font-size:.82rem;color:#9ca3af;margin:0 0 1.5rem">
        Đang chuyển về trang đăng nhập sau <strong id="tb-countdown">${countdown}</strong> giây...
      </p>
      <button id="tb-blocked-logout" style="
        background:linear-gradient(135deg,#ef4444,#dc2626);
        color:#fff;border:none;border-radius:.65rem;
        padding:.6rem 1.5rem;font-size:.875rem;font-weight:700;
        cursor:pointer;width:100%
      ">Thoát ngay</button>
    `;

    const style = document.createElement('style');
    style.textContent = `
      @keyframes tbFadeIn { from { opacity:0 } to { opacity:1 } }
      @keyframes tbSlideUp { from { transform:translateY(30px);opacity:0 } to { transform:translateY(0);opacity:1 } }
    `;
    document.head.appendChild(style);
    overlay.appendChild(card);
    document.body.appendChild(overlay);

    const countdownEl = document.getElementById('tb-countdown');
    const doLogout = () => {
      clearAuthState();
      window.location.href = LOGIN_PATH;
    };

    const timer = setInterval(() => {
      countdown--;
      if (countdownEl) countdownEl.textContent = countdown;
      if (countdown <= 0) {
        clearInterval(timer);
        doLogout();
      }
    }, 1000);

    document.getElementById('tb-blocked-logout').addEventListener('click', () => {
      clearInterval(timer);
      doLogout();
    });
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

    if (sessionEventSource && 
        sessionEventSource.readyState !== 2 && // 2 is CLOSED
        sessionEventToken === token) {
      return;
    }

    stopSessionStream();
    sessionEventToken = token;
    const BACKEND_URL = 'http://localhost:8080';
    const url = `${BACKEND_URL}/api/v1/auth/events?token=${encodeURIComponent(token)}`;
    sessionEventSource = new EventSource(url);

    sessionEventSource.addEventListener('session_invalidated', (event) => {
      console.log('Session invalidated:', event.data);
      goToLogin(event.data);
    });

    sessionEventSource.addEventListener('ping', () => {
      // heart beat received
    });

    sessionEventSource.onerror = () => {
      if (sessionEventSource) {
        sessionEventSource.close();
        sessionEventSource = null;
      }
      // Reconnect after 3s only if token exists
      if (getToken()) {
        setTimeout(connectSessionStream, 3000);
      }
    };
  }

  async function apiFetch(path, options = {}) {
    const BACKEND_URL = 'http://localhost:8080';
    const isDev = window.location.port === '3000' || window.location.port === '5500';
    const fullPath = (isDev && path.startsWith('/')) ? BACKEND_URL + path : path;

    const headers = new Headers(options.headers || {});
    if (!headers.has('Content-Type') && options.body && !(options.body instanceof FormData)) {
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
      if (res.status === 403 && json && json.code === 1030) {
        // Account blocked — show overlay instead of redirect
        showBlockedOverlay(msg);
      } else if ((res.status === 401 || res.status === 403) && token) {
        goToLogin('Phiên đăng nhập hết hạn, vui lòng đăng nhập lại!');
      }
      const err = new Error(msg);
      err.status = res.status;
      err.body = json;
      throw err;
    }
    return json;
  }

  async function apiFetchBlob(path, options = {}) {
    const BACKEND_URL = 'http://localhost:8080';
    const isDev = window.location.port === '3000' || window.location.port === '5500';
    const fullPath = (isDev && path.startsWith('/')) ? BACKEND_URL + path : path;

    const headers = new Headers(options.headers || {});
    const token = getToken();
    if (token) headers.set('Authorization', `Bearer ${token}`);

    const res = await fetch(fullPath, { ...options, headers });
    if (!res.ok) {
      const text = await res.text();
      let json = null;
      try { json = JSON.parse(text); } catch (_) {}
      const msg = (json && json.message) ? json.message : `HTTP ${res.status}`;
      if (res.status === 403 && json && json.code === 1030) {
        showBlockedOverlay(msg);
      } else if ((res.status === 401 || res.status === 403) && token) {
        goToLogin('Phiên đăng nhập hết hạn, vui lòng đăng nhập lại!');
      }
      throw new Error(msg);
    }
    return res.blob();
  }

  function normalizeImageUrl(url) {
    if (!url) return '';
    if (url.startsWith('http')) return url;
    const BACKEND_URL = 'http://localhost:8080';
    // Nếu url bắt đầu bằng /uploads/ hoặc các đường dẫn tĩnh khác
    if (url.startsWith('/')) return BACKEND_URL + url;
    return BACKEND_URL + '/' + url;
  }

  window.TB = window.TB || {};
  window.TB.apiFetch = apiFetch;
  window.TB.apiFetchBlob = apiFetchBlob;
  window.TB.normalizeImageUrl = normalizeImageUrl;
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
    // connectSessionStream(); // DISABLED to prevent thread starvation during review
    validateCurrentSession();
    setInterval(() => {
        const token = getToken();
        if (token) {
            // Disabled: if (!sessionEventSource || sessionEventSource.readyState === 2) connectSessionStream();
            validateCurrentSession();
        }
    }, 60000);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', startAuthWatch, { once: true });
  } else {
    startAuthWatch();
  }
})();
