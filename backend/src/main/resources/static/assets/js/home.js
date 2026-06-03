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
          window.location.href = `./tours.html?categoryId=${cat.id}`;
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
        
        card.innerHTML = `
          <div class="thumb">Tour</div>
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

  // Initialize
  loadCategories();
  loadFeaturedTours();
})();
