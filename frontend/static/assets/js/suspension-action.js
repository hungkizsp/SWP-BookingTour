// suspension-action.js
document.addEventListener('DOMContentLoaded', async () => {
  const userStr = sessionStorage.getItem('user');
  if (!userStr) return;
  const user = JSON.parse(userStr);
  const role = String(user.role || '').toUpperCase();

  // Only for customers
  if (role !== 'CUSTOMER' && role !== 'USER') return;

  try {
    const res = await TB.apiFetch('/api/v1/bookings/pending-suspension-actions');
    const actions = res.data || [];
    
    if (actions.length > 0) {
      // Lấy first action
      const firstAction = actions[0];
      
      // Kiểm tra URL hiện tại xem có đang xử lý booking này qua query params không
      const urlParams = new URLSearchParams(window.location.search);
      const isRescheduling = String(urlParams.get('reschedule')) === String(firstAction.bookingId);
      const isRefunding = String(urlParams.get('refund')) === String(firstAction.bookingId);
      
      if (!isRescheduling && !isRefunding) {
        // Chỉ hiển thị modal nếu chưa chọn xử lý
        showSuspensionActionModal(firstAction);
      }
    }
  } catch (err) {
    console.error('Error fetching suspension actions', err);
  }
});

function showSuspensionActionModal(action) {
  // Inject modal CSS if not exists
  if (!document.getElementById('suspension-modal-style')) {
    const style = document.createElement('style');
    style.id = 'suspension-modal-style';
    style.textContent = `
      .suspension-modal-overlay {
        position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
        background: rgba(0, 0, 0, 0.5); z-index: 99999;
        display: flex; align-items: center; justify-content: center;
        backdrop-filter: blur(4px);
      }
      .suspension-modal-box {
        background: #fff; width: 90%; max-width: 500px;
        border-radius: 16px; padding: 24px; box-shadow: 0 10px 30px rgba(0,0,0,0.2);
      }
      .suspension-modal-title {
        font-size: 1.2rem; font-weight: 700; color: #dc2626; margin-bottom: 12px;
        display: flex; align-items: center; gap: 8px;
      }
      .suspension-modal-content {
        font-size: 0.95rem; color: #4b5563; margin-bottom: 20px; line-height: 1.6;
      }
      .suspension-modal-actions {
        display: flex; gap: 12px; flex-direction: column;
      }
      .suspension-btn {
        width: 100%; padding: 12px; border-radius: 8px; font-weight: 600; cursor: pointer; border: none; font-size: 0.95rem;
      }
      .suspension-btn-primary { background: #064e3b; color: #fff; }
      .suspension-btn-danger { background: #dc2626; color: #fff; }
      .suspension-btn-secondary { background: #e5e7eb; color: #374151; }
    `;
    document.head.appendChild(style);
  }

  const overlay = document.createElement('div');
  overlay.className = 'suspension-modal-overlay';
  overlay.id = 'suspensionActionModal';

  let rescheduleHtml = '';
  if (action.canReschedule) {
    rescheduleHtml = `<button class="suspension-btn suspension-btn-primary" id="wa-reschedule">Đổi ngày khởi hành khác</button>`;
  } else {
    rescheduleHtml = `<div style="font-size: 0.85rem; color: #9ca3af; margin-bottom: 10px; font-style: italic;">
      Tour dự kiến mở lại sau thời gian dài, hiện chưa có lịch khởi hành thay thế phù hợp trong thời gian gần.
    </div>`;
  }

  // Determine label and icon based on suspensionReasonType
  let typeIcon = 'ℹ️';
  let typeLabel = 'Lý do khác';
  switch (action.suspensionReasonType) {
    case 'WEATHER':
      typeIcon = '🌧️';
      typeLabel = 'Thời tiết';
      break;
    case 'POLICY':
      typeIcon = '📋';
      typeLabel = 'Chính sách';
      break;
    case 'LOCATION_UNAVAILABLE':
      typeIcon = '📍';
      typeLabel = 'Địa điểm không khả dụng';
      break;
    case 'SAFETY':
      typeIcon = '⚠️';
      typeLabel = 'An toàn';
      break;
  }

  overlay.innerHTML = `
    <div class="suspension-modal-box">
      <div class="suspension-modal-title">
        <span>⚠️</span> Thông báo Quan Trọng
      </div>
      <div class="suspension-modal-content">
        <p>Tour <strong>${action.tourName}</strong> (Dự kiến đi ngày ${action.departureDate}) của bạn tạm thời bị hoãn.</p>
        <div style="background: #fef2f2; padding: 12px; border-radius: 8px; border-left: 4px solid #dc2626; margin: 12px 0;">
          <strong>Lý do (${typeIcon} ${typeLabel}):</strong> ${action.suspensionReason || 'Điều kiện khách quan'}
        </div>
        <p>Mong bạn thông cảm và vui lòng chọn hướng xử lý bên dưới.</p>
      </div>
      <div class="suspension-modal-actions">
        ${rescheduleHtml}
        <button class="suspension-btn suspension-btn-danger" id="wa-refund">Yêu cầu hoàn tiền</button>
        <button class="suspension-btn suspension-btn-secondary" id="wa-later">Để sau</button>
      </div>
    </div>
  `;

  document.body.appendChild(overlay);

  function closeSuspensionModal() {
    const modal = document.getElementById('suspensionActionModal');
    if (modal) modal.remove();
  }

  // Handlers
  if (action.canReschedule) {
    document.getElementById('wa-reschedule').onclick = () => {
      // Redirect to history page with param so we can auto-open reschedule modal
      if (window.location.pathname.includes('history.html')) {
        closeSuspensionModal();
        if (typeof openRescheduleModal === 'function') {
          openRescheduleModal(action.bookingId, action.tourName, action.departureDate);
        } else {
          window.location.href = window.location.pathname + '?reschedule=' + action.bookingId;
        }
      } else {
        window.location.href = '/user/history.html?reschedule=' + action.bookingId;
      }
    };
  }

  document.getElementById('wa-refund').onclick = () => {
    if (window.location.pathname.includes('history.html')) {
      closeSuspensionModal();
      if (typeof openRefundModal === 'function') {
        openRefundModal(action.bookingId);
      } else {
        window.location.href = window.location.pathname + '?refund=' + action.bookingId;
      }
    } else {
      window.location.href = '/user/history.html?refund=' + action.bookingId;
    }
  };

  document.getElementById('wa-later').onclick = () => {
    overlay.remove();
  };
}
