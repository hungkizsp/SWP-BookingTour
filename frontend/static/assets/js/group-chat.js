/**
 * group-chat.js  —  Tour Group Chat feature
 * Handles: group list, entering a group, SSE live messages, sending messages.
 * Exposed globally as window.groupChat so chat.html can delegate to it.
 * Zero collision with the existing AI/staff chat logic in chat.js.
 */
(() => {
  const $ = (id) => document.getElementById(id);

  const user = sessionStorage.getItem('user')
    ? JSON.parse(sessionStorage.getItem('user'))
    : null;

  if (!user) return; // Group chat is login-only; chat.js already redirects if needed

  const myUserId = user.id;

  /* ── State ── */
  let currentGroupId   = null;
  let currentGroupActive = true;
  let sseEmitter       = null;
  let pollingTimer     = null;
  let currentPage      = 0;
  let hasMorePages     = true;
  let loadingMore      = false;
  const PAGE_SIZE      = 30;

  /* ── DOM helpers ── */
  const groupListView  = $('groupListView');
  const groupMsgView   = $('groupMsgView');
  const groupListBox   = $('groupListBox');
  const groupChatBox   = $('groupChatBox');
  const groupMsgTitle  = $('groupMsgTitle');
  const groupMsgMeta   = $('groupMsgMeta');
  const groupClosedBanner = $('groupClosedBanner');
  const groupInputArea = $('groupInputArea');
  const groupForm      = $('groupForm');
  const groupText      = $('groupText');
  const groupSendBtn   = $('groupSendBtn');

  /* ── Utility ── */
  function escHtml(s) {
    return String(s || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function fmtTime(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  }

  function fmtDate(iso) {
    if (!iso) return '';
    // iso might be an array [year,month,day] from Jackson LocalDate serialisation
    if (Array.isArray(iso)) {
      const [y, m, d] = iso;
      return `${String(d).padStart(2,'0')}/${String(m).padStart(2,'0')}/${y}`;
    }
    const d = new Date(iso);
    return d.toLocaleDateString('vi-VN');
  }

  /* ── Group list ── */
  async function loadGroups() {
    groupListBox.innerHTML = '<div style="text-align:center;padding:20px;color:var(--text-faint);">Đang tải...</div>';
    try {
      const res = await TB.apiFetch('/api/v1/chat/groups/my');
      const groups = res?.data || [];
      renderGroupList(groups);
    } catch (e) {
      groupListBox.innerHTML = `<div style="color:#ef4444;padding:10px;">Lỗi: ${escHtml(e.message)}</div>`;
    }
  }

  function renderGroupList(groups) {
    if (!groups.length) {
      groupListBox.innerHTML = `
        <div style="text-align:center;padding:30px;color:var(--text-faint);">
          <div style="font-size:2.5rem;margin-bottom:12px;">🏖️</div>
          <div style="font-weight:700;">Bạn chưa có chuyến tour nào sắp tới</div>
          <div style="font-size:0.85rem;margin-top:6px;">Sau khi thanh toán thành công, bạn sẽ tự động vào nhóm chat của tour.</div>
        </div>`;
      return;
    }
    groupListBox.innerHTML = groups.map(g => `
      <div class="group-list-item" onclick='window.groupChat.openGroup(${g.id}, ${JSON.stringify(escHtml(g.tourName))}, ${JSON.stringify(fmtDate(g.startDate))}, ${g.memberCount}, ${g.isActive})'>
        <span class="g-name">${escHtml(g.tourName)}</span>
        <span class="g-meta">🗓️ Khởi hành: ${fmtDate(g.startDate)} &nbsp;|&nbsp; 👥 ${g.memberCount} thành viên${!g.isActive ? ' &nbsp;|&nbsp; 🔒 Đã đóng' : ''}</span>
      </div>
    `).join('');
  }

  /* ── Enter a specific group ── */
  function openGroup(groupId, tourName, startDate, memberCount, isActive) {
    currentGroupId     = groupId;
    currentGroupActive = isActive;
    currentPage        = 0;
    hasMorePages       = true;

    groupMsgTitle.textContent = tourName;
    groupMsgMeta.textContent  = `🗓️ ${startDate} · 👥 ${memberCount} thành viên`;

    if (!isActive) {
      groupClosedBanner.style.display = '';
      groupInputArea.style.display    = 'none';
    } else {
      groupClosedBanner.style.display = 'none';
      groupInputArea.style.display    = '';
    }

    groupChatBox.innerHTML = '';
    groupListView.style.display  = 'none';
    groupMsgView.style.display   = 'flex';

    loadMessages(true);
    connectSse(groupId);

    // Scroll-up to load more
    groupChatBox.onscroll = () => {
      if (groupChatBox.scrollTop < 50 && hasMorePages && !loadingMore) {
        loadMessages(false);
      }
    };
  }

  function showGroupList() {
    disconnectSse();
    clearInterval(pollingTimer);
    currentGroupId = null;
    groupMsgView.style.display  = 'none';
    groupListView.style.display = '';
    loadGroups();
  }

  /* ── Load messages ── */
  async function loadMessages(initial) {
    if (loadingMore) return;
    loadingMore = true;
    try {
      const res = await TB.apiFetch(`/api/v1/chat/groups/${currentGroupId}/messages?page=${currentPage}&size=${PAGE_SIZE}`);
      const pageData = res?.data || {};
      const msgs = pageData.content || [];
      hasMorePages = !pageData.last;

      if (initial) {
        // API returns newest-first; reverse to display oldest→newest
        renderMessages(msgs.slice().reverse(), true);
        scrollToBottom();
        currentPage = 1;
      } else {
        // Prepend older messages
        const prevHeight = groupChatBox.scrollHeight;
        renderMessages(msgs.slice().reverse(), false, true);
        groupChatBox.scrollTop = groupChatBox.scrollHeight - prevHeight;
        currentPage++;
      }
    } catch (e) {
      console.error('[GroupChat] loadMessages error', e);
    } finally {
      loadingMore = false;
    }
  }

  /* Avoid duplicate message IDs */
  const renderedIds = new Set();

  function renderMessages(msgs, clearFirst, prepend = false) {
    if (clearFirst) {
      groupChatBox.innerHTML = '';
      renderedIds.clear();
    }
    msgs.forEach(m => {
      if (renderedIds.has(m.id)) return;
      renderedIds.add(m.id);
      const el = buildBubble(m);
      if (prepend) {
        groupChatBox.insertBefore(el, groupChatBox.firstChild);
      } else {
        groupChatBox.appendChild(el);
      }
    });
  }

  function buildBubble(msg) {
    const isMe = msg.userId === myUserId || msg.userId === String(myUserId);
    const side  = isMe ? 'me' : 'other';
    const wrap  = document.createElement('div');
    wrap.className  = `gmsg-wrap ${side}`;
    wrap.dataset.id = msg.id;

    wrap.innerHTML = `
      ${!isMe ? `<div class="gmsg-name">${escHtml(msg.displayName)}</div>` : ''}
      <div class="gmsg-bubble">${escHtml(msg.content)}</div>
      <div class="gmsg-time">${fmtTime(msg.sentAt)}</div>
    `;
    return wrap;
  }

  function scrollToBottom() {
    groupChatBox.scrollTop = groupChatBox.scrollHeight;
  }

  /* ── SSE for real-time messages ── */
  function connectSse(groupId) {
    disconnectSse();
    const BACKEND = 'http://localhost:8080';
    const isDev   = window.location.port === '3000' || window.location.port === '5500';
    const token   = sessionStorage.getItem('token') || localStorage.getItem('token') || '';

    // SSE doesn't support custom headers; pass token as query param if needed
    // (backend must allow ?token= or rely on cookie session)
    const base = isDev ? BACKEND : '';
    const url  = `${base}/api/v1/chat/groups/${groupId}/messages/stream${token ? '?token=' + encodeURIComponent(token) : ''}`;

    try {
      sseEmitter = new EventSource(url, { withCredentials: true });

      sseEmitter.addEventListener('group-message', (e) => {
        try {
          const msg = JSON.parse(e.data);
          if (msg.groupId !== currentGroupId && msg.groupId !== String(currentGroupId)) return;
          renderMessage(msg);
        } catch (err) {
          console.error('[GroupChat-SSE] parse error', err);
        }
      });

      sseEmitter.addEventListener('connected', (e) => {
        console.log('[GroupChat-SSE] SSE connected successfully', e.data);
      });

      sseEmitter.addEventListener('error', (e) => {
        console.error('[GroupChat-SSE] Server error:', e.data);
        disconnectSse();
      });

      sseEmitter.onopen  = () => console.log('[GroupChat-SSE] connected to group', groupId);
      sseEmitter.onerror = () => {
        console.warn('[GroupChat-SSE] error / disconnected, falling back to polling');
        disconnectSse();
        startPolling();
      };
    } catch (err) {
      console.warn('[GroupChat-SSE] could not init, using polling', err);
      startPolling();
    }
  }

  function disconnectSse() {
    if (sseEmitter) { sseEmitter.close(); sseEmitter = null; }
    clearInterval(pollingTimer);
  }

  function startPolling() {
    clearInterval(pollingTimer);
    pollingTimer = setInterval(async () => {
      if (!currentGroupId) return;
      try {
        const res = await TB.apiFetch(`/api/v1/chat/groups/${currentGroupId}/messages?page=0&size=${PAGE_SIZE}`);
        const msgs = (res?.data?.content || []).slice().reverse();
        msgs.forEach(m => {
          if (renderedIds.has(m.id)) return;
          renderedIds.add(m.id);
          groupChatBox.appendChild(buildBubble(m));
        });
        if (msgs.length) scrollToBottom();
      } catch (e) { /* silent */ }
    }, 5000);
  }

  /* ── Helper: Render a single message ── */
  function renderMessage(msg) {
    if (!msg || !msg.id) return;
    if (renderedIds.has(msg.id)) return;
    renderedIds.add(msg.id);
    
    const el = buildBubble(msg);
    groupChatBox.appendChild(el);
    scrollToBottom();
  }

  /* ── Helper: Send message to API ── */
  async function sendMessage(content) {
    if (!content) return;
    
    try {
      const res = await TB.apiFetch(`/api/v1/chat/groups/${currentGroupId}/messages`, {
        method: 'POST',
        body: JSON.stringify({ content })
      });
      
      const msg = res?.data;
      // If SSE is not connected, render directly
      if (!sseEmitter && msg) {
        renderMessage(msg);
      }
    } catch (err) {
      console.error('[GroupChat] send error', err);
      alert('Không thể gửi tin nhắn: ' + err.message);
    }
  }

  /* ── Form Submit (User action only) ── */
  groupSendBtn.addEventListener('click', (e) => {
    e.preventDefault();
    if (!currentGroupId || !currentGroupActive) return;
    
    const content = groupText.value.trim();
    if (!content) return;

    groupText.value = '';
    groupSendBtn.disabled = true;

    sendMessage(content).finally(() => {
      groupSendBtn.disabled = false;
      groupText.focus();
    });
  });

  groupText.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      groupSendBtn.click();
    }
  });

  /* ── Public API ── */
  window.groupChat = { loadGroups, openGroup, showGroupList };
})();
