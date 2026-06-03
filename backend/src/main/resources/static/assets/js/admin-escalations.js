(() => {
  const listEl = document.getElementById('escalationList');
  const reloadBtn = document.getElementById('reloadEscalations');
  const infoEl = document.getElementById('escalationInfo');

  async function refresh() {
    try {
      const res = await TB.apiFetch('/api/v1/admin/chat/escalations');
      renderList(res.data || []);
      if (infoEl) infoEl.hidden = true;
    } catch (err) {
      if (infoEl) {
        infoEl.hidden = false;
        infoEl.textContent = 'Unable to load sessions: ' + (err.message || 'Unknown error');
      } else {
        console.error('Unable to load sessions:', err);
      }
    }
  }

  function renderList(items) {
    listEl.innerHTML = '';
    if (!items.length) {
      listEl.innerHTML = '<p class="muted">No sessions waiting for staff.</p>';
      return;
    }
    items.forEach(item => listEl.appendChild(buildCard(item)));
  }

  function buildCard(session) {
    const card = document.createElement('section');
    card.className = 'card escalation-card';

    function formatBadgeStatus(s) {
      if(s === 'WAITING_STAFF') return 'Waiting Staff';
      if(s === 'STAFF_CHATTING') return 'Staff Chatting';
      return s;
    }
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
        alert('Failed to send reply: ' + (err.message || 'Unknown error'));
      }
    });

    joinBtn.addEventListener('click', async () => {
      try {
        await TB.apiFetch(`/api/v1/admin/chat/escalations/${session.id}/assign`, { method: 'POST' });
        refresh();
      } catch (err) {
        alert('Failed to accept session.');
      }
    });

    closeBtn.addEventListener('click', async () => {
      if (!window.confirm('Mark this session as resolved?')) return;
      try {
        await TB.apiFetch(`/api/v1/admin/chat/escalations/${session.id}/resolve`, { method: 'POST' });
        refresh();
      } catch (err) {
        alert('Failed to resolve session.');
      }
    });

    if (session.status === 'STAFF_CHATTING') {
      joinBtn.disabled = true;
      joinBtn.textContent = 'Session accepted';
    }

    card.append(header, meta, convo, replyForm);
    return card;
  }

  async function loadConversation(session, container) {
    container.innerHTML = '<p class="muted">Loading conversation...</p>';
    const query = session.userId ? `?userId=${encodeURIComponent(session.userId)}` : `?guestId=${encodeURIComponent(session.guestId || '')}`;
    try {
      const res = await TB.apiFetch(`/api/v1/chat/messages${query}`);
      container.innerHTML = '';
      const messages = res.data || [];
      if (!messages.length) {
        container.innerHTML = '<p class="muted">No messages yet.</p>';
        return;
      }
      messages.forEach(m => container.append(renderMessage(m)));
    } catch (err) {
      container.innerHTML = `<p class="muted">Unable to load messages.</p>`;
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
})();
