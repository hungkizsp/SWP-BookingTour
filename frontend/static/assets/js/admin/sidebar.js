document.addEventListener('DOMContentLoaded', () => {
    const currentPath = window.location.pathname.split('/').pop();
    
    const navItems = [
        { href: './dashboard.html', text: 'Dashboard', activeMatch: ['dashboard.html'] },
        { href: './manageTours.html', text: 'Tours', activeMatch: ['manageTours.html', 'editTour.html', 'createrTour.html'] },
        { href: './manageCategories.html', text: 'Categories', activeMatch: ['manageCategories.html', 'manageCatelories.html'] },
        { href: './manageBookings.html', text: 'Bookings', activeMatch: ['manageBookings.html'] },
        { href: './manageUsers.html', text: 'Users', activeMatch: ['manageUsers.html'] },
        { href: './manageReview.html', text: 'Reviews', activeMatch: ['manageReview.html'] },
        { href: './manageNewsletter.html', text: 'Newsletters', activeMatch: ['manageNewsletter.html'] },
        { href: './financialReport.html', text: 'Financial Report', activeMatch: ['financialReport.html'] },
        { href: './manageDiscounts.html', text: 'Manage Discount', activeMatch: ['manageDiscounts.html'] },
        { href: './manageVouchers.html', text: 'Vouchers', activeMatch: ['manageVouchers.html'] },
        { href: './chat-escalations.html', text: 'Support Chat', activeMatch: ['chat-escalations.html'] }
    ];

    let navHtml = '';
    navItems.forEach(item => {
        const isActive = item.activeMatch.includes(currentPath) ? 'active' : '';
        navHtml += `<a href="${item.href}" class="nav-item ${isActive}">${item.text}</a>`;
    });

    const sidebarHtml = `
        <a href="./dashboard.html" class="logo">TourBooking</a>
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
