(() => {
  const user = JSON.parse(sessionStorage.getItem('user') || 'null');
  if (!user?.id) return;

  function backendUrl(path) {
    const BACKEND = 'http://localhost:8080';
    const isDev = window.location.port === '3000' || window.location.port === '5500';
    return (isDev && path.startsWith('/')) ? BACKEND + path : path;
  }

  // ── Inject CSS ──────────────────────────────────────────────
  const style = document.createElement('style');
  style.textContent = `
    #nb-btn {
      position: fixed; top: 18px; right: 18px; z-index: 10000;
      width: 44px; height: 44px; border-radius: 50%;
      background: #0f766e; color: white; border: none; cursor: pointer;
      font-size: 1.2rem; display: flex; align-items: center; justify-content: center;
      box-shadow: 0 4px 14px rgba(0,0,0,0.22);
      transition: transform 0.18s, box-shadow 0.18s;
    }
    #nb-btn:hover { transform: scale(1.08); box-shadow: 0 6px 20px rgba(0,0,0,0.28); }
    #nb-badge {
      position: absolute; top: -4px; right: -4px;
      background: #ef4444; color: white; border-radius: 99px;
      font-size: 0.65rem; font-weight: 800; min-width: 18px; height: 18px;
      display: none; align-items: center; justify-content: center; padding: 0 4px;
      border: 2px solid white; pointer-events: none;
    }
    #nb-panel {
      position: fixed; top: 70px; right: 18px; z-index: 10000;
      width: 340px; max-height: 480px;
      background: white; border-radius: 16px; border: 1px solid #e2e8f0;
      box-shadow: 0 16px 40px rgba(0,0,0,0.18);
      display: none; flex-direction: column; overflow: hidden;
      animation: nb-slide-in 0.2s ease;
    }
    #nb-panel.open { display: flex; }
    @keyframes nb-slide-in {
      from { opacity: 0; transform: translateY(-8px); }
      to   { opacity: 1; transform: translateY(0); }
    }
    .nb-panel-header {
      padding: 14px 18px; border-bottom: 1px solid #f1f5f9;
      display: flex; justify-content: space-between; align-items: center;
      font-weight: 800; font-size: 0.95rem; color: #0f766e;
    }
    #nb-mark-all {
      font-size: 0.75rem; color: #64748b; cursor: pointer; font-weight: 600;
      background: none; border: none; padding: 0;
    }
    #nb-mark-all:hover { color: #0f766e; }
    #nb-list { overflow-y: auto; flex: 1; }
    .nb-item {
      padding: 13px 18px; border-bottom: 1px solid #f8fafc; cursor: pointer;
      transition: background 0.15s;
    }
    .nb-item:hover { background: #f8fafc; }
    .nb-item.unread { background: #f0fdfa; }
    .nb-item .nb-title { font-weight: 700; font-size: 0.88rem; color: #1e293b; margin-bottom: 3px; }
    .nb-item .nb-msg { font-size: 0.8rem; color: #64748b; line-height: 1.4; }
    .nb-item .nb-time { font-size: 0.72rem; color: #94a3b8; margin-top: 4px; }
    .nb-empty { padding: 30px; text-align: center; color: #94a3b8; font-size: 0.88rem; }
  `;
  document.head.appendChild(style);

  // ── Inject DOM ──────────────────────────────────────────────
  const btn = document.createElement('button');
  btn.id = 'nb-btn';
  btn.title = 'Thông báo';
  btn.innerHTML = `🔔<span id="nb-badge"></span>`;

  const panel = document.createElement('div');
  panel.id = 'nb-panel';
  panel.innerHTML = `
    <div class="nb-panel-header">
      <span>Thông báo</span>
      <button id="nb-mark-all">Đánh dấu tất cả đã đọc</button>
    </div>
    <div id="nb-list"><div class="nb-empty">Đang tải...</div></div>
  `;

  document.body.appendChild(btn);
  document.body.appendChild(panel);

  const badge = document.getElementById('nb-badge');
  const list = document.getElementById('nb-list');

  // ── Toggle panel ────────────────────────────────────────────
  btn.addEventListener('click', (e) => {
    e.stopPropagation();
    panel.classList.toggle('open');
    if (panel.classList.contains('open')) loadNotifications();
  });

  document.addEventListener('click', (e) => {
    if (!panel.contains(e.target) && e.target !== btn) {
      panel.classList.remove('open');
    }
  });

  document.getElementById('nb-mark-all').addEventListener('click', async () => {
    await TB.apiFetch('/api/v1/notifications/read-all', { method: 'PATCH' });
    setBadge(0);
    document.querySelectorAll('.nb-item.unread').forEach(el => el.classList.remove('unread'));
  });

  // ── Badge helpers ───────────────────────────────────────────
  function setBadge(count) {
    if (count > 0) {
      badge.textContent = count > 99 ? '99+' : count;
      badge.style.display = 'flex';
    } else {
      badge.style.display = 'none';
    }
  }

  // ── Render notifications ────────────────────────────────────
  function timeAgo(dateStr) {
    if (!dateStr) return '';
    const diff = Math.floor((Date.now() - new Date(dateStr).getTime()) / 1000);
    if (diff < 60) return 'Vừa xong';
    if (diff < 3600) return Math.floor(diff / 60) + ' phút trước';
    if (diff < 86400) return Math.floor(diff / 3600) + ' giờ trước';
    return Math.floor(diff / 86400) + ' ngày trước';
  }

  function renderList(items) {
    if (!items || items.length === 0) {
      list.innerHTML = '<div class="nb-empty">Chưa có thông báo nào.</div>';
      return;
    }
    list.innerHTML = items.map(n => `
      <div class="nb-item ${n.isRead ? '' : 'unread'}" data-id="${n.id}" data-link="${n.link || ''}">
        <div class="nb-title">${escHtml(n.title || '')}</div>
        <div class="nb-msg">${escHtml(n.message || '')}</div>
        <div class="nb-time">${timeAgo(n.createdAt)}</div>
      </div>
    `).join('');

    list.querySelectorAll('.nb-item').forEach(el => {
      el.addEventListener('click', async () => {
        const id = Number(el.dataset.id);
        const link = el.dataset.link;
        if (!el.classList.contains('nb-item') || el.classList.contains('read-loading')) return;
        el.classList.add('read-loading');
        try {
          await TB.apiFetch(`/api/v1/notifications/${id}/read`, { method: 'PATCH' });
          el.classList.remove('unread');
          const unreadNow = list.querySelectorAll('.nb-item.unread').length;
          setBadge(unreadNow);
        } catch (_) {}
        el.classList.remove('read-loading');
        if (link) window.location.href = backendUrl(link);
      });
    });
  }

  async function loadNotifications() {
    try {
      const res = await TB.apiFetch('/api/v1/notifications/my');
      renderList(res.data || []);
    } catch (e) {
      list.innerHTML = '<div class="nb-empty">Không thể tải thông báo.</div>';
    }
  }

  function escHtml(str) {
    const d = document.createElement('div');
    d.textContent = str;
    return d.innerHTML;
  }

  // ── SSE stream ──────────────────────────────────────────────
  function connectStream() {
    const token = TB.getToken();
    if (!token) return;
    const url = backendUrl(`/api/v1/notifications/stream?token=${encodeURIComponent(token)}`);
    const source = new EventSource(url);

    source.addEventListener('unread-count', (e) => {
      setBadge(Number(e.data));
    });

    source.addEventListener('notification', (e) => {
      try {
        const notif = JSON.parse(e.data);
        // Bump badge
        const current = parseInt(badge.textContent || '0', 10) || 0;
        setBadge(current + 1);
        // If panel open, prepend item
        if (panel.classList.contains('open')) {
          const empty = list.querySelector('.nb-empty');
          if (empty) empty.remove();
          const el = document.createElement('div');
          el.className = 'nb-item unread';
          el.dataset.id = notif.id;
          el.dataset.link = notif.link || '';
          el.innerHTML = `
            <div class="nb-title">${escHtml(notif.title || '')}</div>
            <div class="nb-msg">${escHtml(notif.message || '')}</div>
            <div class="nb-time">Vừa xong</div>
          `;
          list.prepend(el);
        }
      } catch (_) {}
    });

    source.onerror = () => {
      source.close();
      setTimeout(connectStream, 5000);
    };
  }

  // ── Init ────────────────────────────────────────────────────
  async function init() {
    try {
      const res = await TB.apiFetch('/api/v1/notifications/unread-count');
      setBadge(Number(res.data || 0));
    } catch (_) {}
    connectStream();
  }

  init();
})();
