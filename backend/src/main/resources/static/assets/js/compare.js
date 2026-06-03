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

  async function load() {
    try {
      const idString = ids.join(',');
      const res = await TB.apiFetch(`/api/v1/tours/compare?ids=${idString}`);
      const tours = res.data || [];

      if (tours.length === 0) {
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
          ${tours.map(t => `
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
            ${tours.map(t => `<td>${contentFn(t)}</td>`).join('')}
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
        <ul style="margin: 0; padding-left: 18px; font-size: 0.85rem; color: var(--text-soft); line-height: 1.6;">
          ${(t.highlights || []).map(h => `<li>${h}</li>`).join('') || '<li>Đang cập nhật...</li>'}
        </ul>
      `);

      bodyHtml += renderRow('Giới thiệu hành trình', t => {
        if (!t.itinerary) return '<div class="itinerary-summary">Đang cập nhật nội dung...</div>';
        try {
          // Thử parse nếu là JSON
          const items = JSON.parse(t.itinerary);
          if (Array.isArray(items)) {
            return `
              <div style="font-size: 0.82rem; line-height: 1.6;">
                ${items.slice(0, 3).map(item => `
                  <div style="margin-bottom: 8px;">
                    <div style="font-weight: 800; color: var(--primary-dark);">${item.title}</div>
                    <div style="color: var(--text-soft);">${item.content}</div>
                  </div>
                `).join('')}
                ${items.length > 3 ? '<div style="color: var(--accent); font-weight: 700; font-size: 0.75rem;">... Xem thêm chi tiết</div>' : ''}
              </div>
            `;
          }
        } catch (e) {
          // Trả về text thường nếu không phải JSON hoặc lỗi parse
          return `<div class="itinerary-summary">${t.itinerary.substring(0, 200)}...</div>`;
        }
        return `<div class="itinerary-summary">${t.itinerary}</div>`;
      });

      bodyHtml += renderRow('Lịch khởi hành & Khung giờ', t => `
        <div style="margin-bottom: 8px; font-size: 0.8rem; font-weight: 600; color: var(--text-soft);">Các ngày gần nhất:</div>
        <div>
          ${(t.schedules || []).slice(0, 4).map(s => `<span class="schedule-tag">${new Date(s.startDate).toLocaleDateString('vi-VN')}</span>`).join('') || '<span style="font-size:0.8rem;">Liên hệ để biết lịch</span>'}
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

  load();
})();
