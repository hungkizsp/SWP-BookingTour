(() => {
  const DEFAULT_IMG = 'data:image/svg+xml;utf8,' + encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="44" height="44"><rect width="44" height="44" rx="10" fill="#e2e8f0"/><text x="22" y="27" font-size="18" text-anchor="middle" fill="#94a3b8" font-family="sans-serif">🏞️</text></svg>'
  );

  function getUser() {
    try { return JSON.parse(sessionStorage.getItem('user') || 'null'); } catch (e) { return null; }
  }

  function injectStyles() {
    if (document.getElementById('gcwStyles')) return;
    const style = document.createElement('style');
    style.id = 'gcwStyles';
    style.textContent = `
      .gcw-bubble {
        position: fixed; bottom: 24px; right: 24px; width: 60px; height: 60px;
        border-radius: 50%; background: var(--primary, #064e3b); color: white;
        font-size: 26px; border: none; box-shadow: 0 8px 24px rgba(0,0,0,0.25);
        cursor: pointer; z-index: 9999; display: flex; align-items: center; justify-content: center;
        transition: transform 0.2s ease;
      }
      .gcw-bubble:hover { transform: scale(1.08); }
      .gcw-badge {
        position: absolute; top: -4px; right: -4px; background: #ef4444; color: white;
        font-size: 11px; font-weight: 800; border-radius: 999px; min-width: 20px; height: 20px;
        display: flex; align-items: center; justify-content: center; padding: 0 4px;
        border: 2px solid white;
      }
      .gcw-panel {
        position: fixed; bottom: 96px; right: 24px; width: 320px; max-height: 420px;
        background: white; border-radius: 16px; box-shadow: 0 16px 40px rgba(0,0,0,0.2);
        overflow: hidden; z-index: 9999; display: none; flex-direction: column;
        font-family: inherit;
      }
      .gcw-panel.open { display: flex; }
      .gcw-panel-header {
        padding: 16px; font-weight: 800; border-bottom: 1px solid #eee;
        color: var(--primary, #064e3b); flex-shrink: 0;
      }
      .gcw-list { overflow-y: auto; }
      .gcw-item {
        display: flex; gap: 12px; padding: 12px 16px; cursor: pointer;
        border-bottom: 1px solid #f1f5f9; align-items: center;
      }
      .gcw-item:hover { background: #f8fafc; }
      .gcw-item img { width: 44px; height: 44px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }
      .gcw-item-title { font-weight: 700; font-size: 0.9rem; color: #0f172a; }
      .gcw-item-sub {
        font-size: 0.78rem; color: #64748b; margin-top: 2px;
        white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 220px;
      }
    `;
    document.head.appendChild(style);
  }

  function buildWidget(groups) {
    injectStyles();

    const root = document.createElement('div');
    root.id = 'gcwRoot';
    root.innerHTML = `
      <div id="gcwPanel" class="gcw-panel">
        <div class="gcw-panel-header">💬 Nhóm chat của bạn</div>
        <div id="gcwList" class="gcw-list"></div>
      </div>
      <button id="gcwBubble" class="gcw-bubble" title="Nhóm chat tour">
        💬
        <span class="gcw-badge">${groups.length}</span>
      </button>
    `;
    document.body.appendChild(root);

    const list = root.querySelector('#gcwList');
    list.innerHTML = groups.map(g => `
      <div class="gcw-item" data-schedule-id="${g.scheduleId}">
        <img src="${g.tourImage ? TB.normalizeImageUrl(g.tourImage) : DEFAULT_IMG}" alt="">
        <div style="min-width:0;">
          <div class="gcw-item-title">${g.tourName || ('Tour #' + g.scheduleId)}</div>
          <div class="gcw-item-sub">${g.lastMessage || 'Chưa có tin nhắn'}</div>
        </div>
      </div>
    `).join('');

    list.querySelectorAll('.gcw-item').forEach(item => {
      item.addEventListener('click', () => {
        window.location.href = `/pages/client/group-chat.html?scheduleId=${item.dataset.scheduleId}`;
      });
    });

    const bubble = root.querySelector('#gcwBubble');
    const panel = root.querySelector('#gcwPanel');
    bubble.addEventListener('click', () => panel.classList.toggle('open'));
    document.addEventListener('click', (e) => {
      if (!root.contains(e.target)) panel.classList.remove('open');
    });
  }

  async function init() {
    const user = getUser();
    if (!user?.id || String(user.role || '').toUpperCase() !== 'CUSTOMER') return;
    if (!window.TB?.apiFetch) return;
    try {
      const res = await TB.apiFetch('/api/v1/group-chats/my-groups');
      const groups = res?.data || [];
      if (!groups.length) return;
      buildWidget(groups);
    } catch (e) {
      console.error('Failed to load group chat widget', e);
    }
  }

  init();
})();
