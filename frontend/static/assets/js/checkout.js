/**
 * checkout.js — Luồng đặt tour với dynamic passenger forms & smart pricing
 * Adult = giá gốc | Child = giá gốc x 75%
 */
(async () => {
  const params     = new URLSearchParams(window.location.search);
  const tourId     = params.get('tourId');
  const scheduleId = params.get('scheduleId');

  if (params.get('cancel') === 'true') {
    alert('Bạn đã hủy thanh toán. Quá trình đặt tour chưa được hoàn tất.');
    window.location.href = './tours.html';
    return;
  }

  if (!tourId || !scheduleId) {
    alert('Thông tin không hợp lệ!');
    window.location.href = './tours.html';
    return;
  }

  const user = sessionStorage.getItem('user') ? JSON.parse(sessionStorage.getItem('user')) : null;
  if (!user) {
    TB.goToLogin('Vui lòng đăng nhập để tiếp tục đặt tour.');
    return;
  }

  // ─── UI element refs ──────────────────────────────────────────────────────────
  const loading            = document.getElementById('checkoutLoading');
  const content            = document.getElementById('checkoutContent');
  const adultInput         = document.getElementById('adultCount');
  const childInput         = document.getElementById('childCount');
  const passengersContainer = document.getElementById('passengersContainer');
  const summaryTourName    = document.getElementById('summaryTourName');
  const summaryDate        = document.getElementById('summaryDate');
  const summaryUnitPrice   = document.getElementById('summaryUnitPrice');
  const summaryAdults      = document.getElementById('summaryAdults');
  const summaryChildren    = document.getElementById('summaryChildren');
  const summaryTotalPrice  = document.getElementById('summaryTotalPrice');
  const confirmBtn         = document.getElementById('confirmBtn');

  // ─── State ────────────────────────────────────────────────────────────────────
  let currentPrice    = 0;
  let appliedDiscount = 0;
  let appliedVoucherCode = '';

  // ─── Load user info ───────────────────────────────────────────────────────────
  document.getElementById('custName').value  = user.fullName || '';
  document.getElementById('custEmail').value = user.email    || '';

  // ─── Fetch tour & schedule ────────────────────────────────────────────────────
  try {
    const [tourRes, scheduleRes] = await Promise.all([
      TB.apiFetch(`/api/v1/tours/${tourId}`),
      TB.apiFetch(`/api/v1/tours/schedules/${scheduleId}`)
    ]);

    const tourData     = tourRes.data;
    const scheduleData = scheduleRes.data;
    currentPrice       = tourData.price;

    summaryTourName.textContent = tourData.tourName;
    const sd = new Date(scheduleData.startDate);
    summaryDate.textContent = `${sd.toLocaleDateString('vi-VN')} lúc ${String(sd.getHours()).padStart(2,'0')}:${String(sd.getMinutes()).padStart(2,'0')}`;
    summaryUnitPrice.textContent = currentPrice.toLocaleString('vi-VN') + ' đ/người lớn';

    renderPassengerForms();
    updateTotals();

    loading.style.display = 'none';
    content.style.display = 'grid';
  } catch (err) {
    console.error(err);
    alert('Không thể tải thông tin tour. Vui lòng thử lại.');
    window.location.href = './tours.html';
  }

  // ─── Pricing ──────────────────────────────────────────────────────────────────
  function updateTotals() {
    const adults   = getAdultCount();
    const children = getChildCount();

    summaryAdults.textContent   = adults + ' người';
    summaryChildren.textContent = children + ' trẻ em';

    const adultTotal = adults   * currentPrice;
    const childTotal = children * currentPrice * 0.75;
    const baseTotal  = adultTotal + childTotal;

    if (appliedDiscount > 0) {
      document.getElementById('discountItem').style.display = 'flex';
      document.getElementById('summaryDiscountAmount').textContent = '-' + appliedDiscount.toLocaleString('vi-VN') + ' đ';
      document.getElementById('summaryVoucherCode').textContent    = appliedVoucherCode;
    } else {
      document.getElementById('discountItem').style.display = 'none';
    }

    const finalTotal = Math.max(0, baseTotal - appliedDiscount);
    summaryTotalPrice.textContent = finalTotal.toLocaleString('vi-VN');
  }

  function getAdultCount()   { return Math.max(1, parseInt(adultInput.value)  || 1); }
  function getChildCount()   { return Math.max(0, parseInt(childInput.value)  || 0); }
  function getTotalPeople()  { return getAdultCount() + getChildCount(); }

  // ─── Adult counter ────────────────────────────────────────────────────────────
  document.getElementById('plusAdultBtn').onclick = () => {
    adultInput.value = getAdultCount() + 1;
    renderPassengerForms();
    updateTotals();
    appliedDiscount = 0; appliedVoucherCode = ''; resetVoucherUI();
  };
  document.getElementById('minusAdultBtn').onclick = () => {
    if (getAdultCount() > 1) {
      adultInput.value = getAdultCount() - 1;
      renderPassengerForms();
      updateTotals();
      appliedDiscount = 0; appliedVoucherCode = ''; resetVoucherUI();
    }
  };
  adultInput.oninput = () => { renderPassengerForms(); updateTotals(); };

  // ─── Child counter ────────────────────────────────────────────────────────────
  document.getElementById('plusChildBtn').onclick = () => {
    childInput.value = getChildCount() + 1;
    renderPassengerForms();
    updateTotals();
    appliedDiscount = 0; appliedVoucherCode = ''; resetVoucherUI();
  };
  document.getElementById('minusChildBtn').onclick = () => {
    if (getChildCount() > 0) {
      childInput.value = getChildCount() - 1;
      renderPassengerForms();
      updateTotals();
      appliedDiscount = 0; appliedVoucherCode = ''; resetVoucherUI();
    }
  };
  childInput.oninput = () => { renderPassengerForms(); updateTotals(); };

  // ─── Dynamic passenger form renderer ─────────────────────────────────────────
  function renderPassengerForms() {
    const adults   = getAdultCount();
    const children = getChildCount();
    const total    = adults + children;

    // Preserve existing values before re-render
    const oldData = collectPassengerData();

    passengersContainer.innerHTML = '';

    for (let i = 0; i < total; i++) {
      const isChild = i >= adults;
      const type    = isChild ? 'CHILD' : 'ADULT';
      const num     = isChild ? (i - adults + 1) : (i + 1);
      const icon    = isChild ? '👦' : '🧑';
      const label   = isChild ? `Trẻ em ${num}` : `Người lớn ${num}`;
      const badge   = isChild ? 'Trẻ em · 75% giá' : 'Người lớn · 100% giá';

      const old = oldData[i] || {};

      const card = document.createElement('div');
      card.className = isChild ? 'passenger-card child-card' : 'passenger-card';

      card.innerHTML = `
        <div class="passenger-header">
          <span class="passenger-label">${icon} ${label}</span>
          <span class="passenger-badge">${badge}</span>
        </div>
        <div class="passenger-grid">
          <div class="form-group full-width">
            <label>Họ và tên <span style="color:#e53e3e">*</span></label>
            <input class="passenger-input" type="text" id="p_name_${i}"
              placeholder="Nhập họ tên đầy đủ..." required
              value="${escHtml(old.fullName || '')}">
          </div>
          <div class="form-group">
            <label>Ngày sinh <span style="color:#e53e3e">*</span></label>
            <input class="passenger-input" type="date" id="p_dob_${i}" required
              value="${escHtml(old.dateOfBirth || '')}">
          </div>
          <div class="form-group">
            <label>Số CCCD / Passport</label>
            <input class="passenger-input" type="text" id="p_id_${i}"
              placeholder="Số giấy tờ tùy thân..."
              value="${escHtml(old.idNumber || '')}">
          </div>
          <input type="hidden" id="p_type_${i}" value="${type}">
        </div>
      `;

      passengersContainer.appendChild(card);
    }
  }

  function escHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/"/g, '&quot;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }

  function collectPassengerData() {
    const total = getTotalPeople();
    const list  = [];
    for (let i = 0; i < total; i++) {
      const nameEl = document.getElementById(`p_name_${i}`);
      if (!nameEl) break;
      list.push({
        fullName:      nameEl?.value?.trim()                            || '',
        dateOfBirth:   document.getElementById(`p_dob_${i}`)?.value   || '',
        idNumber:      document.getElementById(`p_id_${i}`)?.value?.trim() || '',
        passengerType: document.getElementById(`p_type_${i}`)?.value  || 'ADULT',
      });
    }
    return list;
  }

  function validatePassengers() {
    const data = collectPassengerData();
    for (let i = 0; i < data.length; i++) {
      if (!data[i].fullName) {
        alert(`Vui lòng nhập Họ và tên cho Hành khách ${i + 1}.`);
        document.getElementById(`p_name_${i}`)?.focus();
        return null;
      }
      if (!data[i].dateOfBirth) {
        alert(`Vui lòng nhập Ngày sinh cho Hành khách ${i + 1}.`);
        document.getElementById(`p_dob_${i}`)?.focus();
        return null;
      }
    }
    return data;
  }

  function resetVoucherUI() {
    document.getElementById('voucherMsg').textContent = '';
    document.getElementById('voucherCode').value      = '';
  }

  // ─── Voucher ──────────────────────────────────────────────────────────────────
  const voucherInput    = document.getElementById('voucherCode');
  const applyVoucherBtn = document.getElementById('applyVoucherBtn');
  const voucherMsg      = document.getElementById('voucherMsg');

  applyVoucherBtn.onclick = async () => {
    const code = voucherInput.value.trim().toUpperCase();
    if (!code) return;

    applyVoucherBtn.disabled    = true;
    applyVoucherBtn.textContent = '...';

    try {
      const adults   = getAdultCount();
      const children = getChildCount();
      const baseTotal = adults * currentPrice + children * currentPrice * 0.75;

      const res = await TB.apiFetch('/api/v1/bookings/apply-voucher', {
        method: 'POST',
        body: JSON.stringify({ voucherCode: code, currentTotal: baseTotal })
      });

      if (res.data.isValid) {
        appliedDiscount    = res.data.discountAmount;
        appliedVoucherCode = code;
        voucherMsg.style.color = '#4caf50';
        voucherMsg.textContent = res.data.message;
      } else {
        appliedDiscount    = 0;
        appliedVoucherCode = '';
        voucherMsg.style.color = '#f44336';
        voucherMsg.textContent = res.data.message;
      }
      updateTotals();
    } catch (err) {
      console.error(err);
      voucherMsg.style.color = '#f44336';
      voucherMsg.textContent = 'Lỗi hệ thống khi kiểm tra mã.';
    } finally {
      applyVoucherBtn.disabled    = false;
      applyVoucherBtn.textContent = 'Áp dụng';
    }
  };

  // ─── Payment method toggle ────────────────────────────────────────────────────
  let selectedMethod = 'PAYOS';
  document.querySelectorAll('.method-card').forEach(card => {
    card.onclick = () => {
      document.querySelectorAll('.method-card').forEach(c => c.classList.remove('active'));
      card.classList.add('active');
      selectedMethod = card.dataset.method;
    };
  });

  // ─── Confirm booking ──────────────────────────────────────────────────────────
  confirmBtn.onclick = async () => {
    const passengers = validatePassengers();
    if (!passengers) return;

    confirmBtn.disabled    = true;
    confirmBtn.textContent = 'ĐANG XỬ LÝ...';

    try {
      // Build request with adultCount / childCount / passengers list
      const bookingReq = {
        userId:        user.id,
        scheduleId:    parseInt(scheduleId),
        adultCount:    getAdultCount(),
        childCount:    getChildCount(),
        discountCode:  appliedVoucherCode || null,
        passengers:    passengers.map(p => ({
          fullName:      p.fullName,
          dateOfBirth:   p.dateOfBirth,   // "YYYY-MM-DD"
          idNumber:      p.idNumber || null,
          passengerType: p.passengerType
        }))
      };

      const bookingRes = await TB.apiFetch('/api/v1/bookings', {
        method: 'POST',
        body: JSON.stringify(bookingReq)
      });

      const bookingId = bookingRes.data.id;

      if (selectedMethod === 'PAYOS') {
        const paymentRes = await TB.apiFetch('/api/v1/payments/payos/create', {
          method: 'POST',
          body: JSON.stringify({ bookingId, paymentMethod: 'PAYOS' })
        });

        if (paymentRes.data?.checkoutUrl) {
          window.location.href = paymentRes.data.checkoutUrl;
        } else {
          alert('Không thể tạo liên kết thanh toán. Vui lòng liên hệ hỗ trợ.');
          confirmBtn.disabled    = false;
          confirmBtn.textContent = 'XÁC NHẬN ĐẶT TOUR';
        }
      } else {
        alert('Đặt tour thành công! Vui lòng đến văn phòng Danangbest để hoàn tất thanh toán.');
        window.location.href = './index.html';
      }
    } catch (err) {
      console.error(err);
      alert('Đã xảy ra lỗi: ' + err.message);
      confirmBtn.disabled    = false;
      confirmBtn.textContent = 'XÁC NHẬN ĐẶT TOUR';
    }
  };
})();
