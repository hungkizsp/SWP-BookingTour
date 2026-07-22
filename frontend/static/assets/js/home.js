(() => {
  const categoryList = document.getElementById('categoryList');
  const featuredTours = document.getElementById('featuredTours');

  async function loadCategories() {
    try {
      const res = await TB.apiFetch('/api/v1/categories');
      const categories = res.data || [];
      if (categories.length === 0) {
        categoryList.innerHTML = '<span class="pill">No categories found</span>';
        return;
      }
      categoryList.innerHTML = '';
      categories.forEach(cat => {
        const span = document.createElement('span');
        span.className = 'pill';
        span.style.cursor = 'pointer';
        span.innerHTML = `<strong>${escapeHtml(cat.name)}</strong>`;
        span.onclick = () => {
          window.location.href = `./tours.html?cat=${cat.id}`;
        };
        categoryList.appendChild(span);
      });
    } catch (err) {
      categoryList.innerHTML = '<span class="pill">Error loading categories</span>';
    }
  }

  async function loadFeaturedTours() {
    try {
      // Fetch latest tours (using page 0, size 3 for featured section)
      const res = await TB.apiFetch('/api/v1/tours/browse?page=0&size=3&sortBy=createdAt&sortDir=desc');
      const tours = res.data?.content || [];
      if (tours.length === 0) {
        featuredTours.innerHTML = '<div class="card empty-state">No featured tours available at the moment.</div>';
        return;
      }
      featuredTours.innerHTML = '';
      tours.forEach(t => {
        const card = document.createElement('div');
        card.className = 'card';
        card.style.transition = 'transform 0.3s ease, box-shadow 0.3s ease';
        card.onmouseover = () => { card.style.transform = 'translateY(-6px)'; card.style.boxGuards = 'var(--shadow-soft)'; };
        card.onmouseout = () => { card.style.transform = 'translateY(0)'; card.style.boxGuards = 'var(--shadow-card)'; };
        
        const heartIcon = userWishlist.has(t.id) ? '❤️' : '🤍';
        card.innerHTML = `
          <div class="thumb" style="position:relative;">
            Tour
            <button onclick="toggleWishlist(${t.id}, this)" style="position:absolute; top:10px; right:10px; background:white; border:none; border-radius:50%; width:32px; height:32px; cursor:pointer; box-shadow:0 2px 4px rgba(0,0,0,0.1); font-size:16px; display:flex; align-items:center; justify-content:center; z-index:10;">${heartIcon}</button>
          </div>
          <h3 class="title" style="font-size:1.2rem;margin-bottom:12px;">${escapeHtml(t.tourName)}</h3>
          <div class="meta" style="margin-bottom:18px;">
            <span><strong>$${t.price}</strong></span>
            <span>⭐ ${t.rating ? t.rating.toFixed(1) : '0.0'}</span>
          </div>
          <a class="btn btn-secondary" href="./tour-detail.html?id=${t.id}" style="width:100%;">View Details</a>
        `;
        featuredTours.appendChild(card);
      });
    } catch (err) {
      featuredTours.innerHTML = '<div class="card empty-state">Error loading featured tours.</div>';
    }
  }

  function escapeHtml(s) {
    return String(s)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }

  let userWishlist = new Set();
  
  async function initWishlist() {
    const userStr = sessionStorage.getItem('user');
    if (!userStr) return;
    const user = JSON.parse(userStr);
    try {
      const res = await TB.apiFetch(`/api/v1/wishlist?userId=${user.id}&page=0&size=100`);
      const pageData = res.data || res;
      if (pageData.content) {
         pageData.content.forEach(t => userWishlist.add(t.id));
      }
    } catch(e) {}
  }

  window.toggleWishlist = async function(tourId, btn) {
    event.stopPropagation();
    event.preventDefault();
    const userStr = sessionStorage.getItem('user');
    if (!userStr) {
      alert("Vui lòng đăng nhập để lưu tour yêu thích!");
      return;
    }
    const user = JSON.parse(userStr);
    const originalText = btn.innerText;
    btn.innerText = '...';
    try {
      const res = await TB.apiFetch(`/api/v1/wishlist/toggle?userId=${user.id}&tourId=${tourId}`, { method: 'POST' });
      const data = res.data || res;
      if (data.isAdded) {
        btn.innerText = '❤️';
        userWishlist.add(tourId);
      } else {
        btn.innerText = '🤍';
        userWishlist.delete(tourId);
      }
    } catch(e) {
      alert('Lỗi lưu tour: ' + e.message);
      btn.innerText = originalText;
    }
  };

  // Initialize
  initWishlist().then(() => {
    loadCategories();
    loadFeaturedTours();
  });
})();
