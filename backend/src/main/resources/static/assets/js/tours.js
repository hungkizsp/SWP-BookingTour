(() => {
  const grid = document.getElementById('tourGrid');
  const compareBar = document.getElementById('compareBar');
  const prevBtn = document.getElementById('prevBtn');
  const nextBtn = document.getElementById('nextBtn');
  const pageInfo = document.getElementById('pageInfo');
  const totalInfo = document.getElementById('totalInfo');
  const applyBtn = document.getElementById('applyFilters');
  const compareCountEl = document.getElementById('compareCount');

  const params = new URLSearchParams(window.location.search);
  let state = {
    page: 0,
    size: 9,
    keyword: params.get('keyword') || '',
    minPrice: '',
    maxPrice: '',
    categoryId: params.get('cat') || '',
    sortBy: 'price',
    selected: new Set(JSON.parse(localStorage.getItem('compareIds') || '[]'))
  };

  const pRange = params.get('price');
  if (pRange && pRange !== 'all') {
    if (pRange === '3000000-plus') {
      state.minPrice = '3000000';
      state.maxPrice = '';
    } else {
      const [min, max] = pRange.split('-');
      state.minPrice = min;
      state.maxPrice = max;
    }
    const priceSelect = document.getElementById('priceRange');
    if (priceSelect) priceSelect.value = pRange;
  }

  // Pre-fill search input if keyword exists
  const searchInput = document.getElementById('searchInput');
  if (searchInput && state.keyword) {
    searchInput.value = state.keyword;
  }

  function updateCompareBadge() {
    compareCountEl.textContent = state.selected.size;
    compareBar.style.display = state.selected.size > 0 ? 'flex' : 'none';
    localStorage.setItem('compareIds', JSON.stringify([...state.selected]));
  }

  async function fetchTours() {
    const params = new URLSearchParams({
      page: state.page,
      size: state.size,
      sortBy: state.sortBy,
      sortDir: state.sortBy === 'price' ? 'asc' : 'desc'
    });
    if (state.keyword) params.append('keyword', state.keyword);
    if (state.minPrice) params.append('minPrice', state.minPrice);
    if (state.maxPrice) params.append('maxPrice', state.maxPrice);
    if (state.categoryId) params.append('categoryId', state.categoryId);

    try {
      grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 100px;"><div class="loader"></div><p style="margin-top:20px; color:var(--text-faint);">Đang tìm kiếm hành trình cho bạn...</p></div>';
      const res = await TB.apiFetch(`/api/v1/tours/browse?${params.toString()}`);
      renderTours(res);
      updatePagination(res.data);
    } catch (err) {
      grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: var(--price); padding: 50px;">Lỗi khi tải dữ liệu tour. Vui lòng thử lại.</div>';
    }
  }

  function renderTours(pageRes) {
    grid.innerHTML = '';
    const content = pageRes?.data?.content || [];
    if (content.length === 0) {
      grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 100px; color: var(--text-faint);">Không tìm thấy tour phù hợp với yêu cầu của bạn.</div>';
      return;
    }

    content.forEach((t, idx) => {
      const card = document.createElement('div');
      card.className = 'tour-card reveal';
      card.style.animationDelay = `${(idx % 6) * 0.1}s`;

      const priceHtml = t.price
        ? `<span class="price-value">${Number(t.price).toLocaleString()}đ</span>`
        : '<span class="price-value">Liên hệ</span>';

      card.innerHTML = `
        <div class="tour-card-img-wrapper">
          <img src="${t.imageUrl || (t.imageUrls && t.imageUrls[0]) || 'https://danangbest.com/vnt_upload/tour/04_2023/banahill_4.jpg'}" class="tour-card-img" alt="${t.tourName}">
          <div class="tour-card-badge">✨ ${t.categoryName || 'Sản phẩm nổi bật'}</div>
        </div>
        <div class="tour-card-body">
          <h3 class="tour-card-title">${t.tourName}</h3>
          <div class="tour-card-meta">
            <span>⌛ ${t.duration ? t.duration + ' Ngày' : 'Liên hệ'}</span>
            <span>📍 ${t.startLocation || 'Đà Nẵng'}</span>
            <span>⭐ ${(t.rating || 5).toFixed(1)} Rating</span>
            <span>🚌 ${t.transportType || 'Xe du lịch'}</span>
          </div>
          <div class="tour-card-footer">
            <div class="tour-card-price">
              <span class="price-label">Giá chỉ từ:</span>
              ${t.price ? `<span class="price-value">${Number(t.price).toLocaleString()}</span><span class="price-currency">VNĐ</span>` : '<span class="price-value">Liên hệ</span>'}
            </div>
            <div style="display: flex; gap: 10px;">
               <button class="btn btn-secondary compare-btn" data-id="${t.id}" style="padding: 0; min-height: 48px; width: 48px; border-radius: 12px; font-size: 1.2rem;" title="So sánh">⚖️</button>
               <a href="./tour-detail.html?id=${t.id}" class="btn" style="padding: 0 25px; min-height: 48px; font-size: 0.9rem; border-radius: 12px;">XEM CHI TIẾT</a>
            </div>
          </div>
        </div>
      `;
      grid.appendChild(card);
    });

    // Re-init reveal observer
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) entry.target.classList.add('visible');
      });
    }, { threshold: 0.1 });
    document.querySelectorAll('.reveal').forEach(el => observer.observe(el));

    attachCompareListeners();
  }

  function updatePagination(data) {
    if (pageInfo) pageInfo.textContent = `${data.number + 1} / ${data.totalPages || 1}`;
    if (totalInfo) totalInfo.textContent = `${data.totalElements} tour được tìm thấy`;
    if (prevBtn) prevBtn.disabled = data.first;
    if (nextBtn) nextBtn.disabled = data.last;
  }

  function attachCompareListeners() {
    document.querySelectorAll('.compare-btn').forEach(btn => {
      const id = btn.dataset.id;
      if (state.selected.has(id)) btn.style.background = 'var(--accent-soft)';

      btn.onclick = () => {
        if (state.selected.has(id)) {
          state.selected.delete(id);
          btn.style.background = '';
        } else {
          if (state.selected.size >= 3) {
            alert('Bạn chỉ có thể chọn tối đa 3 tour để so sánh.');
            return;
          }
          state.selected.add(id);
          btn.style.background = 'var(--accent-soft)';
        }
        updateCompareBadge();
      };
    });
  }

  if (prevBtn) prevBtn.onclick = () => { if (state.page > 0) { state.page--; fetchTours(); window.scrollTo(0, 0); } };
  if (nextBtn) nextBtn.onclick = () => { state.page++; fetchTours(); window.scrollTo(0, 0); };

  if (applyBtn) applyBtn.onclick = () => {
    state.keyword = document.getElementById('searchInput')?.value || '';
    state.categoryId = document.getElementById('categoryFilters')?.value || '';
    
    const pRange = document.getElementById('priceRange')?.value || 'all';
    if (pRange === 'all') {
      state.minPrice = '';
      state.maxPrice = '';
    } else if (pRange === '3000000-plus') {
      state.minPrice = '3000000';
      state.maxPrice = '';
    } else {
      const [min, max] = pRange.split('-');
      state.minPrice = min;
      state.maxPrice = max;
    }

    state.page = 0;
    fetchTours();
  };

  async function fetchCategories() {
    try {
      const res = await TB.apiFetch('/api/v1/categories');
      const catsBody = document.getElementById('categoryFilters');
      if (res.data && catsBody) {
        catsBody.innerHTML = `
          <option value="">Tất cả danh mục</option>
          ${res.data.map(c => `
            <option value="${c.id}">${c.categoryName}</option>
          `).join('')}
        `;
        if (state.categoryId) catsBody.value = state.categoryId;
      }
    } catch (err) {
      console.error('Failed to load categories', err);
    }
  }

  updateCompareBadge();
  fetchCategories();
  fetchTours();
})();
