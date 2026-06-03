(()=>{
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
  let currentEscalation = null;

  const userId = user?.id ?? null;
  const isGuest = !user;
  let guestId = null;
  if (isGuest) {
    guestId = localStorage.getItem('guestId');
    if (!guestId) {
      guestId = 'guest_' + Math.random().toString(36).substring(2, 11);
      localStorage.setItem('guestId', guestId);
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
      
      const isHandledByStaff = currentEscalation && (currentEscalation.status === 'STAFF_CHATTING' || currentEscalation.status === 'WAITING_STAFF');
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

  async function loadEscalation() {
    try {
      const q = isGuest ? `?guestId=${encodeURIComponent(guestId)}` : `?userId=${encodeURIComponent(userId)}`;
      const res = await TB.apiFetch(`/api/v1/chat/escalations/active${q}`);
      renderEscalation(res.data);
    } catch (err) {}
  }

  function renderEscalation(data) {
    currentEscalation = data;
    if (!escalationStatus) return;
    if (!data) {
      escalationStatus.hidden = true;
      if (endSessionBtn) endSessionBtn.hidden = true;
      return;
    }
    const labels = { OPEN: 'Đã báo nhân viên.', IN_REVIEW: 'Nhân viên đang xem.', WAITING_STAFF: 'Đang đợi nhân viên.' };
    escalationStatus.textContent = labels[data.status] || 'Nhân viên đã được thông báo.';
    escalationStatus.hidden = false;
    if (endSessionBtn) endSessionBtn.hidden = ['RESOLVED', 'CLOSED'].includes(data.status);
  }

  if (escalateBtn) {
    escalateBtn.onclick = async () => {
      const input = document.getElementById('text');
      const note = input.value.trim() || 'Cần hỗ trợ từ nhân viên';
      try {
        await TB.apiFetch('/api/v1/chat/escalations', { method: 'POST', body: JSON.stringify({ userId, guestId: isGuest ? guestId : null, requestNote: note }) });
        input.value = '';
        loadEscalation();
      } catch (err) {}
    }
  }

  load();
  loadEscalation();
  setInterval(load, 3000);
  setInterval(loadEscalation, 5000);
})();