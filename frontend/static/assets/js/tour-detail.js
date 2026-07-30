(() => {
  const params = new URLSearchParams(window.location.search);
  const id = params.get('id');
  if (!id) {
    document.body.innerHTML = '<div class="container" style="padding:100px; text-align:center;"><h2>Thiếu ID tour để hiển thị chi tiết.</h2><a href="./tours.html" class="btn">Quay lại danh sách</a></div>';
    return;
  }

  const el = (id) => document.getElementById(id);

  function escapeHtml(s) {
    return String(s)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }

  // ── Status helpers ─────────────────────────────────────────────────────────

  const STATUS_META = {
    OPEN: { label: 'Đang nhận đặt', color: '#059669', bg: '#d1fae5', icon: '🟢' },
    BOOKING_CLOSED: { label: 'Đã đóng đặt chỗ', color: '#d97706', bg: '#fef3c7', icon: '🔒' },
    SOLD_OUT: { label: 'Hết chỗ', color: '#dc2626', bg: '#fee2e2', icon: '🔴' },
    PENDING_GUIDE: { label: 'Tạm thời không khả dụng', color: '#b45309', bg: '#fef9c3', icon: '⚠️' },
    SUSPENDED: { label: 'Tạm ngưng', color: '#7c3aed', bg: '#ede9fe', icon: '⏸️' },
    IN_PROGRESS: { label: 'Đang diễn ra', color: '#2563eb', bg: '#dbeafe', icon: '🚀' },
    COMPLETED: { label: 'Đã hoàn thành', color: '#64748b', bg: '#f1f5f9', icon: '✅' },
    CANCELLED: { label: 'Đã hủy', color: '#9ca3af', bg: '#f3f4f6', icon: '❌' },
    CANCELLED_BY_OPERATOR: { label: 'Đã hủy bởi nhà điều hành', color: '#7c3aed', bg: '#ede9fe', icon: '🚫' },
    EXPIRED_NO_BOOKING: { label: 'Đã hết hạn - 0 booking', color: '#6b7280', bg: '#e5e7eb', icon: '👻' },
  };

  function getStatusMeta(status) {
    return STATUS_META[String(status).toUpperCase()] || { label: status || 'Không rõ', color: '#6b7280', bg: '#f3f4f6', icon: '⚪' };
  }

  /**
   * Returns whether booking is currently allowed based on status + deadline.
   * @param {object} s - schedule summary object
   * @returns {{ canBook: boolean, reason: string }}
   */
  function getBookabilityState(s) {
    const status = String(s.status || '').toUpperCase();
    const now = new Date();

    if (status === 'CANCELLED') return { canBook: false, reason: 'Lịch trình đã bị hủy.', isPendingGuide: false, isSuspended: false };
    if (s.isExpired) return { canBook: false, reason: 'Lịch trình này đã khởi hành (Giờ khởi hành đã qua so với giờ hiện tại).', isPendingGuide: false, isSuspended: false };
    if (status === 'CANCELLED_BY_OPERATOR') return { canBook: false, reason: 'Lịch trình đã bị hủy bởi nhà điều hành.', isPendingGuide: false, isSuspended: false };
    if (status === 'COMPLETED') return { canBook: false, reason: 'Tour đã hoàn thành.', isPendingGuide: false, isSuspended: false };
    if (status === 'IN_PROGRESS') return { canBook: false, reason: 'Tour đang diễn ra, không thể đặt thêm.', isPendingGuide: false, isSuspended: false };
    if (status === 'BOOKING_CLOSED') return { canBook: false, reason: 'Hạn đặt tour đã kết thúc.', isPendingGuide: false, isSuspended: false };
    if (status === 'SOLD_OUT') return { canBook: false, reason: 'Tour đã hết chỗ.', isPendingGuide: false, isSuspended: false };
    if (status === 'EXPIRED_NO_BOOKING') return { canBook: false, reason: 'Lịch trình đã hết hạn và không có lịch khởi hành do không đủ khách.', isPendingGuide: false, isSuspended: false };
    // PENDING_GUIDE: departure is < 1h away and no guide assigned — booking is blocked
    if (status === 'PENDING_GUIDE') return {
      canBook: false,
      reason: 'Tour temporarily unavailable. Please contact support.',
      isPendingGuide: true,
      isSuspended: false
    };
    // SUSPENDED: tour temporarily halted by admin
    if (status === 'SUSPENDED') return {
      canBook: false,
      reason: 'Lịch này đang tạm ngưng. Vui lòng chọn ngày khác hoặc liên hệ hỗ trợ.',
      isPendingGuide: false,
      isSuspended: true
    };

    // Additional client-side checks against deadline (backend is authoritative, this is UX only)
    if (s.bookingDeadline) {
      const deadline = toDateObj(s.bookingDeadline);
      if (now >= deadline) return { canBook: false, reason: 'Hạn đặt tour đã kết thúc.', isPendingGuide: false };
    }

    if ((s.availableSlots ?? 1) <= 0) return { canBook: false, reason: 'Tour đã hết chỗ.', isPendingGuide: false, isSuspended: false };

    return { canBook: true, reason: '', isPendingGuide: false, isSuspended: false };
  }

  // ── Date/time parsing helpers ─────────────────────────────────────────────

  const toDateObj = (d) => {
    if (!d) return null;
    if (Array.isArray(d)) {
      if (d.length >= 5) return new Date(d[0], d[1] - 1, d[2], d[3], d[4]);
      return new Date(d[0], d[1] - 1, d[2]);
    }
    return new Date(d);
  };

  const formatDate = (d) => {
    const obj = toDateObj(d);
    return obj ? obj.toLocaleDateString('vi-VN') : '—';
  };

  const formatTime = (t) => {
    if (!t) return '';
    if (Array.isArray(t)) return `${String(t[0]).padStart(2, '0')}:${String(t[1]).padStart(2, '0')}`;
    const d = new Date(t);
    if (!isNaN(d)) return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    // Already a string like "07:00"
    return String(t).substring(0, 5);
  };

  const formatDateTime = (dt) => {
    if (!dt) return '—';
    const d = toDateObj(dt);
    if (!d || isNaN(d)) return String(dt);
    return d.toLocaleDateString('vi-VN') + ' ' + String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0');
  };

  // ── Gallery ──────────────────────────────────────────────────────────────

  function renderGallery(images) {
    const root = el('gallery');
    if (!images || images.length === 0) {
      root.innerHTML = '<img src="https://danangbest.com/vnt_upload/tour/04_2023/banahill_1.jpg" class="main-img" alt="Tour img">';
      return;
    }
    const main = images[0];
    const others = images.slice(1, 3);
    let html = `<img src="${TB.normalizeImageUrl(main)}" class="main-img" alt="Tour image" />`;
    others.forEach(img => {
      html += `<img src="${TB.normalizeImageUrl(img)}" alt="Tour image small" />`;
    });
    root.innerHTML = html;
  }

  // ── Itinerary ─────────────────────────────────────────────────────────────

  async function renderItinerary(tourId) {
    const root = el('itinerary');
    try {
      const res = await TB.apiFetch(`/api/v1/tours/${tourId}/itinerary`);
      const items = res.data;
      if (!items || items.length === 0) {
        root.innerHTML = '<p style="padding: 20px; color: var(--text-faint);">Chưa có thông tin lịch trình chi tiết.</p>';
        return;
      }
      root.innerHTML = items.map((item, idx) => `
        <div class="itinerary-item ${idx === 0 ? 'active' : ''}">
          <div class="itinerary-header" onclick="this.parentElement.classList.toggle('active')">
            <strong>Ngày ${item.dayNumber}: ${escapeHtml(item.title)}</strong>
            <span style="font-size: 0.8rem; opacity: 0.5;">▼</span>
          </div>
          <div class="itinerary-content" style="padding: 25px; line-height: 1.8;">
            ${item.imageUrl ? `<img src="${TB.normalizeImageUrl(item.imageUrl)}" style="width:100%; max-height:300px; object-fit:cover; border-radius:8px; margin-bottom:15px;" alt="${escapeHtml(item.title)}">` : ''}
            <div style="margin-bottom: 15px;">
              ${item.transportation ? `<span style="margin-right: 15px;">🚌 <strong>Di chuyển:</strong> ${escapeHtml(item.transportation)}</span>` : ''}
              ${item.meals ? `<span style="margin-right: 15px;">🍽️ <strong>Bữa ăn:</strong> ${escapeHtml(item.meals)}</span>` : ''}
              ${item.accommodation ? `<span style="margin-right: 15px;">🏨 <strong>Lưu trú:</strong> ${escapeHtml(item.accommodation)}</span>` : ''}
            </div>
            ${item.highlights ? `<div style="margin-bottom: 15px;">📍 <strong>Điểm tham quan:</strong> ${escapeHtml(item.highlights)}</div>` : ''}
            <div style="white-space: pre-line;">${escapeHtml(item.description || 'Đang cập nhật...')}</div>
          </div>
        </div>
      `).join('');
    } catch (e) {
      console.error('Error fetching itinerary:', e);
      root.innerHTML = '<p style="padding: 20px; color: var(--text-faint);">Vui lòng liên hệ để nhận lịch trình chi tiết.</p>';
    }
  }

  // ── Schedule select + booking button ─────────────────────────────────────

  function renderSchedules(list) {
    const root = el('schedules');
    const btn = el('bookNowBtn');
    const statusBadgeWrap = el('scheduleStatusBadge');
    const deadlineInfo = el('scheduleDeadlineInfo');

    if (!list || list.length === 0) {
      root.innerHTML = '<option value="">Liên hệ hotline để xem lịch</option>';
      if (btn) { btn.disabled = true; }
      return;
    }

    if (list.length === 0) {
      root.innerHTML = '<option value="">Hiện chưa có lịch khởi hành phù hợp</option>';
      if (btn) { btn.disabled = true; }
      return;
    }

    root.innerHTML = '<option value="">-- Chọn lịch khởi hành --</option>' + list.map(s => {
      const status = String(s.status || '').toUpperCase();
      const { canBook } = getBookabilityState(s);
      const meta = getStatusMeta(status);
      const dateStr = formatDate(s.startDate);
      const timeStr = formatTime(s.departureTime) || formatTime(s.startDate);
      const slotText = (s.availableSlots ?? 0) > 0 ? `Còn ${s.availableSlots} chỗ` : 'Hết chỗ';

      const expiredText = s.isExpired ? ' (Đã khởi hành)' : '';
      return `<option value="${s.scheduleId}" ${(!canBook && !s.isExpired) ? 'disabled' : ''} data-status="${escapeHtml(status)}">
        ${meta.icon} ${dateStr}${timeStr ? ' lúc ' + timeStr : ''} · ${slotText} · ${meta.label}${expiredText}
      </option>`;
    }).join('');

    // Trigger initial state render
    updateBookingState(list);

    root.onchange = () => updateBookingState(list);
  }

  function updateBookingState(list) {
    const root = el('schedules');
    const btn = el('bookNowBtn');
    const statusBadgeWrap = el('scheduleStatusBadge');
    const deadlineInfo = el('scheduleDeadlineInfo');
    const departureInfo = el('scheduleDepartureInfo');
    const selectedId = root ? parseInt(root.value) : null;

    if (!selectedId || !list) {
      if (btn) { btn.disabled = false; btn.style.opacity = '1'; btn.style.cursor = 'pointer'; }
      if (statusBadgeWrap) statusBadgeWrap.innerHTML = '';
      if (deadlineInfo) deadlineInfo.innerHTML = '';
      if (departureInfo) departureInfo.innerHTML = '';
      return;
    }

    const schedule = list.find(s => s.scheduleId === selectedId);
    if (!schedule) return;

    const bookState = getBookabilityState(schedule);
    const { canBook, reason } = bookState;
    const meta = getStatusMeta(schedule.status);

    // Status badge
    if (statusBadgeWrap) {
      statusBadgeWrap.innerHTML = `
        <span style="
          display: inline-flex; align-items: center; gap: 6px;
          background: ${meta.bg}; color: ${meta.color};
          border: 1px solid ${meta.color}33;
          padding: 5px 14px; border-radius: 20px;
          font-size: 0.8rem; font-weight: 700;
        ">${meta.icon} ${meta.label}</span>`;
    }

    // Deadline info
    if (deadlineInfo) {
      const deadline = schedule.bookingDeadline;
      if (deadline) {
        const deadlineDate = toDateObj(deadline);
        const isPast = deadlineDate && new Date() >= deadlineDate;
        deadlineInfo.innerHTML = `
          <div style="font-size: 0.82rem; color: ${isPast ? '#dc2626' : '#64748b'}; margin-top: 8px;">
            📅 Hạn đặt: <strong>${formatDateTime(deadline)}</strong>
            ${isPast ? ' <span style="color:#dc2626; font-weight:700;">(Đã hết hạn)</span>' : ''}
          </div>`;
      } else {
        deadlineInfo.innerHTML = '';
      }
    }

    // Departure Info
    if (departureInfo) {
      if (schedule.departureTime) {
        const timeStr = formatTime(schedule.departureTime);
        departureInfo.innerHTML = `
          <div style="font-size: 0.82rem; color: #059669; margin-top: 4px; font-weight: 700;">
            ⏰ Giờ khởi hành: <strong>${timeStr}</strong>
          </div>`;
      } else {
        departureInfo.innerHTML = '';
      }
    }

    // ── Expired Schedule special alert banner ────────────────────────────────
    let expiredAlert = document.getElementById('expiredScheduleAlert');
    if (schedule.isExpired) {
      if (!expiredAlert) {
        expiredAlert = document.createElement('div');
        expiredAlert.id = 'expiredScheduleAlert';
        expiredAlert.style.cssText = [
          'background: linear-gradient(135deg, #fee2e2, #fecaca)',
          'border: 2px solid #ef4444',
          'border-radius: 12px',
          'padding: 16px 20px',
          'margin-top: 12px',
          'display: flex',
          'align-items: flex-start',
          'gap: 12px',
          'box-shadow: 0 2px 8px rgba(239,68,68,0.15)'
        ].join(';');
        expiredAlert.innerHTML = `
          <span style="font-size:1.6rem;flex-shrink:0;">⏳</span>
          <div>
            <div style="font-weight:800;color:#991b1b;font-size:0.95rem;margin-bottom:4px;">Đã quá giờ đăng ký</div>
            <div style="color:#7f1d1d;font-size:0.875rem;line-height:1.5;">
              Lịch trình này đã khởi hành (Giờ khởi hành đã qua so với giờ hiện tại). Vui lòng chọn ngày khác.
            </div>
          </div>
        `;
        const anchor = departureInfo || deadlineInfo || statusBadgeWrap;
        if (anchor && anchor.parentNode) {
          anchor.parentNode.insertBefore(expiredAlert, anchor.nextSibling);
        }
      }
      expiredAlert.style.display = 'flex';
    } else {
      if (expiredAlert) expiredAlert.style.display = 'none';
    }

    // ── PENDING_GUIDE special alert banner ────────────────────────────────
    let pgAlert = document.getElementById('pendingGuideAlert');
    if (bookState.isPendingGuide) {
      if (!pgAlert) {
        pgAlert = document.createElement('div');
        pgAlert.id = 'pendingGuideAlert';
        pgAlert.style.cssText = [
          'background: linear-gradient(135deg, #fef9c3, #fef3c7)',
          'border: 2px solid #f59e0b',
          'border-radius: 12px',
          'padding: 16px 20px',
          'margin-top: 12px',
          'display: flex',
          'align-items: flex-start',
          'gap: 12px',
          'box-shadow: 0 2px 8px rgba(245,158,11,0.15)'
        ].join(';');
        pgAlert.innerHTML = `
          <span style="font-size:1.6rem;flex-shrink:0;">⚠️</span>
          <div>
            <div style="font-weight:800;color:#92400e;font-size:0.95rem;margin-bottom:4px;">Tour Temporarily Unavailable</div>
            <div style="color:#78350f;font-size:0.875rem;line-height:1.5;">
              This tour schedule is currently pending guide assignment and cannot be booked.
              Please <a href="../chat.html" style="color:#b45309;font-weight:700;">contact support</a> or check back later.
            </div>
          </div>
        `;
        // Insert alert after the deadline info or status badge
        const anchor = deadlineInfo || statusBadgeWrap;
        if (anchor && anchor.parentNode) {
          anchor.parentNode.insertBefore(pgAlert, anchor.nextSibling);
        }
      }
      pgAlert.style.display = 'flex';
    } else {
      if (pgAlert) pgAlert.style.display = 'none';
    }

    // Button state
    if (btn) {
      if (canBook) {
        btn.disabled = false;
        btn.style.opacity = '1';
        btn.style.cursor = 'pointer';
        btn.style.display = '';
        btn.textContent = 'ĐẶT TOUR NGAY';
      } else {
        btn.disabled = true;
        btn.style.cursor = 'not-allowed';
        if (bookState.isPendingGuide) {
          // Hide the button entirely for PENDING_GUIDE
          btn.style.display = 'none';
        } else {
          btn.style.display = '';
          btn.style.opacity = '0.5';
          btn.textContent = schedule.isExpired ? 'Đã quá giờ đăng ký' : (reason || 'Không thể đặt');
        }
      }
    }
  }

  // ── Book now button ───────────────────────────────────────────────────────

  el('bookNowBtn').onclick = () => {
    const scheduleId = el('schedules').value;
    if (!scheduleId) {
      alert('Vui lòng chọn ngày khởi hành để tiếp tục đặt tour.');
      return;
    }
    const qs = new URLSearchParams();
    qs.set('tourId', String(id));
    qs.set('scheduleId', String(scheduleId));
    window.location.href = `./checkout.html?${qs.toString()}`;
  };

  // ── Main data load ────────────────────────────────────────────────────────

  async function load() {
    const res = await TB.apiFetch(`/api/v1/tours/${encodeURIComponent(id)}`, { method: 'GET' });
    const t = res.data;
    if (!t) throw new Error('Không thể tải dữ liệu tour.');

    el('title').textContent = t.tourName || '';
    if (el('breadcrumb')) el('breadcrumb').textContent = t.tourName || '';
    el('desc').textContent = t.description || '';
    el('price').textContent = t.price ? `${Number(t.price).toLocaleString()}` : 'Liên hệ';
    el('duration').textContent = t.duration ? t.duration + ' Ngày' : 'Liên hệ';
    if (el('departure')) el('departure').textContent = t.startLocation || 'Đà Nẵng';
    if (el('endLocation')) el('endLocation').textContent = t.endLocation || 'Đang cập nhật';
    if (el('transport')) el('transport').textContent = t.transportType || 'Xe du lịch đời mới';
    if (el('tourCode')) el('tourCode').textContent = `DB-${t.id || id}`;

    if (el('suitableAges')) el('suitableAges').textContent = t.suitableAges || 'Mọi lứa tuổi';
    if (el('childPolicy')) el('childPolicy').textContent = t.childPolicy || 'Theo chính sách chung của công ty';
    if (el('whyChooseUs')) el('whyChooseUs').textContent = t.whyChooseUs || '';

    renderGallery(t.imageUrls);
    renderItinerary(t.id);
    const visibleSchedules = (t.schedules || []).filter(s => {
      const status = String(s.status || '').toUpperCase();
      // Hide SUSPENDED, completed, cancelled schedules - only show bookable/visible ones
      return status === 'OPEN' || status === 'IN_PROGRESS' || status === 'PENDING_GUIDE' || status === 'BOOKING_CLOSED' || status === 'SOLD_OUT';
    });

    renderSchedules(visibleSchedules);
  }

  // ── Reviews ───────────────────────────────────────────────────────────────

  async function loadReviews() {
    try {
      const res = await TB.apiFetch(`/api/v1/reviews/tour/${encodeURIComponent(id)}`, { method: 'GET' });
      const reviews = res.data || [];
      const listEl = el('reviewList');
      if (reviews.length === 0) {
        listEl.innerHTML = '<p style="color: var(--text-soft); text-align: center; padding: 20px;">Chưa có đánh giá nào. Hãy là người đầu tiên đánh giá!</p>';
        return;
      }
      listEl.innerHTML = reviews.map(r => `
        <div style="background: white; border: 1px solid var(--border); border-radius: 12px; padding: 20px;">
          <div style="display: flex; justify-content: space-between; margin-bottom: 10px;">
            <strong style="color: var(--primary);">${escapeHtml(r.userName || 'Khách hàng')}</strong>
            <span style="color: #fbbf24;">${'⭐'.repeat(r.rating)}</span>
          </div>
          <p style="color: var(--text-soft); line-height: 1.6; font-size: 0.95rem;">${escapeHtml(r.comment || '')}</p>
          <div style="font-size: 0.8rem; color: var(--text-faint); margin-top: 10px;">${r.createdAt ? new Date(r.createdAt).toLocaleString() : ''}</div>
        </div>
      `).join('');

      const currentUser = sessionStorage.getItem('user') ? JSON.parse(sessionStorage.getItem('user')) : null;
      if (currentUser) {
        const uid = currentUser.id || currentUser.userId;
        const mine = reviews.find(r => Number(r.userId) === Number(uid));
        if (mine) {
          if (el('reviewRating')) el('reviewRating').value = String(mine.rating ?? 5);
          if (el('reviewComment')) el('reviewComment').value = mine.comment || '';
          const submitBtn = el('submitReviewBtn');
          if (submitBtn) submitBtn.textContent = 'Cập nhật đánh giá';
        }
      }
    } catch (e) {
      console.error('Failed to load reviews', e);
      el('reviewList').innerHTML = '<p style="color: var(--price); text-align: center;">Lỗi khi tải đánh giá.</p>';
    }
  }

  // ── FAQs ──────────────────────────────────────────────────────────────────

  async function loadFaqs() {
    try {
      const [tourRes, globalRes] = await Promise.all([
        TB.apiFetch(`/api/v1/faqs/tour/${id}`),
        TB.apiFetch(`/api/v1/faqs/global`)
      ]);

      const tourFaqs = (tourRes && tourRes.data) ? tourRes.data : (Array.isArray(tourRes) ? tourRes : []);
      const globalFaqs = (globalRes && globalRes.data) ? globalRes.data : (Array.isArray(globalRes) ? globalRes : []);
      const list = [...(Array.isArray(tourFaqs) ? tourFaqs : []),
      ...(Array.isArray(globalFaqs) ? globalFaqs : [])];

      const elFaqList = el('tourFaqList');
      if (!list || list.length === 0) {
        elFaqList.innerHTML = '<div style="padding: 20px; text-align: center; color: var(--text-faint);">Chưa có câu hỏi thường gặp cho tour này.</div>';
        return;
      }

      elFaqList.innerHTML = list.map(f => `
        <div class="faq-accordion" style="border-bottom: 1px solid var(--border);">
          <div style="padding: 15px 20px; font-weight: 700; color: var(--primary); cursor: pointer; display: flex; justify-content: space-between;"
               onclick="this.nextElementSibling.style.display = this.nextElementSibling.style.display === 'block' ? 'none' : 'block'">
            <span>${escapeHtml(f.question)}</span>
            <span>▼</span>
          </div>
          <div style="padding: 0 20px 15px; color: var(--text-soft); font-size: 0.95rem; line-height: 1.6; display: none;">${escapeHtml(f.answer)}</div>
        </div>
      `).join('');
    } catch (e) {
      console.error('FAQ load error:', e);
      el('tourFaqList').innerHTML = '<div style="padding: 20px; text-align: center; color: var(--text-faint);">Không thể tải câu hỏi.</div>';
    }
  }

  // ── Bootstrap ─────────────────────────────────────────────────────────────

  load().then(() => {
    loadReviews();
    loadFaqs();
  }).catch(err => {
    console.error(err);
    alert('Lỗi khi tải thông tin tour: ' + err.message);
  });

  const user = sessionStorage.getItem('user') ? JSON.parse(sessionStorage.getItem('user')) : null;
  const navRight = el('navRight');
  if (navRight) {
    if (user) {
      navRight.innerHTML = `
        <div style="display: flex; align-items: center; gap: 20px;">
          <span style="font-weight: 700; color: var(--primary); font-size: 0.9rem;">Chào, ${user.fullName || 'Bạn'}</span>
          ${String(user.role || '').toUpperCase() === 'CUSTOMER' ? '<a class="btn btn-secondary" href="../user/personal-info.html" style="padding: 0 18px; min-height: 40px; height: 40px; font-size: 0.8rem; border-radius: 10px;">Thông tin cá nhân</a>' : ''}
          <button class="btn btn-secondary" id="logoutBtn" style="padding: 0 20px; min-height: 40px; height: 40px; font-size: 0.8rem; border-radius: 10px;">Đăng xuất</button>
        </div>`;
      const btn = el('logoutBtn');
      if (btn) btn.onclick = () => { sessionStorage.clear(); location.reload(); };
    } else {
      navRight.innerHTML = `
        <div style="display: flex; align-items: center; gap: 15px;">
           <a href="./auth/login.html" style="font-weight: 800; color: var(--text-soft); font-size: 0.9rem;">Đăng nhập</a>
           <a href="./auth/register.html" class="btn" style="padding: 0 25px; min-height: 40px; height: 40px; font-size: 0.8rem; border-radius: 10px;">ĐĂNG KÝ</a>
        </div>`;
    }
  }

  const formContainer = el('reviewFormContainer');
  const loginPrompt = el('loginPromptReview');
  if (loginPrompt) loginPrompt.style.display = 'none';
  if (formContainer) {
    formContainer.innerHTML = `
      <div style="background: #f0f9ff; border: 1px solid #bae6fd; border-radius: 12px; padding: 20px; text-align: center; color: #0369a1;">
        Bạn chỉ có thể viết đánh giá sau khi đã hoàn tất chuyến tour này.
        Vui lòng vào mục <a href="../user/history.html" style="color: var(--primary); font-weight: 700;">Lịch sử đặt tour</a> để gửi đánh giá.
      </div>
    `;
    formContainer.style.display = 'block';
  }
})();
