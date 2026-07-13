(() => {
  const params = new URLSearchParams(window.location.search);
  const scheduleId = params.get('scheduleId');
  const forceReadonly = params.get('readonly') === '1';

  const $ = (id) => document.getElementById(id);
  const user = JSON.parse(sessionStorage.getItem('user') || 'null');

  function backendUrl(path) {
    const BACKEND_URL = 'http://localhost:8080';
    const isDev = window.location.port === '3000' || window.location.port === '5500';
    return (isDev && path.startsWith('/')) ? BACKEND_URL + path : path;
  }

  function setBackLink() {
    const role = String(user?.role || '').toUpperCase();
    const link = $('backLink');
    if (!link) return;
    if (role === 'GUIDE') link.href = '../guide/dashboard.html';
    else if (role === 'STAFF' || role === 'ADMIN') link.href = '../staff/manageSchedules.html';
    else link.href = '../index.html';
  }

  const DEFAULT_AVATAR = 'data:image/svg+xml;utf8,' + encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40"><rect width="40" height="40" fill="#cbd5e1"/><circle cx="20" cy="15" r="7" fill="#94a3b8"/><circle cx="20" cy="42" r="16" fill="#94a3b8"/></svg>'
  );

  const DEFAULT_TOUR_IMAGE = 'data:image/svg+xml;utf8,' + encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="280" height="110"><rect width="280" height="110" fill="#e2e8f0"/><text x="140" y="60" font-size="14" text-anchor="middle" fill="#94a3b8" font-family="sans-serif">Tour</text></svg>'
  );

  function avatarOrPlaceholder(url) {
    return url || DEFAULT_AVATAR;
  }

  function roleLabel(role) {
    const r = String(role || '').toUpperCase();
    if (r === 'GUIDE') return 'Hướng dẫn viên';
    if (r === 'STAFF' || r === 'ADMIN') return 'Nhân viên (chỉ xem)';
    return 'Khách hàng';
  }

  function renderMembers(members) {
    $('memberList').innerHTML = members.map(m => `
      <div class="gc-member">
        <img src="${avatarOrPlaceholder(m.avatarUrl)}" alt="">
        <div>
          <div class="name">${m.fullName || 'Người dùng'}${m.role === 'GUIDE' ? ' 🧭' : ''}</div>
          <div class="role">${roleLabel(m.role)}</div>
        </div>
      </div>
    `).join('');
    $('memberCount').textContent = `${members.length} thành viên`;
  }

  function renderScheduleInfo(info) {
    $('tourName').textContent = info.tourName || `Lịch trình #${scheduleId}`;
    $('tourImage').src = info.tourImage ? TB.normalizeImageUrl(info.tourImage) : DEFAULT_TOUR_IMAGE;
    const depart = info.departureDate || '?';
    const ret = info.returnDate || '?';
    $('tourDates').textContent = `${depart} → ${ret}`;
  }

  function appendMessage(msg) {
    const isMe = user && Number(msg.senderId) === Number(user.id);
    const bubbleClass = isMe ? 'me' : `other ${String(msg.senderRole || '').toLowerCase() === 'guide' ? 'guide' : ''}`;
    const box = $('chatBox');
    const el = document.createElement('div');
    el.className = `gc-msg ${bubbleClass}`;
    const time = msg.sentAt ? new Date(msg.sentAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '';
    el.innerHTML = `
      ${!isMe ? `<div class="meta">${msg.senderName || 'Ẩn danh'}${msg.senderRole === 'GUIDE' ? ' · HDV' : ''}</div>` : ''}
      <div>${escapeHtml(msg.message || '')}</div>
      <div class="meta" style="text-align:${isMe ? 'right' : 'left'}; margin-top:4px; margin-bottom:0;">${time}</div>
    `;
    box.appendChild(el);
    box.scrollTop = box.scrollHeight;
  }

  function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }

  function applyReadonly(isReadonly) {
    if (!isReadonly) return;
    $('readonlyBanner').style.display = 'block';
    $('inputArea').style.display = 'none';
  }

  function connectStream() {
    const token = TB.getToken();
    const url = backendUrl(`/api/v1/group-chats/${scheduleId}/stream?token=${encodeURIComponent(token)}`);
    const source = new EventSource(url);
    source.addEventListener('group-chat-message', (event) => {
      try {
        appendMessage(JSON.parse(event.data));
      } catch (e) {
        console.error('Failed to parse group chat message', e);
      }
    });
    source.onerror = () => {
      source.close();
      setTimeout(connectStream, 3000);
    };
  }

  async function init() {
    if (!user?.id) {
      TB.goToLogin('Vui lòng đăng nhập để vào nhóm chat.');
      return;
    }
    if (!scheduleId) {
      $('chatBox').innerHTML = '<div style="text-align:center;color:#ef4444;">Thiếu mã lịch trình (scheduleId).</div>';
      return;
    }

    setBackLink();

    const role = String(user.role || '').toUpperCase();
    const isReadonly = forceReadonly || role === 'STAFF' || role === 'ADMIN';
    applyReadonly(isReadonly);

    try {
      const [infoRes, membersRes, messagesRes] = await Promise.all([
        TB.apiFetch(`/api/v1/group-chats/${scheduleId}/info`),
        TB.apiFetch(`/api/v1/group-chats/${scheduleId}/members`),
        TB.apiFetch(`/api/v1/group-chats/${scheduleId}/messages`),
      ]);

      renderScheduleInfo(infoRes.data || {});
      renderMembers(membersRes.data || []);

      const messages = messagesRes.data || [];
      if (messages.length === 0) {
        $('chatBox').innerHTML = '<div style="text-align:center;color:#94a3b8;">Chưa có tin nhắn nào. Hãy là người đầu tiên bắt đầu cuộc trò chuyện!</div>';
      } else {
        messages.forEach(appendMessage);
      }

      connectStream();
    } catch (err) {
      $('chatBox').innerHTML = `<div style="text-align:center;color:#ef4444;">${err.message || 'Không thể tải nhóm chat.'}</div>`;
      return;
    }

    const form = $('sendForm');
    if (form && !isReadonly) {
      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const input = $('messageInput');
        const content = input.value.trim();
        if (!content) return;
        input.value = '';
        try {
          await TB.apiFetch(`/api/v1/group-chats/${scheduleId}/messages`, {
            method: 'POST',
            body: JSON.stringify({ message: content }),
          });
        } catch (err) {
          alert('Gửi tin nhắn thất bại: ' + (err.message || 'Vui lòng thử lại.'));
        }
      });
    }
  }

  init();
})();
