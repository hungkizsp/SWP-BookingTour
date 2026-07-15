document.addEventListener('DOMContentLoaded', () => {
    const currentPath = window.location.pathname.split('/').pop();
    
    const navGroups = [
        {
            title: "LỊCH TRÌNH & CÔNG VIỆC",
            items: [
                { href: './dashboard.html', text: 'Tour của tôi', icon: '&#x1F4C5;', activeMatch: ['dashboard.html', 'schedule-detail.html'] }
            ]
        },
        {
            title: "TÀI KHOẢN",
            items: [
                { href: './profile.html', text: 'Hồ sơ cá nhân', icon: '&#x1F464;', activeMatch: ['profile.html'] }
            ]
        }
    ];

    let navHtml = '';
    navGroups.forEach(group => {
        navHtml += `<div class="sidebar-group-title">${group.title}</div>`;
        group.items.forEach(item => {
            const isActive = item.activeMatch.includes(currentPath) ? 'active' : '';
            navHtml += `<a href="${item.href}" class="nav-item ${isActive}">
                            <span class="nav-icon">${item.icon}</span>
                            <span>${item.text}</span>
                        </a>`;
        });
    });

    const sidebarHtml = `
        <a href="./dashboard.html" class="logo">
            <span style="font-size: 1.8rem; line-height: 1;">🌊</span>
            <span>Guide Portal</span>
        </a>
        <nav>
            ${navHtml}
        </nav>
    `;

    // Try to find an existing sidebar element
    let sidebarEl = document.querySelector('.sidebar');
    
    // If found, replace its innerHTML. Otherwise, prepend it to admin-layout.
    if (sidebarEl) {
        sidebarEl.innerHTML = sidebarHtml;
    } else {
        const layoutEl = document.querySelector('.admin-layout');
        if (layoutEl) {
            sidebarEl = document.createElement('aside');
            sidebarEl.className = 'sidebar';
            sidebarEl.innerHTML = sidebarHtml;
            layoutEl.insertBefore(sidebarEl, layoutEl.firstChild);
        }
    }
});
