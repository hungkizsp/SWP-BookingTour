document.addEventListener('DOMContentLoaded', () => {
    const currentPath = window.location.pathname.split('/').pop();
    
    const navGroups = [
        {
            title: "TỔNG QUAN",
            items: [
                { href: './dashboard.html', text: 'Dashboard', icon: '📊', activeMatch: ['dashboard.html'] }
            ]
        },
        {
            title: "ĐIỀU HÀNH & TOUR",
            items: [
                { href: './manageTours.html', text: 'Tours', icon: '🗺️', activeMatch: ['manageTours.html', 'editTour.html', 'createrTour.html'] },
                { href: './manageCategories.html', text: 'Categories', icon: '🏷️', activeMatch: ['manageCategories.html', 'manageCatelories.html'] }
            ]
        },
        {
            title: "KHÁCH HÀNG & GIAO DỊCH",
            items: [
                { href: './manageBookings.html', text: 'Bookings', icon: '📅', activeMatch: ['manageBookings.html'] },
                { href: './manageUsers.html', text: 'Users', icon: '👥', activeMatch: ['manageUsers.html'] },
                { href: './manageReview.html', text: 'Reviews', icon: '⭐', activeMatch: ['manageReview.html'] },
                { href: './chat-escalations.html', text: 'Support Chat', icon: '💬', activeMatch: ['chat-escalations.html'] }
            ]
        },
        {
            title: "MARKETING & TÀI CHÍNH",
            items: [
                { href: './manageDiscounts.html', text: 'Discounts', icon: '🔥', activeMatch: ['manageDiscounts.html'] },
                { href: './manageVouchers.html', text: 'Vouchers', icon: '🎟️', activeMatch: ['manageVouchers.html'] },
                { href: './manageNewsletter.html', text: 'Newsletters', icon: '📧', activeMatch: ['manageNewsletter.html'] },
                { href: './financialReport.html', text: 'Financial Report', icon: '💰', activeMatch: ['financialReport.html'] }
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
            <span>TourBooking</span>
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

    // Inject Critical Alerts Component dynamically
    if (!document.querySelector('script[src="/assets/js/critical-alerts.js"]')) {
        const script = document.createElement('script');
        script.src = '/assets/js/critical-alerts.js';
        document.head.appendChild(script);
    }
});
