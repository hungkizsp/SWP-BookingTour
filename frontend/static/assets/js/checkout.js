/**
 * checkout.js — Luồng đặt tour với dynamic passenger forms & smart pricing
 * ADULT = 100% | CHILD = 75% | INFANT = 10% (không chiếm chỗ)
 */
(async () => {
  const params = new URLSearchParams(window.location.search);
  const tourId = params.get('tourId');
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

  const userStr = sessionStorage.getItem('user');
  let user = userStr ? JSON.parse(userStr) : null;
  if (!user) {
    TB.goToLogin('Vui lòng đăng nhập để tiếp tục đặt tour.');
    return;
  }
  try {
    const authRes = await TB.apiFetch('/api/v1/auth/me');
    if (authRes && authRes.data) user = authRes.data;
  } catch (err) {
    console.warn("Could not fetch latest profile", err);
  }

  // ─── UI element refs ──────────────────────────────────────────────────────────
  const loading = document.getElementById('checkoutLoading');
  const content = document.getElementById('checkoutContent');
  const adultInput = document.getElementById('adultCount');
  const childInput = document.getElementById('childCount');
  const infantInput = document.getElementById('infantCount');
  const passengersContainer = document.getElementById('passengersContainer');
  const summaryTourName = document.getElementById('summaryTourName');
  const summaryDate = document.getElementById('summaryDate');
  const summaryUnitPrice = document.getElementById('summaryUnitPrice');
  const summaryAdults = document.getElementById('summaryAdults');
  const summaryChildren = document.getElementById('summaryChildren');
  const summaryInfants = document.getElementById('summaryInfants');
  const summaryAdultPrice = document.getElementById('summaryAdultPrice');
  const summaryChildPrice = document.getElementById('summaryChildPrice');
  const summaryInfantPrice = document.getElementById('summaryInfantPrice');
  const summaryChildPriceRow = document.getElementById('summaryChildPriceRow');
  const summaryInfantPriceRow = document.getElementById('summaryInfantPriceRow');
  const summaryTotalPrice = document.getElementById('summaryTotalPrice');
  const confirmBtn = document.getElementById('confirmBtn');

  // ─── State ────────────────────────────────────────────────────────────────────
  let currentPrice = 0;
  let tourStartDate = null; // Date object (local midnight)
  let scheduleData = null;
  let appliedDiscount = 0;
  let appliedVoucherCode = '';
  let loyaltyPointsAvailable = 0;
  let loyaltyPointsToRedeem = 0;
  let loyaltyDiscountAmount = 0;
  let childRate = 0.75;
  let infantRate = 0.10;

  // Update meta visually later if needed, but keeping it simple for now or dynamic
  let PASSENGER_META = {
    ADULT: { icon: '🧑', label: 'Người lớn', badge: 'Người lớn · 100% giá', cardClass: '' },
    CHILD: { icon: '👦', label: 'Trẻ em', badge: 'Trẻ em · Đang cập nhật giá', cardClass: 'child-card' },
    INFANT: { icon: '👶', label: 'Em bé', badge: 'Em bé · Đang cập nhật giá', cardClass: 'infant-card' },
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
  document.getElementById('custName').value = user.fullName || '';
  document.getElementById('custEmail').value = user.email || '';
  const custPhone = document.getElementById('custPhone');
  if (custPhone) {
    custPhone.value = user.phoneNumber || user.phone || '';
  }

  // ─── Fetch tour & schedule ────────────────────────────────────────────────────
  try {
    const [tourRes, scheduleRes, discountRes, loyaltyRes] = await Promise.all([
      TB.apiFetch(`/api/v1/tours/${tourId}`),
      TB.apiFetch(`/api/v1/tours/schedules/${scheduleId}`),
      TB.apiFetch(`/api/v1/discount-policies`).catch(e => ({ data: [] })),
      TB.apiFetch(`/api/v1/loyalty/my-points`).catch(e => ({ data: null }))
    ]);

    const tourData = tourRes.data;
    scheduleData = scheduleRes.data;
    const policies = discountRes.data || discountRes || [];
    const loyaltyData = loyaltyRes.data;

    if (loyaltyData) {
      loyaltyPointsAvailable = loyaltyData.totalPoints || 0;
      document.getElementById('loyaltySection').style.display = 'block';
      document.getElementById('loyaltyTotalPoints').textContent = loyaltyPointsAvailable.toLocaleString('vi-VN');
      document.getElementById('loyaltyTotalValue').textContent = (loyaltyData.pointsValue || 0).toLocaleString('vi-VN');
    }
    currentPrice = tourData.price;
    tourStartDate = parseTourStartDate(scheduleData.startDate);

    // ── Schedule bookability pre-flight ───────────────────────────────────
    // This mirrors the backend's authoritative checks to provide instant UX feedback.
    // The server will ALWAYS re-validate; this is purely a convenience guard.
    const schedStatus = String(scheduleData.status || '').toUpperCase();
    const NON_BOOKABLE = ['CANCELLED', 'COMPLETED', 'IN_PROGRESS', 'BOOKING_CLOSED', 'EXPIRED_NO_BOOKING'];

    let blockReason = null;
    if (NON_BOOKABLE.includes(schedStatus)) {
      const labels = {
        CANCELLED: 'Lịch trình này đã bị hủy.',
        COMPLETED: 'Tour này đã hoàn thành.',
        IN_PROGRESS: 'Tour đang diễn ra, không thể đặt thêm chỗ.',
        BOOKING_CLOSED: 'Hạn đặt tour cho lịch trình này đã kết thúc.',
        EXPIRED_NO_BOOKING: 'Đã hết hạn - Không có khách đặt.',
      };
      blockReason = labels[schedStatus] || 'Lịch trình này hiện không thể đặt.';
    } else if (schedStatus === 'SOLD_OUT' || (scheduleData.availableSlots != null && scheduleData.availableSlots <= 0)) {
      blockReason = 'Tour đã hết chỗ. Vui lòng chọn lịch khác.';
    } else if (scheduleData.bookingDeadline) {
      const dl = Array.isArray(scheduleData.bookingDeadline)
        ? new Date(scheduleData.bookingDeadline[0], scheduleData.bookingDeadline[1] - 1, scheduleData.bookingDeadline[2], scheduleData.bookingDeadline[3] || 0, scheduleData.bookingDeadline[4] || 0)
        : new Date(scheduleData.bookingDeadline);
      if (!isNaN(dl) && new Date() >= dl) {
        blockReason = 'Hạn đặt tour đã kết thúc lúc ' + dl.toLocaleString('vi-VN') + '. Vui lòng chọn lịch khác.';
      }
    }

    if (blockReason) {
      loading.style.display = 'none';
      document.querySelector('main').innerHTML = `
        <div style="text-align:center; padding: 100px 20px;">
          <div style="font-size:3rem; margin-bottom:20px;">🚫</div>
          <h2 style="color: var(--primary); margin-bottom:15px;">Không thể đặt chỗ</h2>
          <p style="color: var(--text-soft); font-size: 1.05rem; margin-bottom: 30px;">${blockReason}</p>
          <a href="javascript:history.back()" class="btn" style="padding: 0 30px; height: 50px; border-radius: 12px;">← Chọn lịch khác</a>
        </div>`;
      return;
    }
    // ─────────────────────────────────────────────────────────────────────

    policies.forEach(p => {
      if (p.passengerType === 'CHILD' && p.isActive) childRate = p.rate;
      if (p.passengerType === 'INFANT' && p.isActive) infantRate = p.rate;
    });

    const childPct = Math.round(childRate * 100);
    const infantPct = Math.round(infantRate * 100);

    PASSENGER_META.CHILD.badge = `Trẻ em · ${childPct}% giá`;
    PASSENGER_META.INFANT.badge = `Em bé · ${infantPct}% giá`;

    // Update HTML labels in checkout.html (not in the checkout.js passenger cards)
    const childRateLbl = document.getElementById('childRateLabel');
    const infantRateLbl = document.getElementById('infantRateLabel');
    const summaryChildPct = document.getElementById('summaryChildPct');
    const summaryInfantPct = document.getElementById('summaryInfantPct');
    if (childRateLbl) childRateLbl.textContent = `× ${childPct}% giá`;
    if (infantRateLbl) infantRateLbl.textContent = `× ${infantPct}% giá`;
    if (summaryChildPct) summaryChildPct.textContent = `${childPct}%`;
    if (summaryInfantPct) summaryInfantPct.textContent = `${infantPct}%`;

    summaryTourName.textContent = tourData.tourName;
    const sd = new Date(scheduleData.startDate);
    summaryDate.textContent = `${sd.toLocaleDateString('vi-VN')} lúc ${String(sd.getHours()).padStart(2, '0')}:${String(sd.getMinutes()).padStart(2, '0')}`;
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
  function getAdultCount() { return Math.max(1, parseInt(adultInput.value) || 1); }
  function getChildCount() { return Math.max(0, parseInt(childInput.value) || 0); }
  function getInfantCount() { return Math.max(0, parseInt(infantInput.value) || 0); }
  function getTotalPeople() { return getAdultCount() + getChildCount() + getInfantCount(); }

  function buildPassengerSlots() {
    const slots = [];
    const adults = getAdultCount();
    const children = getChildCount();
    const infants = getInfantCount();

    for (let i = 0; i < adults; i++)   slots.push({ type: 'ADULT', num: i + 1 });
    for (let i = 0; i < children; i++) slots.push({ type: 'CHILD', num: i + 1 });
    for (let i = 0; i < infants; i++)  slots.push({ type: 'INFANT', num: i + 1 });
    return slots;
  }

  // ─── Pricing ──────────────────────────────────────────────────────────────────
  function updateTotals() {
    const adults = getAdultCount();
    const children = getChildCount();
    const infants = getInfantCount();

    summaryAdults.textContent = adults + ' người';
    summaryChildren.textContent = children + ' trẻ em';
    summaryInfants.textContent = infants + ' em bé';

    const adultTotal = adults * currentPrice;
    const childTotal = children * currentPrice * childRate;
    const infantTotal = infants * currentPrice * infantRate;
    const baseTotal = adultTotal + childTotal + infantTotal;

    summaryAdultPrice.textContent = adultTotal.toLocaleString('vi-VN') + ' đ';
    summaryChildPrice.textContent = childTotal.toLocaleString('vi-VN') + ' đ';
    summaryInfantPrice.textContent = infantTotal.toLocaleString('vi-VN') + ' đ';
    summaryChildPriceRow.style.display = children > 0 ? 'flex' : 'none';
    summaryInfantPriceRow.style.display = infants > 0 ? 'flex' : 'none';

    if (appliedDiscount > 0) {
      document.getElementById('discountItem').style.display = 'flex';
      document.getElementById('summaryDiscountAmount').textContent = '-' + appliedDiscount.toLocaleString('vi-VN') + ' đ';
      document.getElementById('summaryVoucherCode').textContent = appliedVoucherCode;
    } else {
      document.getElementById('discountItem').style.display = 'none';
    }

    if (loyaltyDiscountAmount > 0) {
      document.getElementById('loyaltyDiscountItem').style.display = 'flex';
      document.getElementById('summaryLoyaltyDiscountAmount').textContent = '-' + loyaltyDiscountAmount.toLocaleString('vi-VN') + ' đ';
      document.getElementById('summaryLoyaltyPoints').textContent = loyaltyPointsToRedeem.toLocaleString('vi-VN');
    } else {
      document.getElementById('loyaltyDiscountItem').style.display = 'none';
    }

    const finalTotal = Math.max(0, baseTotal - appliedDiscount - loyaltyDiscountAmount);
    summaryTotalPrice.textContent = finalTotal.toLocaleString('vi-VN');
  }

  function onCountChange() {
    const available = (scheduleData && scheduleData.availableSlots != null) ? scheduleData.availableSlots : 9999;
    let adults = getAdultCount();
    let children = getChildCount();
    let infants = getInfantCount();

    // Check total slots vs available
    if (adults + children > available) {
      alert(`Tổng số chỗ đăng ký (người lớn + trẻ em: ${adults + children}) không được vượt quá số chỗ còn lại của tour (còn ${available} chỗ).`);
      if (adults > available) {
        adultInput.value = available;
        childInput.value = 0;
      } else {
        childInput.value = available - adults;
      }
      adults = getAdultCount();
      children = getChildCount();
    }

    // Check chaperone ratio
    if (children > adults * 2) {
      alert('Số lượng trẻ em vượt quá giới hạn (tối đa 2 trẻ em / 1 người lớn). Đã tự động điều chỉnh.');
      childInput.value = adults * 2;
      children = adults * 2;
    }

    if (infants > adults * 1) {
      alert('Số lượng em bé vượt quá giới hạn (tối đa 1 em bé / 1 người lớn). Đã tự động điều chỉnh.');
      infantInput.value = adults * 1;
      infants = adults * 1;
    }

    renderPassengerForms();
    updateTotals();
    appliedDiscount = 0;
    appliedVoucherCode = '';
    resetVoucherUI();
    loyaltyDiscountAmount = 0;
    loyaltyPointsToRedeem = 0;
    resetLoyaltyUI();
  }

  // ─── Counters ─────────────────────────────────────────────────────────────────
  document.getElementById('plusAdultBtn').onclick = () => {
    const available = (scheduleData && scheduleData.availableSlots != null) ? scheduleData.availableSlots : 9999;
    if (getAdultCount() + getChildCount() + getInfantCount() + 1 > available) {
      alert(`Không thể tăng thêm. Tổng số chỗ đăng ký không được vượt quá số chỗ còn lại của tour (còn ${available} chỗ).`);
      return;
    }
    adultInput.value = getAdultCount() + 1;
    onCountChange();
  };

  document.getElementById('minusAdultBtn').onclick = () => {
    if (getAdultCount() > 1) {
      adultInput.value = getAdultCount() - 1;
      onCountChange();
    }
  };
  adultInput.onchange = onCountChange;

  document.getElementById('plusChildBtn').onclick = () => {
    const available = (scheduleData && scheduleData.availableSlots != null) ? scheduleData.availableSlots : 9999;
    if (getAdultCount() + getChildCount() + 1 > available) {
      alert(`Không thể tăng thêm. Tổng số chỗ đăng ký không được vượt quá số chỗ còn lại của tour (còn ${available} chỗ).`);
      return;
    }
    if (getChildCount() + 1 > getAdultCount() * 2) {
      alert('Số trẻ em vượt quá giới hạn (tối đa 2 trẻ em / 1 người lớn đi cùng)');
      return;
    }
    childInput.value = getChildCount() + 1;
    onCountChange();
  };
  document.getElementById('minusChildBtn').onclick = () => {
    if (getChildCount() > 0) {
      childInput.value = getChildCount() - 1;
      onCountChange();
    }
  };
  childInput.onchange = onCountChange;

  document.getElementById('plusInfantBtn').onclick = () => {
    if (getInfantCount() + 1 > getAdultCount() * 1) {
      alert('Số em bé vượt quá giới hạn (tối đa 1 em bé / 1 người lớn đi cùng)');
      return;
    }
    infantInput.value = getInfantCount() + 1;
    onCountChange();
  };
  document.getElementById('minusInfantBtn').onclick = () => {
    if (getInfantCount() > 0) {
      infantInput.value = getInfantCount() - 1;
      onCountChange();
    }
  };
  infantInput.onchange = onCountChange;

  // ─── Dynamic passenger form renderer ─────────────────────────────────────────
  function renderPassengerForms() {
    const slots = buildPassengerSlots();
    const oldMap = mapPassengerDataByType(collectPassengerData());

    passengersContainer.innerHTML = '';

    slots.forEach((slot, i) => {
      const meta = PASSENGER_META[slot.type];
      const label = `${meta.label} ${slot.num}`;
      const old = oldMap[`${slot.type}_${slot.num - 1}`] || {};
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
          ${(slot.type === 'ADULT') ? `<button type="button" id="ocr_btn_${i}" class="btn btn-secondary" style="margin-left:auto; padding: 6px 12px; font-size: 0.8rem; background: #e2e8f0; color: #1e293b; border: 1px solid #cbd5e1; border-radius: 6px; cursor: pointer;" onclick="window.importCCCD(${i})">Import CCCD</button>` : ''}
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
              value="${escHtml(old.dateOfBirth || '')}"
              onchange="onDobChange(${i})">
          </div>
          <div class="form-group" id="p_id_group_${i}" style="${slot.type === 'INFANT' ? 'display:none;' : ''}">
            <label id="p_id_label_${i}">${idLabel}</label>
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
    const list = [];
    for (let i = 0; i < slots.length; i++) {
      const nameEl = document.getElementById(`p_name_${i}`);
      if (!nameEl) break;
      list.push({
        fullName: nameEl?.value?.trim() || '',
        dateOfBirth: document.getElementById(`p_dob_${i}`)?.value || '',
        idNumber: document.getElementById(`p_id_${i}`)?.value?.trim() || '',
        passengerType: document.getElementById(`p_type_${i}`)?.value || slots[i].type,
      });
    }
    return list;
  }

  function validatePassengers() {
    const slots = buildPassengerSlots();
    const data = collectPassengerData();

    for (let i = 0; i < data.length; i++) {
      const slot = slots[i];
      const meta = PASSENGER_META[slot.type];

      if (!data[i].fullName) {
        alert(`Vui lòng nhập Họ và tên cho ${meta.label} ${slot.num}.`);
        document.getElementById(`p_name_${i}`)?.focus();
        return null;
      }
      if (/[^a-zA-ZÀ-Ỹà-ỹ\s]/.test(data[i].fullName) || /\s{2,}/.test(data[i].fullName)) {
        alert(`Họ và tên cho ${meta.label} ${slot.num} không được chứa ký tự đặc biệt hoặc khoảng trắng liên tiếp.`);
        document.getElementById(`p_name_${i}`)?.focus();
        return null;
      }

      if (!data[i].dateOfBirth) {
        alert(`Vui lòng nhập Ngày sinh cho ${meta.label} ${slot.num}.`);
        document.getElementById(`p_dob_${i}`)?.focus();
        return null;
      }

      const dob = new Date(data[i].dateOfBirth);
      const nowToday = new Date();
      nowToday.setHours(0, 0, 0, 0);
      const dobCheck = new Date(dob);
      dobCheck.setHours(0, 0, 0, 0);

      if (dobCheck > nowToday) {
        alert(`Ngày sinh của ${meta.label} ${slot.num} không thể ở tương lai.`);
        document.getElementById(`p_dob_${i}`)?.focus();
        return null;
      }

      if (tourStartDate) {
        const startCheck = new Date(tourStartDate);
        startCheck.setHours(0, 0, 0, 0);
        if (dobCheck >= startCheck) {
          alert(`Ngày sinh của ${meta.label} ${slot.num} phải trước ngày khởi hành tour.`);
          document.getElementById(`p_dob_${i}`)?.focus();
          return null;
        }
      }

      const today = tourStartDate || new Date();
      let age = today.getFullYear() - dob.getFullYear();
      const m = today.getMonth() - dob.getMonth();
      if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
        age--;
      }

      if (slot.type === 'ADULT' && age < 12) {
        alert(`Ngày sinh không hợp lệ cho ${meta.label} ${slot.num}. Người lớn (adult) phải từ 12 tuổi trở lên tính đến ngày khởi hành.`);
        document.getElementById(`p_dob_${i}`)?.focus();
        return null;
      }
      if (slot.type === 'CHILD' && (age < 2 || age > 11)) {
        alert(`Ngày sinh không hợp lệ cho ${meta.label} ${slot.num}. Trẻ em phải từ 2 đến 11 tuổi.`);
        document.getElementById(`p_dob_${i}`)?.focus();
        return null;
      }
      if (slot.type === 'INFANT' && age >= 2) {
        alert(`Ngày sinh không hợp lệ cho ${meta.label} ${slot.num}. Em bé phải nhỏ hơn 2 tuổi.`);
        document.getElementById(`p_dob_${i}`)?.focus();
        return null;
      }

      if (slot.type === 'ADULT') {
        if (!data[i].idNumber) {
          alert(`Vui lòng nhập Số CCCD / Passport cho ${meta.label} ${slot.num}.`);
          document.getElementById(`p_id_${i}`)?.focus();
          return null;
        }
        if (!/^\d{12}$/.test(data[i].idNumber) && !/^[A-Z0-9]{8,12}$/i.test(data[i].idNumber)) {
          alert(`Số định danh cho ${meta.label} ${slot.num} không hợp lệ. Định dạng CCCD (12 chữ số) hoặc Passport (8-12 ký tự chữ và số).`);
          document.getElementById(`p_id_${i}`)?.focus();
          return null;
        }

        // Check duplicate CCCD/Passport
        for (let j = 0; j < i; j++) {
          if (slots[j].type === 'ADULT' && data[j].idNumber === data[i].idNumber) {
            alert(`Số định danh của ${meta.label} ${slot.num} bị trùng với ${PASSENGER_META[slots[j].type].label} ${slots[j].num}.`);
            document.getElementById(`p_id_${i}`)?.focus();
            return null;
          }
        }
      }
    }
    return data;
  }

  function resetVoucherUI() {
    document.getElementById('voucherMsg').textContent = '';
    document.getElementById('voucherCode').value = '';
  }

  // ─── Voucher ──────────────────────────────────────────────────────────────────
  const voucherInput = document.getElementById('voucherCode');
  const applyVoucherBtn = document.getElementById('applyVoucherBtn');
  const voucherMsg = document.getElementById('voucherMsg');

  applyVoucherBtn.onclick = async () => {
    const code = voucherInput.value.trim().toUpperCase();
    if (!code) return;

    applyVoucherBtn.disabled = true;
    applyVoucherBtn.textContent = '...';

    try {
      const adults = getAdultCount();
      const children = getChildCount();
      const infants = getInfantCount();
      const baseTotal = adults * currentPrice
        + children * currentPrice * childRate
        + infants * currentPrice * infantRate;

      const res = await TB.apiFetch('/api/v1/bookings/apply-voucher', {
        method: 'POST',
        body: JSON.stringify({ voucherCode: code, currentTotal: baseTotal, tourId: parseInt(tourId) })
      });

      if (res.data.isValid) {
        appliedDiscount = res.data.discountAmount;
        appliedVoucherCode = code;
        voucherMsg.style.color = '#4caf50';
        voucherMsg.textContent = res.data.message;
      } else {
        appliedDiscount = 0;
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
      applyVoucherBtn.disabled = false;
      applyVoucherBtn.textContent = 'Áp dụng';
    }
  };

  function resetLoyaltyUI() {
    document.getElementById('loyaltyMsg').textContent = '';
    document.getElementById('loyaltyPointsInput').value = '';
  }

  // ─── Loyalty Points ──────────────────────────────────────────────────────────
  const applyLoyaltyBtn = document.getElementById('applyLoyaltyBtn');
  const loyaltyInput = document.getElementById('loyaltyPointsInput');
  const loyaltyMsg = document.getElementById('loyaltyMsg');

  applyLoyaltyBtn.onclick = async () => {
    const points = parseInt(loyaltyInput.value);
    if (isNaN(points) || points <= 0) return;

    if (points > loyaltyPointsAvailable) {
      loyaltyMsg.style.color = '#f44336';
      loyaltyMsg.textContent = 'Số điểm muốn dùng vượt quá số điểm hiện có.';
      return;
    }

    applyLoyaltyBtn.disabled = true;
    applyLoyaltyBtn.textContent = '...';

    try {
      const adults = getAdultCount();
      const children = getChildCount();
      const infants = getInfantCount();
      const baseTotal = adults * currentPrice
        + children * currentPrice * childRate
        + infants * currentPrice * infantRate;

      // If there is already a voucher discount, we pass the total AFTER voucher
      const totalAfterVoucher = Math.max(0, baseTotal - appliedDiscount);

      const res = await TB.apiFetch('/api/v1/loyalty/validate-redeem', {
        method: 'POST',
        body: JSON.stringify({ pointsToRedeem: points, bookingTotal: totalAfterVoucher })
      });

      if (res.data.valid) {
        loyaltyDiscountAmount = res.data.discountAmount;
        loyaltyPointsToRedeem = points;
        loyaltyMsg.style.color = '#4caf50';
        loyaltyMsg.textContent = res.data.message || `Đã đổi thành công ${points.toLocaleString('vi-VN')} điểm.`;
      } else {
        loyaltyDiscountAmount = 0;
        loyaltyPointsToRedeem = 0;
        loyaltyMsg.style.color = '#f44336';
        loyaltyMsg.textContent = res.data.message || 'Không thể đổi điểm.';
      }
      updateTotals();
    } catch (err) {
      console.error(err);
      loyaltyMsg.style.color = '#f44336';
      loyaltyMsg.textContent = 'Lỗi hệ thống khi đổi điểm.';
    } finally {
      applyLoyaltyBtn.disabled = false;
      applyLoyaltyBtn.textContent = 'Đổi điểm';
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

  // ─── OCR CCCD ─────────────────────────────────────────────────────────────────
  const cccdInput = document.createElement('input');
  cccdInput.type = 'file';
  cccdInput.accept = 'image/*';
  cccdInput.style.display = 'none';
  document.body.appendChild(cccdInput);

  let currentOCRIndex = 0;
  window.importCCCD = function (index) {
    currentOCRIndex = index;
    cccdInput.click();
  };

  cccdInput.addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const btn = document.getElementById(`ocr_btn_${currentOCRIndex}`);
    if (btn) btn.textContent = 'Đang quét...';

    try {
      const { data: { text } } = await Tesseract.recognize(file, 'vie');
      console.log("OCR Result:", text);

      // Normalize OCR text
      const normalizedText = text
        .replace(/\r/g, "")
        .replace(/[‘’'"]/g, "")       // remove quote characters
        .replace(/[|]/g, "1")
        .replace(/Q/g, "0")           // OCR thường nhận 0 -> Q
        .replace(/O/g, "0")           // OCR thường nhận 0 -> O
        .replace(/\s+/g, " ")
        .trim();

      // ─── 1. CCCD Number ─────────────────────────────
      let idMatch = normalizedText.match(/\b\d{12}\b/);

      // fallback: nếu OCR chèn khoảng trắng
      if (!idMatch) {
        const digits = normalizedText.replace(/\D/g, "");
        if (digits.length >= 12) {
          idMatch = [digits.substring(0, 12)];
        }
      }

      const idEl = document.getElementById(`p_id_${currentOCRIndex}`);
      if (idMatch && idEl) {
        idEl.value = idMatch[0];
      }

      // ─── 2. Date of Birth ───────────────────────────
      const dobText = text
        .replace(/Q/g, "0")
        .replace(/O/g, "0");

      let dobMatch =
        dobText.match(/(\d{2})[\/\-\.](\d{2})[\/\-\.](\d{4})/) ||
        dobText.match(/Date.*?(\d{2})[\/\-\.]([0-9QO]{2})[\/\-\.](\d{4})/i);

      const dobEl = document.getElementById(`p_dob_${currentOCRIndex}`);
      if (dobMatch && dobEl) {
        const day = dobMatch[1];
        const month = dobMatch[2].replace(/[QO]/g, "0");
        const year = dobMatch[3];
        dobEl.value = `${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`;
      }

      // ─── 3. Full Name ──────────────────────────────
      let nameStr = "";
      const lines = text
        .split("\n")
        .map(l => l.trim())
        .filter(Boolean);

      // Ưu tiên lấy dòng ngay sau "Full name"
      for (let i = 0; i < lines.length; i++) {
        if (/full\s*name/i.test(lines[i])) {
          const candidate = (lines[i + 1] || "")
            .replace(/[‘’'".,]/g, "")
            .trim();

          if (candidate.length >= 5) {
            nameStr = candidate;
            break;
          }
        }
      }

      // Fallback: tìm dòng in hoa
      if (!nameStr) {
        for (const rawLine of lines) {
          const line = rawLine.replace(/[‘’'".,]/g, "").trim();

          if (
            /^[A-ZÀ-Ỹ\s]+$/i.test(line) &&
            line.length >= 5 &&
            !line.includes("CỘNG") &&
            !line.includes("ĐỘC") &&
            !line.includes("VIỆT NAM") &&
            !line.includes("CĂN CƯỚC") &&
            !line.includes("CITIZEN") &&
            !line.includes("IDENTITY")
          ) {
            nameStr = line;
            break;
          }
        }
      }

      const nameEl = document.getElementById(`p_name_${currentOCRIndex}`);
      if (nameStr && nameEl) {
        nameEl.value = nameStr;
      }

      // ─── Debug log (masking) ───────────────────────
      console.log(
        "OCR Extracted ID:",
        idMatch ? "xxxx" + idMatch[0].substring(4) : "None"
      );
      console.log(
        "OCR Extracted DOB:",
        dobMatch
          ? `${dobMatch[1]}/${dobMatch[2].replace(/[QO]/g, "0")}/${dobMatch[3]}`
          : "None"
      );
      console.log(
        "OCR Extracted Name:",
        nameStr
          ? nameStr.substring(0, 2) + "xxxx"
          : "None"
      );
      // Masking in console for privacy
      alert('Đã nhận diện thông tin, vui lòng kiểm tra lại!');
    } catch (err) {
      console.error(err);
      alert('Không thể nhận diện hình ảnh.');
    } finally {
      if (btn) btn.textContent = 'Import CCCD';
      cccdInput.value = '';
    }
  });

  window.onDobChange = function (index) {
    const dobEl = document.getElementById(`p_dob_${index}`);
    if (!dobEl || !dobEl.value) return;

    const dob = new Date(dobEl.value);
    const today = tourStartDate || new Date();
    let age = today.getFullYear() - dob.getFullYear();
    const m = today.getMonth() - dob.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
      age--;
    }

    let newType = 'INFANT';
    if (age >= 12) newType = 'ADULT';
    else if (age >= 2) newType = 'CHILD';

    const typeEl = document.getElementById(`p_type_${index}`);
    if (typeEl && typeEl.value !== newType) {
      typeEl.value = newType;

      // Update styling
      const meta = PASSENGER_META[newType];
      const card = typeEl.closest('.passenger-card');
      if (card) {
        card.className = `passenger-card ${meta.cardClass}`.trim();
        const header = card.querySelector('.passenger-header');
        if (header) {
          const labelSpan = header.querySelector('.passenger-label');
          if (labelSpan) labelSpan.innerHTML = `${meta.icon} ${meta.label} ${index + 1}`;
          const badgeSpan = header.querySelector('.passenger-badge');
          if (badgeSpan) badgeSpan.textContent = meta.badge;
        }
      }

      // Toggle ID field visibility
      const idGroup = document.getElementById(`p_id_group_${index}`);
      if (idGroup) {
        if (newType === 'INFANT') {
          idGroup.style.display = 'none';
        } else {
          idGroup.style.display = 'block';
          const label = document.getElementById(`p_id_label_${index}`);
          if (label) {
            label.innerHTML = newType === 'ADULT' ? 'Số CCCD / Passport <span style="color:#e53e3e">*</span>' : 'Số định danh (CCCD/Passport)';
          }
        }
      }

      // Adjust UI counters to match reality so totals match
      recalculateCountsFromForms();
    }
  };

  function recalculateCountsFromForms() {
    let a = 0, c = 0, i = 0;
    const slots = buildPassengerSlots();
    for (let idx = 0; idx < slots.length; idx++) {
      const typeEl = document.getElementById(`p_type_${idx}`);
      const type = typeEl ? typeEl.value : slots[idx].type;
      if (type === 'ADULT') a++;
      if (type === 'CHILD') c++;
      if (type === 'INFANT') i++;
    }
    // Update inputs without triggering onchange
    adultInput.value = a;
    childInput.value = c;
    infantInput.value = i;
    updateTotals();
  }

  // ─── Confirm booking ──────────────────────────────────────────────────────────
  confirmBtn.onclick = async () => {
    // 1. Validate contact info
    const phoneEl = document.getElementById('custPhone');
    const phone = phoneEl ? phoneEl.value.trim() : '';
    if (!phone) {
      alert('Vui lòng nhập Số điện thoại liên lạc.');
      phoneEl?.focus();
      return;
    }
    if (!/^0\d{9}$/.test(phone)) {
      alert('Số điện thoại liên lạc không hợp lệ. Phải gồm đúng 10 chữ số và bắt đầu bằng số 0.');
      phoneEl?.focus();
      return;
    }

    // 2. Validate accompaniment ratio
    // 2. Validate accompaniment ratio (2 children + 1 infant per adult, checked independently)
    if (getChildCount() > getAdultCount() * 2) {
      alert('Số trẻ em vượt quá giới hạn (tối đa 2 trẻ em / 1 người lớn đi cùng).');
      return;
    }
    if (getInfantCount() > getAdultCount() * 1) {
      alert('Số em bé vượt quá giới hạn (tối đa 1 em bé / 1 người lớn đi cùng).');
      return;
    }

    // 3. Validate slots limit against available slots
    const requestedSlots = getAdultCount() + getChildCount();
    const available = scheduleData.availableSlots != null ? scheduleData.availableSlots : 9999;
    if (requestedSlots > available) {
      alert(`Số lượng khách đăng ký (người lớn + trẻ em: ${requestedSlots}) vượt quá số chỗ còn lại của tour (còn ${available} chỗ).`);
      return;
    }

    // 4. Validate passenger cards
    const passengers = validatePassengers();
    if (!passengers) return;

    // TASK 5: Infant cap — max 2 infants per booking (must match backend rule)
    const maxSlots = scheduleData.maxSlots || scheduleData.availableSlots || 20;
    const MAX_INFANTS = Math.min(2, Math.floor(maxSlots * 0.1));
    if (getInfantCount() > MAX_INFANTS) {
      alert(`Mỗi booking chỉ được phép tối đa ${MAX_INFANTS} em bé (dưới 2 tuổi) để đảm bảo an toàn tour.`);
      return;
    }

    confirmBtn.disabled = true;
    confirmBtn.textContent = 'ĐANG XỬ LÝ...';

    // Auto-update user phone number if changed
    if (phone !== (user.phoneNumber || user.phone || '')) {
      try {
        await TB.apiFetch(`/api/v1/users/${user.id}`, {
          method: 'PUT',
          body: JSON.stringify({
            fullName: user.fullName,
            email: user.email,
            phoneNumber: phone,
            role: user.role,
            isActive: user.isActive
          })
        });
        user.phoneNumber = phone;
        sessionStorage.setItem('user', JSON.stringify(user));
      } catch (e) {
        console.warn("Could not auto-update user phone number in profile", e);
      }
    }

    try {
      const bookingReq = {
        userId: user.id,
        scheduleId: parseInt(scheduleId),
        adultCount: getAdultCount(),
        childCount: getChildCount(),
        infantCount: getInfantCount(),
        discountCode: appliedVoucherCode || null,
        pointsToRedeem: loyaltyPointsToRedeem || 0,
        passengers: passengers.map(p => ({
          fullName: p.fullName,
          dateOfBirth: p.dateOfBirth,
          idNumber: p.idNumber || null,
          passengerType: p.passengerType
        }))
      };

      const bookingRes = await TB.apiFetch('/api/v1/bookings', {
        method: 'POST',
        body: JSON.stringify(bookingReq)
      });

      const bookingId = bookingRes.data.id;

      // Bước 1: Gọi API redeem điểm trước khi thanh toán nếu có dùng điểm
      if (loyaltyPointsToRedeem > 0) {
        try {
          const rawTotal = (getAdultCount() * currentPrice) +
            (getChildCount() * currentPrice * childRate) +
            (getInfantCount() * currentPrice * infantRate);

          await TB.apiFetch('/api/v1/loyalty/redeem', {
            method: 'POST',
            body: JSON.stringify({
              bookingId: bookingId,
              pointsToRedeem: loyaltyPointsToRedeem,
              bookingTotal: rawTotal
            })
          });
        } catch (e) {
          console.error("Lỗi khi áp dụng điểm thưởng:", e);
          alert('Có lỗi xảy ra khi áp dụng điểm thưởng, vui lòng thử lại!');
          confirmBtn.disabled = false;
          confirmBtn.textContent = 'XÁC NHẬN ĐẶT TOUR';
          return;
        }
      }

      if (selectedMethod === 'PAYOS') {
        const paymentRes = await TB.apiFetch('/api/v1/payments/payos/create', {
          method: 'POST',
          body: JSON.stringify({ bookingId, paymentMethod: 'PAYOS' })
        });

        if (paymentRes.data?.checkoutUrl) {
          window.location.href = paymentRes.data.checkoutUrl;
        } else {
          alert('Không thể tạo liên kết thanh toán. Vui lòng liên hệ hỗ trợ.');
          confirmBtn.disabled = false;
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
          confirmBtn.disabled = false;
          confirmBtn.textContent = 'XÁC NHẬN ĐẶT TOUR';
        }
      } else {
        await TB.apiFetch('/api/v1/payments/cash/intent', {
          method: 'POST',
          body: JSON.stringify({ bookingId })
        });
        alert('Đặt tour thành công! Vui lòng đến văn phòng Danangbest để hoàn tất thanh toán.');
        window.location.href = './index.html';
      }
    } catch (err) {
      console.error(err);
      alert('Đã xảy ra lỗi: ' + err.message);
      confirmBtn.disabled = false;
      confirmBtn.textContent = 'XÁC NHẬN ĐẶT TOUR';
    }
  };
})();
