document.addEventListener('DOMContentLoaded', () => {
    const currentPath = window.location.pathname.split('/').pop();
    
    const navGroups = [
        {
            title: "TỔNG QUAN",
            items: [
                { href: './dashboard.html', text: 'Dashboard', icon: '&#x1F4CA;', activeMatch: ['dashboard.html'] }
            ]
        },
        {
            title: "GIAO DỊCH & BOOKING",
            items: [
                { href: './manageBookings.html', text: 'Manage Bookings', icon: '&#x1F4C5;', activeMatch: ['manageBookings.html'] },
                { href: './manageRefunds.html', text: 'Manage Refunds', icon: '&#x1F4B8;', activeMatch: ['manageRefunds.html'] }
            ]
        },
        {
            title: "ĐIỀU HÀNH TOUR",
            items: [
                { href: './manageSchedules.html', text: 'Manage Schedules', icon: '&#x1F5FA;', activeMatch: ['manageSchedules.html'] }
            ]
        },
        {
            title: "HỖ TRỢ KHÁCH HÀNG",
            items: [
                { href: './chat-escalations.html', text: 'Chat / Escalations', icon: '&#x1F4AC;', activeMatch: ['chat-escalations.html'] }
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
            <span>Staff Portal</span>
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
