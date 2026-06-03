(() => {
  const token = window.TB?.getToken?.();
  if (!token) return;

  const badge = document.createElement('button');
  badge.id = 'adminNotificationBadge';
  badge.type = 'button';
  badge.className = 'btn';
  badge.innerHTML = '<span class="badge-count">0</span><span class="badge-label">Waiting chats</span>';
  badge.addEventListener('click', () => {
    window.location.href = '/pages/admin/chat-escalations.html';
  });

  const info = document.createElement('div');
  info.id = 'adminNotificationInfo';
  info.hidden = true;

  const controls = document.querySelector('.admin-controls') || document.body;
  controls.appendChild(info);
  controls.appendChild(badge);

  const BACKEND_URL = 'http://localhost:8081';
  const evt = new EventSource(`${BACKEND_URL}/api/v1/admin/chat/notifications/stream?token=${encodeURIComponent(token)}`);
  evt.onmessage = (event) => {
    let payload;
    try {
      payload = JSON.parse(event.data);
    } catch (err) {
      return;
    }
    const count = payload.waitingCount || 1;
    badge.querySelector('.badge-count').textContent = count;
    badge.querySelector('.badge-label').textContent = `${count} waiting`;
    info.textContent = `${payload.customerLabel || 'Guest'}: ${payload.snippet || 'Needs help'}`;
    info.hidden = false;
  };
  evt.onerror = () => {
    badge.classList.add('errored');
  };
})();
