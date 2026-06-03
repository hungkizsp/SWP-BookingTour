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

  function renderGallery(images) {
    const root = el('gallery');
    if (!images || images.length === 0) {
      root.innerHTML = '<img src="https://danangbest.com/vnt_upload/tour/04_2023/banahill_1.jpg" class="main-img" alt="Tour img">';
      return;
    }
    const main = images[0];
    const others = images.slice(1, 3);
    let html = `<img src="${main}" class="main-img" alt="Tour image" />`;
    others.forEach(img => {
      html += `<img src="${img}" alt="Tour image small" />`;
    });
    root.innerHTML = html;
  }

  function renderItinerary(itineraryJson) {
    const root = el('itinerary');
    if (!itineraryJson) {
      root.innerHTML = '<p style="padding: 20px; color: var(--text-faint);">Vui lòng liên hệ để nhận lịch trình chi tiết từ nhân viên tư vấn.</p>';
      return;
    }

    try {
      const items = typeof itineraryJson === 'string' && itineraryJson.trim().startsWith('[') 
                    ? JSON.parse(itineraryJson) 
                    : itineraryJson;
      
      if (Array.isArray(items)) {
        root.innerHTML = items.map((item, idx) => `
          <div class="itinerary-item ${idx === 0 ? 'active' : ''}">
            <div class="itinerary-header" onclick="this.parentElement.classList.toggle('active')">
              <strong>${escapeHtml(item.title || item.day || `Ngày ${idx + 1}`)}</strong>
              <span style="font-size: 0.8rem; opacity: 0.5;">▼</span>
            </div>
            <div class="itinerary-content" style="padding: 20px; line-height: 1.8;">
              ${item.content || item.description || 'Nội dung đang được cập nhật...'}
            </div>
          </div>
        `).join('');
      } else {
        root.innerHTML = `<div class="itinerary-content" style="display:block; border:1px solid var(--border); border-radius:12px; background:white; padding:30px; line-height:2;">${itineraryJson}</div>`;
      }
    } catch (e) {
      root.innerHTML = `<div class="itinerary-content" style="display:block; border:1px solid var(--border); border-radius:12px; background:white; padding:30px; line-height:2;">${itineraryJson}</div>`;
    }
  }

  function renderSchedules(list) {
    const root = el('schedules');
    if (!list || list.length === 0) {
      root.innerHTML = '<option value="">Liên hệ hotline để xem lịch</option>';
      return;
    }
    root.innerHTML = list.map(s => {
      const status = String(s.status || '').toUpperCase();
      const isAvailable = status === 'AVAILABLE' || status === 'OPEN';
      return `<option value="${s.scheduleId}" ${!isAvailable ? 'disabled' : ''}>
        🚀 Khởi hành: ${s.startDate} - Còn ${s.availableSlots} chỗ
      </option>`;
    }).join('');
  }

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
    if (el('transport')) el('transport').textContent = t.transportType || 'Xe du lịch đời mới';
    if (el('tourCode')) el('tourCode').textContent = `DB-${t.id || id}`;

    if (el('suitableAges')) el('suitableAges').textContent = t.suitableAges || 'Mọi lứa tuổi';
    if (el('childPolicy')) el('childPolicy').textContent = t.childPolicy || 'Theo chính sách chung của công ty';
    if (el('whyChooseUs')) el('whyChooseUs').textContent = t.whyChooseUs || '';

    renderGallery(t.imageUrls);
    renderItinerary(t.itinerary);
    renderSchedules(t.schedules);
  }

  load().catch(err => {
    console.error(err);
    alert('Lỗi khi tải thông tin tour: ' + err.message);
  });
})();
