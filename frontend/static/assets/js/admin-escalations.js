(() => {
  const listEl = document.getElementById('escalationList');
  const reloadBtn = document.getElementById('reloadEscalations');
  const infoEl = document.getElementById('escalationInfo');

  async function refresh() {
    try {
      const res = await TB.apiFetch('/api/v1/admin/chat/escalations');
      const items = res.data || [];
      renderList(items);
      if (infoEl) infoEl.hidden = true;
    } catch (err) {
      if (infoEl) {
        infoEl.hidden = false;
        infoEl.textContent = 'Không thể tải danh sách phiên: ' + (err.message || 'Unknown error');
      }
    }
  }

  function renderList(items) {
    const existingCards = Array.from(listEl.querySelectorAll('.escalation-card'));
    const keepIds = new Set(items.map(i => i.id));

    // Remove cards no longer in the list
    existingCards.forEach(card => {
      const id = parseInt(card.getAttribute('data-id'));
      if (!keepIds.has(id)) card.remove();
    });

    if (!items.length) {
      listEl.innerHTML = '<p class="muted">Không có phiên nào đang chờ nhân viên.</p>';
      return;
    }

    items.forEach(item => {
      let card = listEl.querySelector(`.escalation-card[data-id="${item.id}"]`);
      if (card) {
        updateCard(card, item);
      } else {
        card = buildCard(item);
        listEl.appendChild(card);
      }
    });

    // Sort cards by last activity (if possible) or just keep order
    // For now, let's keep it simple.
  }

  function updateCard(card, session) {
    // 1. Update Badge
    const badge = card.querySelector('.status-badge');
    const cleanStatus = session.status?.toLowerCase() || '';
    badge.className = `status-badge status-${cleanStatus}`;
    badge.textContent = formatBadgeStatus(session.status);
    
    // 2. Update Meta
    const meta = card.querySelector('.escalation-meta');
    if (session.lastMessageAt) {
        meta.innerHTML = `<div><strong>Hoạt động gần nhất:</strong> ${new Date(session.lastMessageAt).toLocaleString()}</div>`;
    }

    // 3. Update Conversation (Only if last activity changed or count changed?)
    // To keep it simple, we reload conversation
    const convo = card.querySelector('.escalation-conversation');
    const lastCount = parseInt(convo.getAttribute('data-count') || '0');
    loadConversation(session, convo, false); // false = silent update

    // 4. Update Buttons state
    const joinBtn = card.querySelector('button.btn-secondary');
    if (session.status === 'STAFF_CHATTING') {
      if (joinBtn) {
        joinBtn.disabled = true;
        joinBtn.textContent = 'Session accepted';
      }
    } else {
      if (joinBtn) {
        joinBtn.disabled = false;
        joinBtn.textContent = 'Accept session';
      }
    }
  }

  function formatBadgeStatus(s) {
    if(s === 'WAITING_STAFF') return 'Waiting Staff';
    if(s === 'STAFF_CHATTING') return 'Staff Chatting';
    return s || 'Unknown';
  }

  function buildCard(session) {
    const card = document.createElement('section');
    card.className = 'card escalation-card';
    card.setAttribute('data-id', session.id);

    const cleanStatus = session.status?.toLowerCase() || '';

    const header = document.createElement('div');
    header.className = 'escalation-header';
    header.innerHTML = `
      <div>
        <h3>Session #${session.id}</h3>
        <p class="muted">Customer: ${session.customerLabel || 'Guest'}</p>
      </div>
      <span class="status-badge status-${cleanStatus}" style="letter-spacing: 0.5px">${formatBadgeStatus(session.status)}</span>
    `;

    const meta = document.createElement('div');
    meta.className = 'escalation-meta';
    meta.innerHTML = `
      ${session.lastMessageAt ? `<div><strong>Last activity:</strong> ${new Date(session.lastMessageAt).toLocaleString()}</div>` : ''}
    `;

    const convo = document.createElement('div');
    convo.className = 'escalation-conversation';
    loadConversation(session, convo);

    const replyForm = document.createElement('form');
    replyForm.className = 'escalation-reply';
    replyForm.innerHTML = `
      <textarea placeholder="Type a reply..." rows="2" required></textarea>
      <div class="reply-actions">
        <button class="btn" type="submit">Reply</button>
        <button class="btn btn-secondary" type="button">Accept session</button>
        <button class="btn btn-danger" type="button">Mark resolved</button>
      </div>
    `;

    const textarea = replyForm.querySelector('textarea');
    const [replyBtn, joinBtn, closeBtn] = replyForm.querySelectorAll('button');

    replyForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const message = textarea.value.trim();
      if (!message) return;
      try {
        await TB.apiFetch(`/api/v1/admin/chat/escalations/${session.id}/reply`, {
          method: 'POST',
          body: JSON.stringify({ message })
        });
        textarea.value = '';
        refresh();
      } catch (err) {
        alert('Gửi phản hồi thất bại: ' + (err.message || 'Unknown error'));
      }
    });

    joinBtn.addEventListener('click', async () => {
      try {
        await TB.apiFetch(`/api/v1/admin/chat/escalations/${session.id}/assign`, { method: 'POST' });
        refresh();
      } catch (err) {
        alert('Tiếp nhận phiên thất bại.');
      }
    });

    closeBtn.addEventListener('click', async () => {
      if (!window.confirm('Đánh dấu phiên này là đã giải quyết?')) return;
      try {
        await TB.apiFetch(`/api/v1/admin/chat/escalations/${session.id}/resolve`, { method: 'POST' });
        refresh();
      } catch (err) {
        alert('Giải quyết phiên thất bại.');
      }
    });

    if (session.status === 'STAFF_CHATTING') {
      joinBtn.disabled = true;
      joinBtn.textContent = 'Session accepted';
    }

    card.append(header, meta, convo, replyForm);
    return card;
  }

  async function loadConversation(session, container, showSpinner = true) {
    if (showSpinner) container.innerHTML = '<p class="muted">Đang tải cuộc hội thoại...</p>';
    const query = session.userId ? `?userId=${encodeURIComponent(session.userId)}` : `?guestId=${encodeURIComponent(session.guestId || '')}`;
    try {
      const res = await TB.apiFetch(`/api/v1/chat/messages${query}`);
      const messages = res.data || [];
      const currentCount = parseInt(container.getAttribute('data-count') || '0');
      
      if (messages.length === currentCount && !showSpinner) return; // No change

      container.innerHTML = '';
      if (!messages.length) {
        container.innerHTML = '<p class="muted">Chưa có tin nhắn.</p>';
      } else {
        messages.forEach(m => container.append(renderMessage(m)));
      }
      container.setAttribute('data-count', messages.length);
      // Auto scroll to bottom
      container.scrollTo({ top: container.scrollHeight, behavior: 'smooth' });
    } catch (err) {
      if (showSpinner) container.innerHTML = `<p class="muted">Không thể tải tin nhắn.</p>`;
    }
  }

  function renderMessage(message) {
    const item = document.createElement('div');
    item.className = 'escalation-message';
    const sender = message.senderType ? message.senderType.toUpperCase() : 'UNKNOWN';
    item.innerHTML = `
      <div class="msg-head">
        <span class="msg-sender">${sender}</span>
        ${message.sentAt ? `<span class="msg-time">${new Date(message.sentAt).toLocaleString()}</span>` : ''}
      </div>
      <div class="msg-body">${escapeHtml(message.message || '')}</div>
    `;
    return item;
  }

  function escapeHtml(input) {
    return String(input || '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }

  reloadBtn?.addEventListener('click', (e) => {
    e.preventDefault();
    refresh();
  });

  refresh();
  // Auto refresh every 4 seconds
  setInterval(refresh, 4000);
})();
