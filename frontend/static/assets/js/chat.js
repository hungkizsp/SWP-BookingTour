document.addEventListener('DOMContentLoaded', () => {
  const user = sessionStorage.getItem('user') ? JSON.parse(sessionStorage.getItem('user')) : null;
  if (!user) {
    TB.goToLogin('Vui lòng đăng nhập để có thể sử dụng tính năng Chat.');
    return;
  }

  const box = document.getElementById('chatBox');
  const chatForm = document.getElementById('form');
  const who = document.getElementById('who');
  const escalateBtn = document.getElementById('escalateBtn');
  const endSessionBtn = document.getElementById('endSessionBtn');
  const escalationStatus = document.getElementById('escalationStatus');
  let currentSession = { status: 'AI' };

  const userId = user?.id ?? null;
  const isGuest = !user;
  let guestId = null;
  if (isGuest) {
    guestId = sessionStorage.getItem('guestId');
    if (!guestId) {
      guestId = 'guest_' + Math.random().toString(36).substring(2, 11);
      sessionStorage.setItem('guestId', guestId);
    }
  }

  if (who) {
    who.textContent = user ? (user.fullName || user.email) : 'Guest';
    const container = who.parentElement;
    container.querySelectorAll('.btn-secondary-context').forEach(b => b.remove());

    if (user) {
      const logoutBtn = document.createElement('button');
      logoutBtn.className = 'btn btn-secondary btn-secondary-context';
      logoutBtn.style.marginLeft = '10px';
      logoutBtn.textContent = 'Logout';
      logoutBtn.onclick = () => TB.logout();

      const isAdmin = String(user.role || '').toUpperCase() === 'ADMIN';
      if (isAdmin) {
        const adminBtn = document.createElement('button');
        adminBtn.className = 'btn btn-secondary btn-secondary-context';
        adminBtn.textContent = 'Chat escalations';
        adminBtn.style.marginLeft = '10px';
        adminBtn.onclick = () => window.location.href = '/pages/admin/chat-escalations.html';
        container.appendChild(adminBtn);
      }
      container.appendChild(logoutBtn);
    }
  }

  function escapeHtml(s) {
    return String(s)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }

  function formatAiText(text) {
    if (!text) return '';
    let html = escapeHtml(text);
    html = html.replace(/\[(.+?)\]\((https?:\/\/.+?)\)/g, '<a href="$2" target="_blank" style="color: var(--primary); font-weight: 800; text-decoration: underline;">$1</a>');
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/\n/g, '<br>');
    return html;
  }

  let isRendering = false;
  let lastMessageCount = -1;

  async function load() {
    if (isRendering) return;
    try {
      const q = isGuest ? `?guestId=${encodeURIComponent(guestId)}` : `?userId=${encodeURIComponent(userId)}`;
      const res = await TB.apiFetch(`/api/v1/chat/messages${q}`);
      const msgs = res.data || [];

      console.log("[Chat] Current messages count:", msgs.length);

      if (msgs.length !== lastMessageCount) {
        console.log("[Chat] Count changed, rebuilding UI...");
        await render(msgs);
        lastMessageCount = msgs.length;
      }
    } catch (err) {
      console.error('Load history error', err);
    }
  }

  async function render(msgs) {
    if (isRendering) return;
    isRendering = true;
    try {
      const seen = new Set();
      const unique = [];
      msgs.forEach(m => {
        if (m.id && seen.has(m.id)) {
          console.warn("[Chat] Duplicate ID detected from server:", m.id);
          return;
        }
        if (m.id) seen.add(m.id);
        unique.push(m);
      });
      unique.sort((a, b) => new Date(a.sentAt) - new Date(b.sentAt));

      box.innerHTML = '';
      if (unique.length === 0) {
        proactiveGreeting();
      } else {
        unique.forEach(m => {
          const senderType = (m.senderType || 'USER').toUpperCase();
          const isAdminSender = senderType === 'STAFF' || senderType === 'ADMIN';
          const isAiSender = senderType === 'AI';
          const isUserSender = !isAdminSender && !isAiSender;

          const fromMe = (isUserSender && (
            (m.userId && userId && m.userId === userId)
            || (!m.userId && m.guestId && m.guestId === guestId)
          )) || (isAdminSender && user && user.role === 'ADMIN');

          const div = document.createElement('div');

          let cssClass = 'user';
          if (isAiSender) cssClass = 'ai';
          else if (isAdminSender) cssClass = 'staff';

          div.className = `msg ${cssClass}`;
          if (fromMe) div.classList.add('me');

          const label = isAiSender ? '🤖 Trợ lý AI' : (isAdminSender ? 'Nhân viên' : 'Bạn');
          const content = senderType === 'AI' ? formatAiText(m.message) : escapeHtml(m.message);
          div.innerHTML = `
            <div style="font-size:0.75rem;opacity:0.7;margin-bottom:4px;">${label}</div>
            <div>${content}</div>
          `;
          box.appendChild(div);
        });
        box.scrollTo({ top: box.scrollHeight, behavior: 'smooth' });
      }
    } finally {
      isRendering = false;
    }
  }

  function proactiveGreeting() {
    const div = document.createElement('div');
    div.className = 'msg ai';
    const welcome = '👋 Xin chào! Tôi là trợ lý tư vấn tour du lịch của Dana.\n\nBạn muốn đi đâu? Hãy cho tôi biết địa điểm, thời gian hoặc ngân sách, tối sẽ gợi ý tour tốt nhất cho bạn! 😊';
    div.innerHTML = `<div style="font-size:0.75rem;opacity:0.7;margin-bottom:4px;">🤖 Trợ lý AI</div><div>${formatAiText(welcome)}</div>`;
    box.appendChild(div);
  }

  let isSending = false;
  chatForm.onsubmit = async (e) => {
    e.preventDefault();
    if (isSending) {
      console.warn("[Chat] Blocked duplicate submit attempt.");
      return;
    }

    const input = document.getElementById('text');
    const content = input.value.trim();
    if (!content) return;

    isSending = true;
    const now = new Date().getTime();
    console.log(`[Chat] Submitting message at ${now}: "${content}"`);

    input.value = '';
    const btn = chatForm.querySelector('button[type="submit"]');
    if (btn) btn.disabled = true;

    try {
      await TB.apiFetch('/api/v1/chat/messages', {
        method: 'POST',
        body: JSON.stringify({ userId, guestId: isGuest ? guestId : null, message: content, senderType: 'USER' })
      });
      console.log(`[Chat] Post success for message at ${now}`);
      await load();

      const sessionStatus = currentSession?.status || 'AI';
      const isHandledByStaff = sessionStatus === 'STAFF_CHATTING' || sessionStatus === 'WAITING_STAFF';
      if (!isHandledByStaff) {
        setTimeout(() => getAiResponse(content), 600);
      } else {
        console.log("[Chat] AI silent because staff is handling or requested.");
      }
    } catch (err) {
      console.error("[Chat] Post failed", err);
      alert('Không thể gửi tin nhắn.');
    } finally {
      isSending = false;
      if (btn) btn.disabled = false;
    }
  };

  async function getAiResponse(userMsg) {
    const typing = document.createElement('div');
    typing.className = 'msg ai';
    typing.id = 'typing-indicator';
    typing.innerHTML = `<div style="font-size:0.75rem;opacity:0.7;margin-bottom:4px;">🤖 Trợ lý AI</div><div style="opacity:0.6;">⏳ Đang soạn tin nhắn...</div>`;
    box.appendChild(typing);
    box.scrollTo({ top: box.scrollHeight, behavior: 'smooth' });
    try {
      await TB.apiFetch('/api/v1/ai/chat', { method: 'POST', body: JSON.stringify({ message: userMsg, userId, guestId: isGuest ? guestId : null }) });
      document.getElementById('typing-indicator')?.remove();
      await load();
    } catch (err) {
      document.getElementById('typing-indicator')?.remove();
      await load();
    }
  }

  const statusBar = document.getElementById('connection-status-bar');
  const resetToAiBtn = document.getElementById('resetToAiBtn');
  const chatInput = document.getElementById('text');
  const submitBtn = chatForm ? chatForm.querySelector('button[type="submit"]') : null;

  function resetToIdleMode() {
    if (statusBar) statusBar.style.display = 'none';
    if (escalationStatus) escalationStatus.hidden = true;
    if (endSessionBtn) endSessionBtn.hidden = true;
    if (escalateBtn) escalateBtn.style.display = 'inline-block';
    if (resetToAiBtn) resetToAiBtn.style.display = 'none';
    if (chatInput) { chatInput.disabled = false; chatInput.placeholder = 'Nhập tin nhắn của bạn...'; }
    if (submitBtn) submitBtn.disabled = false;
  }

  function showWaitingStaffBar() {
    if (statusBar) {
      statusBar.style.display = 'block';
      statusBar.style.background = '#eff6ff';
      statusBar.style.color = '#1e40af';
      statusBar.style.border = '1px solid #bfdbfe';
      statusBar.textContent = '⏳ Hệ thống đã ghi nhận yêu cầu. Đang kết nối bạn với nhân viên hỗ trợ, vui lòng đợi trong giây lát...';
    }
    if (escalationStatus) escalationStatus.hidden = true;
    if (escalateBtn) escalateBtn.style.display = 'none';
    if (resetToAiBtn) resetToAiBtn.style.display = 'none';
    if (chatInput) { chatInput.disabled = false; chatInput.placeholder = 'Nhập tin nhắn của bạn...'; }
    if (submitBtn) submitBtn.disabled = false;
  }

  function showStaffChattingBar(staffName) {
    if (statusBar) {
      statusBar.style.display = 'block';
      statusBar.style.background = '#f0fdf4';
      statusBar.style.color = '#166534';
      statusBar.style.border = '1px solid #bbf7d0';
      statusBar.textContent = `🟢 Bạn đang trò chuyện với nhân viên hỗ trợ [${staffName}].`;
    }
    if (escalateBtn) escalateBtn.style.display = 'none';
    if (resetToAiBtn) resetToAiBtn.style.display = 'none';
    if (chatInput) { chatInput.disabled = false; chatInput.placeholder = 'Nhập tin nhắn của bạn...'; }
    if (submitBtn) submitBtn.disabled = false;
  }

  function showClosedState() {
    if (statusBar) statusBar.style.display = 'none';
    if (chatInput) {
      chatInput.disabled = true;
      chatInput.placeholder = 'Cuộc trò chuyện đã kết thúc...';
    }
    if (submitBtn) submitBtn.disabled = true;
    if (resetToAiBtn) resetToAiBtn.style.display = 'flex';
  }

  /** Chỉ điều khiển UI theo trạng thái chính thức của ChatSession (API / SSE). */
  function renderSessionStatus(session) {
    currentSession = session || { status: 'AI' };
    const status = currentSession.status || 'AI';

    switch (status) {
      case 'WAITING_STAFF':
        showWaitingStaffBar();
        break;
      case 'STAFF_CHATTING':
        showStaffChattingBar(currentSession.assignedStaffName || 'Nhân viên hỗ trợ');
        break;
      case 'CLOSED':
        showClosedState();
        break;
      case 'AI':
      default:
        resetToIdleMode();
        break;
    }
  }

  // Khởi tạo EventSource để lắng nghe sự kiện từ Server-Sent Events (SSE)
  let sseSource = null;
  function initSse() {
    if (sseSource) {
      sseSource.close();
    }

    // Khi chạy ở dev server (port 3000/5500), phải trỏ thẳng về Backend host
    // (giống cách apiFetch trong api.js xử lý)
    const BACKEND_URL = 'http://localhost:8080';
    const isDev = window.location.port === '3000' || window.location.port === '5500';
    const sseUrl = isDev ? `${BACKEND_URL}/api/v1/chat/messages/stream` : '/api/v1/chat/messages/stream';

    console.log("[SSE] Connecting to:", sseUrl);
    sseSource = new EventSource(sseUrl);

    sseSource.addEventListener('chat-message', (e) => {
      try {
        const msg = JSON.parse(e.data);
        console.log("[SSE] chat-message received:", msg);

        if (msg.status === 'CLOSED' || msg.type === 'SESSION_CLOSED') {
          renderSessionStatus({ status: 'CLOSED' });
          appendSystemMessage(msg.message || 'Cuộc hỗ trợ đã kết thúc bởi nhân viên. Cảm ơn bạn!');
          load();
          return;
        }
        if (msg.status === 'STAFF_CHATTING' || msg.type === 'SESSION_ASSIGNED') {
          renderSessionStatus({
            status: 'STAFF_CHATTING',
            assignedStaffName: msg.staffName || 'Nhân viên hỗ trợ'
          });
          load();
          return;
        }
        if (msg.status === 'WAITING_STAFF') {
          renderSessionStatus({ status: 'WAITING_STAFF' });
          load();
          return;
        }

        load();
      } catch (err) {
        console.error("[SSE] Failed to parse event message:", err);
      }
    });

    sseSource.addEventListener('session-assigned', (e) => {
      try {
        const data = JSON.parse(e.data);
        console.log("[SSE] session-assigned event received:", data);
        renderSessionStatus({
          status: 'STAFF_CHATTING',
          assignedStaffName: data.staffName || 'Nhân viên hỗ trợ'
        });
        load();
      } catch (err) { }
    });

    sseSource.addEventListener('session-closed', (e) => {
      try {
        const data = JSON.parse(e.data);
        console.log("[SSE] session-closed event received:", data);
        renderSessionStatus({ status: 'CLOSED' });
        appendSystemMessage(data.message || 'Cuộc hỗ trợ đã kết thúc bởi nhân viên. Cảm ơn bạn!');
        load();
      } catch (err) { }
    });

    sseSource.onopen = () => {
      console.log("[SSE] Connection established successfully.");
    };

    sseSource.onerror = (err) => {
      console.warn("[SSE] Connection error. Sse connection closed or server offline.", err);
    };
  }

  function appendSystemMessage(text) {
    // Tránh append trùng lặp thông báo kết thúc
    const existSystemMsgs = box.querySelectorAll('.msg.system-alert');
    if (existSystemMsgs.length > 0) return;

    const infoDiv = document.createElement('div');
    infoDiv.className = 'msg ai system-alert';
    infoDiv.style.alignSelf = 'center';
    infoDiv.style.background = '#fee2e2';
    infoDiv.style.border = '1px solid #fecaca';
    infoDiv.style.color = '#991b1b';
    infoDiv.style.padding = '10px 20px';
    infoDiv.style.borderRadius = '8px';
    infoDiv.style.margin = '10px 0';
    infoDiv.innerHTML = `<div style="font-size:0.9rem; font-weight:700; text-align:center;">⚠️ ${text}</div>`;

    box.appendChild(infoDiv);
    box.scrollTo({ top: box.scrollHeight, behavior: 'smooth' });
  }

  async function loadSessionStatus() {
    try {
      const q = isGuest ? `?guestId=${encodeURIComponent(guestId)}` : `?userId=${encodeURIComponent(userId)}`;
      const res = await TB.apiFetch(`/api/v1/chat/session${q}`);
      renderSessionStatus(res.data);
    } catch (err) {
      console.error('Load session status error', err);
    }
  }

  if (escalateBtn) {
    escalateBtn.onclick = async () => {
      const note = chatInput ? chatInput.value.trim() : 'Cần hỗ trợ từ nhân viên';
      try {
        await TB.apiFetch('/api/v1/chat/escalations', {
          method: 'POST',
          body: JSON.stringify({ userId, guestId: isGuest ? guestId : null, requestNote: note })
        });
        if (chatInput) chatInput.value = '';
        await loadSessionStatus();
      } catch (err) {
        console.error("Escalation failed", err);
      }
    }
  }

  if (resetToAiBtn) {
    resetToAiBtn.onclick = async () => {
      // Gửi API thông báo đóng/hoàn thành cuộc trò chuyện ở phía DB
      try {
        await TB.apiFetch('/api/v1/chat/escalations/close', {
          method: 'POST',
          body: JSON.stringify({ userId, guestId: isGuest ? guestId : null })
        });
      } catch (err) {
        console.warn("Could not close active escalation on server, resetting locally:", err);
      }

      // Mở khóa giao diện và reset các biến
      if (chatInput) {
        chatInput.disabled = false;
        chatInput.placeholder = "Nhập tin nhắn của bạn...";
        chatInput.value = "";
      }
      if (submitBtn) {
        submitBtn.disabled = false;
      }

      resetToAiBtn.style.display = 'none';
      if (escalateBtn) escalateBtn.style.display = 'inline-block';
      if (statusBar) statusBar.style.display = 'none';

      currentSession = { status: 'AI' };
      lastMessageCount = -1;

      // Xóa lịch sử hiển thị và chào lại tự động
      box.innerHTML = '';
      proactiveGreeting();
    };
  }

  resetToIdleMode();
  load();
  loadSessionStatus();
  initSse();

  setInterval(load, 10000);
  setInterval(loadSessionStatus, 15000);
});