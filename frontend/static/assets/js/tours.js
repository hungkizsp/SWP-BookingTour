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
  let wishlistIds = new Set();

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

      const isWished = wishlistIds.has(t.id);

      card.innerHTML = `
        <div class="tour-card-img-wrapper" style="position:relative;">
          <img src="${TB.normalizeImageUrl(t.imageUrl || (t.imageUrls && t.imageUrls[0]) || 'https://danangbest.com/vnt_upload/tour/04_2023/banahill_4.jpg')}" class="tour-card-img" alt="${t.tourName}">
          <div class="tour-card-badge">✨ ${t.categoryName || 'Sản phẩm nổi bật'}</div>
          <button
            class="wishlist-btn"
            data-tour-id="${t.id}"
            title="${isWished ? 'Bỏ khỏi yêu thích' : 'Lưu vào yêu thích'}"
            style="position:absolute; top:10px; right:10px; background:white; border:none; border-radius:50%; width:36px; height:36px; cursor:pointer; box-shadow:0 2px 6px rgba(0,0,0,0.15); font-size:1.1rem; display:flex; align-items:center; justify-content:center; z-index:10; transition:transform 0.15s;"
          >${isWished ? '❤️' : '🤍'}</button>
        </div>
        <div class="tour-card-body">
          <h3 class="tour-card-title">${t.tourName}</h3>
          <div class="tour-card-meta">
            <span>⏳ ${t.duration ? t.duration + ' Ngày' : 'Liên hệ'}</span>
            <span>📍 ${t.startLocation || 'Đà Nẵng'}</span>
            <span>⭐ ${(t.rating || 5).toFixed(1)} Rating</span>
            <span>😌 ${t.transportType || 'Xe du lịch'}</span>
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
    attachWishlistListeners();
  }

  function updatePagination(data) {
    if (!data) return;
    const page = Number(data.page ?? 0);
    const total = Number(data.totalPages ?? 1);
    if (pageInfo) pageInfo.textContent = `${page + 1} / ${total || 1}`;
    if (totalInfo) totalInfo.textContent = `${data.totalElements || 0} tour được tìm thấy`;
    if (prevBtn) prevBtn.disabled = (page === 0);
    if (nextBtn) nextBtn.disabled = (page + 1 >= total);
  }

  function attachWishlistListeners() {
    document.querySelectorAll('.wishlist-btn').forEach(btn => {
      btn.addEventListener('mouseenter', () => { btn.style.transform = 'scale(1.2)'; });
      btn.addEventListener('mouseleave', () => { btn.style.transform = 'scale(1)'; });
      btn.onclick = async (e) => {
        e.preventDefault();
        e.stopPropagation();
        const userStr = sessionStorage.getItem('user') || localStorage.getItem('user');
        if (!userStr) {
          alert('Vui lòng đăng nhập để lưu tour yêu thích!');
          return;
        }
        const user = JSON.parse(userStr);
        const tourId = Number(btn.dataset.tourId);
        const prev = btn.textContent;
        btn.textContent = '...';
        btn.disabled = true;
        try {
          const res = await TB.apiFetch(`/api/v1/wishlist/toggle?userId=${user.id}&tourId=${tourId}`, { method: 'POST' });
          const data = res.data || res;
          if (data.isAdded) {
            wishlistIds.add(tourId);
            btn.textContent = '❤️';
            btn.title = 'Bỏ khỏi yêu thích';
          } else {
            wishlistIds.delete(tourId);
            btn.textContent = '🤍';
            btn.title = 'Lưu vào yêu thích';
          }
        } catch (err) {
          btn.textContent = prev;
          alert('Lỗi: ' + err.message);
        } finally {
          btn.disabled = false;
        }
      };
    });
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
      // Increase size to 100 to get all categories for the filter
      const res = await TB.apiFetch('/api/v1/categories?size=100');
      const catsBody = document.getElementById('categoryFilters');
      const cats = res.data?.content || [];
      if (catsBody) {
        catsBody.innerHTML = `
          <option value="">Tất cả danh mục</option>
          ${cats.map(c => `
            <option value="${c.id}">${c.categoryName}</option>
          `).join('')}
        `;
        if (state.categoryId) catsBody.value = state.categoryId;
      }
    } catch (err) {
      console.error('Failed to load categories', err);
    }
  }

  async function initWishlist() {
    const userStr = sessionStorage.getItem('user') || localStorage.getItem('user');
    if (!userStr) return;
    const user = JSON.parse(userStr);
    try {
      const res = await TB.apiFetch(`/api/v1/wishlist?userId=${user.id}&page=0&size=200`);
      const pd = res.data || res;
      if (pd.content) pd.content.forEach(t => wishlistIds.add(t.id));
    } catch (e) {}
  }

  updateCompareBadge();
  fetchCategories();
  initWishlist().then(() => fetchTours());
})();
