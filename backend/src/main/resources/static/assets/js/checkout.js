/**
 * checkout.js — Luồng đặt tour với dynamic passenger forms & smart pricing
 * ADULT = 100% | CHILD = 75% | INFANT = 10% (không chiếm chỗ)
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
  const loading             = document.getElementById('checkoutLoading');
  const content             = document.getElementById('checkoutContent');
  const adultInput          = document.getElementById('adultCount');
  const childInput          = document.getElementById('childCount');
  const infantInput         = document.getElementById('infantCount');
  const passengersContainer = document.getElementById('passengersContainer');
  const summaryTourName     = document.getElementById('summaryTourName');
  const summaryDate         = document.getElementById('summaryDate');
  const summaryUnitPrice    = document.getElementById('summaryUnitPrice');
  const summaryAdults       = document.getElementById('summaryAdults');
  const summaryChildren     = document.getElementById('summaryChildren');
  const summaryInfants      = document.getElementById('summaryInfants');
  const summaryAdultPrice   = document.getElementById('summaryAdultPrice');
  const summaryChildPrice   = document.getElementById('summaryChildPrice');
  const summaryInfantPrice  = document.getElementById('summaryInfantPrice');
  const summaryChildPriceRow  = document.getElementById('summaryChildPriceRow');
  const summaryInfantPriceRow = document.getElementById('summaryInfantPriceRow');
  const summaryTotalPrice   = document.getElementById('summaryTotalPrice');
  const confirmBtn          = document.getElementById('confirmBtn');

  // ─── State ────────────────────────────────────────────────────────────────────
  let currentPrice    = 0;
  let tourStartDate   = null; // Date object (local midnight)
  let appliedDiscount = 0;
  let appliedVoucherCode = '';

  const PASSENGER_META = {
    ADULT:  { icon: '🧑', label: 'Người lớn', badge: 'Người lớn · 100% giá', cardClass: '' },
    CHILD:  { icon: '👦', label: 'Trẻ em',    badge: 'Trẻ em · 75% giá',    cardClass: 'child-card' },
    INFANT: { icon: '👶', label: 'Em bé',     badge: 'Em bé · 10% giá',     cardClass: 'infant-card' },
  };

  // ─── Date helpers ───────────────────────────────────────────────────────────────
  function parseTourStartDate(raw) {
    const datePart = String(raw).split('T')[0];
    const [y, m, d] = datePart.split('-').map(Number);
    return new Date(y, m - 1, d);
  }

  function formatDateInput(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  function addYears(date, years) {
    const d = new Date(date.getTime());
    d.setFullYear(d.getFullYear() + years);
    return d;
  }

  function getDobBounds(type) {
    if (!tourStartDate) return { min: '', max: '' };

    const start = tourStartDate;
    switch (type) {
      case 'INFANT':
        return { min: formatDateInput(addYears(start, -2)), max: formatDateInput(start) };
      case 'CHILD':
        return { min: formatDateInput(addYears(start, -12)), max: formatDateInput(addYears(start, -2)) };
      case 'ADULT':
      default:
        return { min: '', max: formatDateInput(addYears(start, -12)) };
    }
  }

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
    tourStartDate      = parseTourStartDate(scheduleData.startDate);

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

  // ─── Passenger counts ─────────────────────────────────────────────────────────
  function getAdultCount()  { return Math.max(1, parseInt(adultInput.value)  || 1); }
  function getChildCount()  { return Math.max(0, parseInt(childInput.value)  || 0); }
  function getInfantCount() { return Math.max(0, parseInt(infantInput.value) || 0); }
  function getTotalPeople() { return getAdultCount() + getChildCount() + getInfantCount(); }

  function buildPassengerSlots() {
    const slots = [];
    const adults   = getAdultCount();
    const children = getChildCount();
    const infants  = getInfantCount();

    for (let i = 0; i < adults; i++)   slots.push({ type: 'ADULT',  num: i + 1 });
    for (let i = 0; i < children; i++) slots.push({ type: 'CHILD',  num: i + 1 });
    for (let i = 0; i < infants; i++)  slots.push({ type: 'INFANT', num: i + 1 });
    return slots;
  }

  // ─── Pricing ──────────────────────────────────────────────────────────────────
  function updateTotals() {
    const adults   = getAdultCount();
    const children = getChildCount();
    const infants  = getInfantCount();

    summaryAdults.textContent   = adults   + ' người';
    summaryChildren.textContent = children + ' trẻ em';
    summaryInfants.textContent  = infants  + ' em bé';

    const adultTotal  = adults   * currentPrice;
    const childTotal  = children * currentPrice * 0.75;
    const infantTotal = infants  * currentPrice * 0.10;
    const baseTotal   = adultTotal + childTotal + infantTotal;

    summaryAdultPrice.textContent = adultTotal.toLocaleString('vi-VN') + ' đ';
    summaryChildPrice.textContent = childTotal.toLocaleString('vi-VN') + ' đ';
    summaryInfantPrice.textContent = infantTotal.toLocaleString('vi-VN') + ' đ';
    summaryChildPriceRow.style.display  = children > 0 ? 'flex' : 'none';
    summaryInfantPriceRow.style.display = infants  > 0 ? 'flex' : 'none';

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

  function onCountChange() {
    renderPassengerForms();
    updateTotals();
    appliedDiscount = 0;
    appliedVoucherCode = '';
    resetVoucherUI();
  }

  // ─── Counters ─────────────────────────────────────────────────────────────────
  document.getElementById('plusAdultBtn').onclick = () => {
    adultInput.value = getAdultCount() + 1;
    onCountChange();
  };
  document.getElementById('minusAdultBtn').onclick = () => {
    if (getAdultCount() > 1) {
      adultInput.value = getAdultCount() - 1;
      onCountChange();
    }
  };
  adultInput.oninput = onCountChange;

  document.getElementById('plusChildBtn').onclick = () => {
    childInput.value = getChildCount() + 1;
    onCountChange();
  };
  document.getElementById('minusChildBtn').onclick = () => {
    if (getChildCount() > 0) {
      childInput.value = getChildCount() - 1;
      onCountChange();
    }
  };
  childInput.oninput = onCountChange;

  document.getElementById('plusInfantBtn').onclick = () => {
    infantInput.value = getInfantCount() + 1;
    onCountChange();
  };
  document.getElementById('minusInfantBtn').onclick = () => {
    if (getInfantCount() > 0) {
      infantInput.value = getInfantCount() - 1;
      onCountChange();
    }
  };
  infantInput.oninput = onCountChange;

  // ─── Dynamic passenger form renderer ─────────────────────────────────────────
  function renderPassengerForms() {
    const slots = buildPassengerSlots();
    const oldMap = mapPassengerDataByType(collectPassengerData());

    passengersContainer.innerHTML = '';

    slots.forEach((slot, i) => {
      const meta  = PASSENGER_META[slot.type];
      const label = `${meta.label} ${slot.num}`;
      const old   = oldMap[`${slot.type}_${slot.num - 1}`] || {};
      const bounds = getDobBounds(slot.type);

      const idPlaceholder = slot.type === 'ADULT'
        ? 'Số định danh (CCCD/Passport)'
        : 'Số giấy khai sinh hoặc hộ chiếu (Không bắt buộc)';

      const idLabel = slot.type === 'ADULT'
        ? 'Số CCCD / Passport <span style="color:#e53e3e">*</span>'
        : 'Số định danh (CCCD/Passport)';

      const card = document.createElement('div');
      card.className = `passenger-card ${meta.cardClass}`.trim();

      card.innerHTML = `
        <div class="passenger-header">
          <span class="passenger-label">${meta.icon} ${label}</span>
          <span class="passenger-badge">${meta.badge}</span>
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
              min="${bounds.min}" max="${bounds.max}"
              value="${escHtml(old.dateOfBirth || '')}">
          </div>
          <div class="form-group">
            <label>${idLabel}</label>
            <input class="passenger-input" type="text" id="p_id_${i}"
              placeholder="${escHtml(idPlaceholder)}"
              value="${escHtml(old.idNumber || '')}">
          </div>
          <input type="hidden" id="p_type_${i}" value="${slot.type}">
        </div>
      `;

      passengersContainer.appendChild(card);
    });
  }

  function escHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/"/g, '&quot;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }

  function mapPassengerDataByType(data) {
    const counters = { ADULT: 0, CHILD: 0, INFANT: 0 };
    const map = {};
    data.forEach(p => {
      const idx = counters[p.passengerType] || 0;
      map[`${p.passengerType}_${idx}`] = p;
      counters[p.passengerType] = idx + 1;
    });
    return map;
  }

  function collectPassengerData() {
    const slots = buildPassengerSlots();
    const list  = [];
    for (let i = 0; i < slots.length; i++) {
      const nameEl = document.getElementById(`p_name_${i}`);
      if (!nameEl) break;
      list.push({
        fullName:      nameEl?.value?.trim()                            || '',
        dateOfBirth:   document.getElementById(`p_dob_${i}`)?.value   || '',
        idNumber:      document.getElementById(`p_id_${i}`)?.value?.trim() || '',
        passengerType: document.getElementById(`p_type_${i}`)?.value  || slots[i].type,
      });
    }
    return list;
  }

  function validatePassengers() {
    const slots = buildPassengerSlots();
    const data  = collectPassengerData();

    for (let i = 0; i < data.length; i++) {
      const slot = slots[i];
      const meta = PASSENGER_META[slot.type];

      if (!data[i].fullName) {
        alert(`Vui lòng nhập Họ và tên cho ${meta.label} ${slot.num}.`);
        document.getElementById(`p_name_${i}`)?.focus();
        return null;
      }
      if (!data[i].dateOfBirth) {
        alert(`Vui lòng nhập Ngày sinh cho ${meta.label} ${slot.num}.`);
        document.getElementById(`p_dob_${i}`)?.focus();
        return null;
      }
      if (slot.type === 'ADULT' && !data[i].idNumber) {
        alert(`Vui lòng nhập Số CCCD/Passport cho Người lớn ${slot.num}.`);
        document.getElementById(`p_id_${i}`)?.focus();
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
      const infants  = getInfantCount();
      const baseTotal = adults * currentPrice
        + children * currentPrice * 0.75
        + infants  * currentPrice * 0.10;

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
      const bookingReq = {
        userId:       user.id,
        scheduleId:   parseInt(scheduleId),
        adultCount:   getAdultCount(),
        childCount:   getChildCount(),
        infantCount:  getInfantCount(),
        discountCode: appliedVoucherCode || null,
        passengers:   passengers.map(p => ({
          fullName:      p.fullName,
          dateOfBirth:   p.dateOfBirth,
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
      } else if (selectedMethod === 'VNPAY') {
        const paymentRes = await TB.apiFetch(`/api/v1/payments/vnpay/create/${bookingId}`, {
          method: 'POST'
        });

        if (paymentRes.data?.checkoutUrl) {
          window.location.href = paymentRes.data.checkoutUrl;
        } else {
          alert('Không thể tạo liên kết VNPay. Vui lòng liên hệ hỗ trợ.');
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
