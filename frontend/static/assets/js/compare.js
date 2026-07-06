(() => {
  const ids = JSON.parse(sessionStorage.getItem('compareIds') || '[]');
  const gridWrapper = document.getElementById('gridWrapper');
  const tableHead = document.getElementById('tableHead');
  const tableBody = document.getElementById('tableBody');
  const empty = document.getElementById('empty');

  function escapeHtml(s) {
    return String(s || '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }

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
        sessionStorage.removeItem('compareIds');
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
                <img src="${TB.normalizeImageUrl((t.imageUrls && t.imageUrls[0]) || t.imageUrl || '')}" 
                     style="width:100%; height:100%; object-fit:cover; display: block;"
                     onerror="this.src='https://images.unsplash.com/photo-1552074284-5e88ef1aef18?auto=format&fit=crop&w=800'">
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
      
      // Find extremes for highlighting
      let minPricePerDay = null;
      let maxRating = null;

      currentTours.forEach(t => {
        if (t.pricePerDay != null) {
          if (minPricePerDay === null || t.pricePerDay < minPricePerDay) {
            minPricePerDay = t.pricePerDay;
          }
        }
        if (t.rating != null) {
          if (maxRating === null || t.rating > maxRating) {
            maxRating = t.rating;
          }
        }
      });

      bodyHtml += renderRow('💰 Giá', t => `
        <div style="font-weight: 800; color: var(--price); font-size: 1.1rem;">
          ${t.price ? Number(t.price).toLocaleString() + 'đ' : 'Liên hệ'}
        </div>
      `);

      bodyHtml += renderRow('📅 Số ngày', t => `
        <div style="font-weight: 600;">${t.duration ? t.duration + ' ngày' : '—'}</div>
      `);

      bodyHtml += renderRow('💵 Giá/ngày', t => {
        const val = t.pricePerDay;
        const isMin = val !== null && val === minPricePerDay;
        const bg = isMin ? '#dcfce3' : 'transparent';
        const color = isMin ? '#166534' : 'inherit';
        const display = val ? Number(val).toLocaleString() + 'đ' : '—';
        return `<div style="background: ${bg}; color: ${color}; padding: 8px; border-radius: 6px; font-weight: ${isMin ? '700' : 'normal'}; display: inline-block;">${display}</div>`;
      });

      bodyHtml += renderRow('⭐ Điểm đánh giá', t => {
        const val = t.rating || 0;
        const isMax = val > 0 && val === maxRating;
        const bg = isMax ? '#fef3c7' : 'transparent';
        const color = isMax ? '#92400e' : 'inherit';
        return `<div style="background: ${bg}; color: ${color}; padding: 8px; border-radius: 6px; font-weight: ${isMax ? '700' : 'normal'}; display: inline-block;">
          ${val.toFixed(1)} (${t.reviewCount || 0})
        </div>`;
      });

      bodyHtml += renderRow('🚌 Phương tiện', t => `
        <div>${escapeHtml(t.transportType || 'Đang cập nhật')}</div>
      `);

      bodyHtml += renderRow('🍽️ Bữa ăn', t => `
        <div>${escapeHtml(t.meals || 'Đang cập nhật')}</div>
      `);

      bodyHtml += renderRow('🏨 Chỗ ở', t => `
        <div>${escapeHtml(t.accommodation || 'Đang cập nhật')}</div>
      `);

      bodyHtml += renderRow('👥 Sức chứa', t => `
        <div>${t.maxGroupSize ? t.maxGroupSize + ' người' : '—'}</div>
      `);

      bodyHtml += renderRow('📍 Điểm khởi hành', t => `
        <div>${escapeHtml(t.startLocation || 'Đà Nẵng')}</div>
      `);

      bodyHtml += renderRow('🎯 Còn chỗ', t => {
        const slots = t.closestScheduleSlots || 0;
        const isSoldOut = slots === 0;
        const bg = isSoldOut ? '#fee2e2' : 'transparent';
        const color = isSoldOut ? '#dc2626' : (slots > 0 ? '#10b981' : 'inherit');
        const display = isSoldOut ? 'Hết chỗ' : `${slots} chỗ`;
        return `<div style="background: ${bg}; color: ${color}; padding: 8px; border-radius: 6px; font-weight: 700; display: inline-block;">${display}</div>`;
      });

      bodyHtml += renderRow('', t => {
        const slots = t.closestScheduleSlots || 0;
        if (slots > 0) {
          return `<a href="./tour-detail.html?id=${t.id}" class="btn" style="width:100%; height:44px; border-radius:10px; font-size: 0.8rem;">ĐẶT NGAY</a>`;
        } else {
          return `<a href="./tour-detail.html?id=${t.id}" class="btn btn-secondary" style="width:100%; height:44px; border-radius:10px; font-size: 0.8rem; opacity: 0.6;">XEM CHI TIẾT</a>`;
        }
      });

      tableBody.innerHTML = bodyHtml;

      // 4. Handle Remove
      document.querySelectorAll('.remove-btn-table').forEach(btn => {
        btn.onclick = () => {
          const idToRemove = String(btn.dataset.id);
          const currentIds = JSON.parse(sessionStorage.getItem('compareIds') || '[]');
          const newIds = currentIds.map(String).filter(id => id !== idToRemove);
          sessionStorage.setItem('compareIds', JSON.stringify(newIds));
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
  window.showFullItinerary = async (tourId) => {
    const tour = currentTours.find(t => t.id == tourId);
    if (!tour) return;
    
    const modal = document.getElementById('itineraryModal');
    const modalBody = document.getElementById('modalBody');
    const modalTitle = document.getElementById('modalTitle');
    modalTitle.textContent = `Lịch trình chi tiết: ${tour.tourName}`;
    
    if (tour.itinerary === 'NEW_API' || tour.itineraryDaysCount > 0) {
      try {
        const res = await TB.apiFetch(`/api/v1/tours/${tourId}/itinerary`);
        const items = res.data;
        if (!items || items.length === 0) {
          modalBody.innerHTML = '<p>Đang cập nhật lịch trình...</p>';
        } else {
          modalBody.innerHTML = items.map(item => `
            <div class="itinerary-item-full">
              <div class="time-title">Ngày ${item.dayNumber}: ${escapeHtml(item.title)}</div>
              <div class="content-text">${escapeHtml(item.description)}</div>
            </div>
          `).join('');
        }
        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
        return;
      } catch (e) {
        console.error('Failed to fetch new itinerary', e);
      }
    }

    try {
      const items = JSON.parse(tour.itinerary);
      modalBody.innerHTML = items.map(item => `
        <div class="itinerary-item-full">
          <div class="time-title">${escapeHtml(item.title)}</div>
          <div class="content-text">${escapeHtml(item.content)}</div>
        </div>
      `).join('');

      modal.style.display = 'flex';
      document.body.style.overflow = 'hidden';
    } catch (e) {
      console.error('Failed to parse itinerary', e);
      modalBody.innerHTML = '<p>Đang cập nhật lịch trình...</p>';
      modal.style.display = 'flex';
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
