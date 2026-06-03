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
    let html = `<img src="${TB.normalizeImageUrl(main)}" class="main-img" alt="Tour image" />`;
    others.forEach(img => {
      html += `<img src="${TB.normalizeImageUrl(img)}" alt="Tour image small" />`;
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
    
    // Format JSON array dates like [2026, 4, 18] to string
    const toDateObj = (d) => {
      if (Array.isArray(d)) return new Date(d[0], d[1] - 1, d[2]);
      return new Date(d);
    };

    const formatDate = (d) => toDateObj(d).toLocaleDateString('vi-VN');

    const formatTime = (d) => {
      const date = toDateObj(d);
      const hh = String(date.getHours()).padStart(2, '0');
      const mm = String(date.getMinutes()).padStart(2, '0');
      return `${hh}:${mm}`;
    };

    root.innerHTML = '<option value="">-- Chọn lịch khởi hành --</option>' + list.map(s => {
      const status = String(s.status || '').toUpperCase();
      const isAvailable = status === 'AVAILABLE' || status === 'OPEN';
      const dateStr = formatDate(s.startDate);
      // Show "khung giờ" based on schedule time
      const timeStr = formatTime(s.startDate);
      return `<option value="${s.scheduleId}" ${!isAvailable ? 'disabled' : ''}>
        🚀 ${dateStr} lúc ${timeStr} - Còn ${s.availableSlots} chỗ
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
    } catch (e) {
      console.error('Failed to load reviews', e);
      el('reviewList').innerHTML = '<p style="color: var(--price); text-align: center;">Lỗi khi tải đánh giá.</p>';
    }
  }

  async function loadFaqs() {
    try {
      const res = await TB.apiFetch(`/api/v1/faqs/tour/${id}`, { method: 'GET' });
      const faqs = res || []; // If response is directly a list, handle that. If res.data, wait, public routes might not use standard ApiResponse. Our FaqController returns List<TourFaq> directly. So it's accessible via `await fetch().then(r=>r.json())`. But `TB.apiFetch` parses JSON natively. 
      // Wait, let's use standard fetch to be safe since it's a simple public endpoint:
      const raw = await fetch(`http://localhost:8080/api/v1/faqs/tour/${id}`);
      const list = await raw.json();

      const elFaqList = el('tourFaqList');
      if (!list || list.length === 0) {
        elFaqList.innerHTML = '<div style="padding: 20px; text-align: center; color: var(--text-faint);">Chưa có câu hỏi thường gặp cho tour này.</div>';
        return;
      }

      elFaqList.innerHTML = list.map(f => `
        <div class="faq-accordion" style="border-bottom: 1px solid var(--border);">
          <div style="padding: 15px 20px; font-weight: 700; color: var(--primary); cursor: pointer; display: flex; justify-content: space-between;" onclick="this.nextElementSibling.style.display = this.nextElementSibling.style.display === 'block' ? 'none' : 'block'">
            <span>${f.question}</span>
            <span>▼</span>
          </div>
          <div style="padding: 0 20px 15px; color: var(--text-soft); font-size: 0.95rem; line-height: 1.6; display: none;">${f.answer}</div>
        </div>
      `).join('');
    } catch(e) {
      console.error(e);
      el('tourFaqList').innerHTML = '<div style="padding: 20px; text-align: center; color: var(--text-faint);">Không thể tải câu hỏi.</div>';
    }
  }

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
      if(btn) btn.onclick = () => { sessionStorage.clear(); location.reload(); };
    } else {
      navRight.innerHTML = `
        <div style="display: flex; align-items: center; gap: 15px;">
           <a href="./auth/login.html" style="font-weight: 800; color: var(--text-soft); font-size: 0.9rem;">Đăng nhập</a>
           <a href="./auth/register.html" class="btn" style="padding: 0 25px; min-height: 40px; height: 40px; font-size: 0.8rem; border-radius: 10px;">ĐĂNG KÝ</a>
        </div>`;
    }
  }

  if (user) {
    const loginPrompt = el('loginPromptReview');
    const formContainer = el('reviewFormContainer');
    if(loginPrompt) loginPrompt.style.display = 'none';
    if(formContainer) formContainer.style.display = 'block';

    const submitBtn = el('submitReviewBtn');
    if (submitBtn) {
      submitBtn.onclick = async () => {
        const rating = el('reviewRating').value;
        const comment = el('reviewComment').value;
        if (!comment.trim()) {
          alert('Vui lòng nhập nhận xét!');
          return;
        }
        try {
          submitBtn.disabled = true;
          submitBtn.textContent = 'Đang gửi...';
          await TB.apiFetch('/api/v1/reviews', {
            method: 'POST',
            body: JSON.stringify({
              tourId: Number(id),
              userId: user.id || user.userId || user.id, // Fallback check
              rating: Number(rating),
              comment: comment.trim()
            })
          });
          alert('Gửi đánh giá thành công!');
          el('reviewComment').value = '';
          el('reviewRating').value = '5';
          submitBtn.disabled = false;
          submitBtn.textContent = 'Gửi đánh giá';
          loadReviews();
        } catch (err) {
          console.error(err);
          alert('Có lỗi xảy ra: ' + (err.response?.data?.message || err.message));
          submitBtn.disabled = false;
          submitBtn.textContent = 'Gửi đánh giá';
        }
      };
    }
  }
})();
