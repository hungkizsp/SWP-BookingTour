(() => {
  const ids = JSON.parse(localStorage.getItem('compareIds') || '[]');
  const gridWrapper = document.getElementById('gridWrapper');
  const tableHead = document.getElementById('tableHead');
  const tableBody = document.getElementById('tableBody');
  const empty = document.getElementById('empty');

  if (ids.length === 0) {
    empty.style.display = 'block';
    return;
  }

  let currentTours = [];

  const toDateObj = (d) => {
    if (Array.isArray(d)) return new Date(d[0], d[1] - 1, d[2]);
    return new Date(d);
  };

  const formatDate = (d) => {
    const dt = toDateObj(d);
    return Number.isNaN(dt.getTime()) ? '' : dt.toLocaleDateString('vi-VN');
  };

  async function load() {
    try {
      const idString = ids.join(',');
      const res = await TB.apiFetch(`/api/v1/tours/compare?ids=${idString}`);
      currentTours = res.data || [];

      if (currentTours.length === 0) {
        localStorage.removeItem('compareIds');
        empty.style.display = 'block';
        return;
      }

      empty.style.display = 'none';
      gridWrapper.style.display = 'block';

      // 1. Render Header Row (Images & Titles)
      tableHead.innerHTML = `
        <tr>
          <th class="label-cell">THÔNG TIN CHUNG</th>
          ${currentTours.map(t => `
            <th class="tour-header-cell">
              <div style="width: 100%; height: 180px; overflow:hidden; border-radius:12px; margin-bottom:15px; background: #f1f5f9;">
                <img src="${(t.imageUrls && t.imageUrls[0]) || 'https://images.unsplash.com/photo-1552074284-5e88ef1aef18?auto=format&fit=crop&w=800'}" 
                     style="width:100%; height:100%; object-fit:cover; display: block;">
              </div>
              <div class="tour-title" style="min-height: 3.5em; display: flex; align-items: center; justify-content: center;">${t.tourName}</div>
              <div style="font-size: 1.4rem; font-weight: 800; color: var(--price);">${t.price ? Number(t.price).toLocaleString() + 'đ' : 'Liên hệ'}</div>
              <button class="remove-btn-table" data-id="${t.id}">Xóa khỏi so sánh</button>
            </th>
          `).join('')}
        </tr>
      `;

      // 2. Helper to render a data row
      const renderRow = (label, contentFn) => {
        return `
          <tr>
            <td class="label-cell">${label}</td>
            ${currentTours.map(t => `<td>${contentFn(t)}</td>`).join('')}
          </tr>
        `;
      };

      // 3. Render Body Rows
      let bodyHtml = '';
      
      bodyHtml += renderRow('Thời lượng & Điểm đến', t => `
        <div style="font-weight: 700; color: var(--primary-dark); margin-bottom: 5px;">🕒 ${t.duration ? t.duration + ' Ngày' : 'Liên hệ'}</div>
        <div style="font-size: 0.85rem;">📍 Khởi hành: <strong>${t.startLocation || 'Đà Nẵng'}</strong></div>
      `);

      bodyHtml += renderRow('Điểm nhấn nổi bật', t => `
        <div style="display: flex; flex-direction: column; gap: 8px;">
          ${(t.highlights || []).map(h => `
            <div style="display: flex; align-items: flex-start; gap: 8px; font-size: 0.85rem; color: var(--text-soft); line-height: 1.4;">
              <span style="color: #10b981; font-weight: 800;">✓</span>
              <span>${h}</span>
            </div>
          `).join('') || '<div style="font-size: 0.85rem; color: var(--text-faint);">Đang cập nhật...</div>'}
        </div>
      `);

      bodyHtml += renderRow('Mô tả chi tiết', t => `
        <div style="font-size: 0.82rem; line-height: 1.6; color: var(--text-soft); text-align: justify;">
          ${t.description ? (t.description.length > 500 ? t.description.substring(0, 500) + '...' : t.description) : 'Đang cập nhật mô tả...'}
        </div>
      `);

      bodyHtml += renderRow('Lịch trình tóm tắt', t => {
        if (!t.itinerary) return '<div class="itinerary-summary">Đang cập nhật...</div>';
        try {
          const items = JSON.parse(t.itinerary);
          if (Array.isArray(items)) {
            return `
              <div style="font-size: 0.82rem; line-height: 1.6;">
                ${items.slice(0, 3).map(item => `
                  <div style="margin-bottom: 8px;">
                    <div style="font-weight: 800; color: var(--primary-dark);">${item.title}</div>
                    <div style="color: var(--text-soft); text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">${item.content}</div>
                  </div>
                `).join('')}
                <button class="btn-text-action" onclick="showFullItinerary(${t.id})">... Xem thêm chi tiết</button>
              </div>
            `;
          }
        } catch (e) {}
        return `<div class="itinerary-summary">${t.itinerary.substring(0, 150)}...</div>`;
      });

      bodyHtml += renderRow('Lịch khởi hành & Khung giờ', t => `
        <div style="margin-bottom: 8px; font-size: 0.8rem; font-weight: 600; color: var(--text-soft);">Các ngày gần nhất:</div>
        <div>
          ${(t.schedules || []).slice(0, 4).map(s => `<span class="schedule-tag">${formatDate(s.startDate) || '—'}</span>`).join('') || '<span style="font-size:0.8rem;">Liên hệ để biết lịch</span>'}
        </div>
      `);

      bodyHtml += renderRow('Đối tượng & Chính sách', t => `
        <div class="policy-box">
          <div style="font-weight: 800; margin-bottom: 5px;">👥 Dành cho: ${t.suitableAges || 'Mọi lứa tuổi'}</div>
          <div>🧒 Trẻ em: ${t.childPolicy || 'Theo chính sách chung'}</div>
        </div>
      `);

      bodyHtml += renderRow('Tại sao chọn tour này?', t => `
        <div class="why-box">
          ✨ ${t.whyChooseUs || 'Chất lượng phục vụ cam kết hàng đầu, hỗ trợ khách hàng 24/7.'}
        </div>
      `);

      bodyHtml += renderRow('Đánh giá & Phương tiện', t => `
        <div style="font-weight: 700;">⭐ ${(t.rating || 5.0).toFixed(1)} / 5.0</div>
        <div style="font-size: 0.85rem; color: var(--text-soft);">🚌 ${t.transportType || 'Xe du lịch đời mới'}</div>
      `);

      bodyHtml += renderRow('Thao tác', t => `
        <a href="./tour-detail.html?id=${t.id}" class="btn" style="width:100%; height:44px; border-radius:10px; font-size: 0.8rem;">XEM CHI TIẾT</a>
      `);

      tableBody.innerHTML = bodyHtml;

      // 4. Handle Remove
      document.querySelectorAll('.remove-btn-table').forEach(btn => {
        btn.onclick = () => {
          const idToRemove = String(btn.dataset.id);
          const currentIds = JSON.parse(localStorage.getItem('compareIds') || '[]');
          const newIds = currentIds.map(String).filter(id => id !== idToRemove);
          localStorage.setItem('compareIds', JSON.stringify(newIds));
          window.location.reload();
        };
      });

    } catch (err) {
      console.error('Error in comparison:', err);
      empty.innerHTML = `<p style="color:red;">Lỗi khi tải dữ liệu. <button onclick="location.reload()">Thử lại</button></p>`;
      empty.style.display = 'block';
    }
  }

  // Exposed globally for onclick
  window.showFullItinerary = (tourId) => {
    const tour = currentTours.find(t => t.id == tourId);
    if (!tour || !tour.itinerary) return;
    
    try {
      const items = JSON.parse(tour.itinerary);
      const modal = document.getElementById('itineraryModal');
      const modalBody = document.getElementById('modalBody');
      const modalTitle = document.getElementById('modalTitle');

      modalTitle.textContent = `Lịch trình chi tiết: ${tour.tourName}`;
      modalBody.innerHTML = items.map(item => `
        <div class="itinerary-item-full">
          <div class="time-title">${item.title}</div>
          <div class="content-text">${item.content}</div>
        </div>
      `).join('');

      modal.style.display = 'flex';
      document.body.style.overflow = 'hidden';
    } catch (e) {
      console.error('Failed to parse itinerary', e);
    }
  };

  const closeModal = document.getElementById('closeModal');
  const modal = document.getElementById('itineraryModal');
  if (closeModal) {
    closeModal.onclick = () => {
      modal.style.display = 'none';
      document.body.style.overflow = '';
    };
  }
  window.onclick = (event) => {
    if (event.target == modal) {
      modal.style.display = 'none';
      document.body.style.overflow = '';
    }
  };

  load();
})();
