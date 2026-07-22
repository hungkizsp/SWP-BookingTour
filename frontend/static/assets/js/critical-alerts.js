
// =========================================================================
// Critical Guide Alerts Component
// Injected into the top of the main-content on dashboards and manage pages
// =========================================================================

document.addEventListener('DOMContentLoaded', () => {
    initCriticalAlerts();
});

async function initCriticalAlerts() {
    // Check if we are on a page where this should be displayed
    const allowedPages = ['dashboard.html', 'manageBookings.html', 'manageSchedules.html'];
    const currentPath = window.location.pathname.split('/').pop();
    if (!allowedPages.includes(currentPath)) return;

    // Create the container
    const container = document.createElement('div');
    container.id = 'critical-alerts-container';
    
    // Insert at the top of the main content area, right after the header
    const mainContent = document.querySelector('.main-content');
    if (!mainContent) return;
    
    const header = mainContent.querySelector('header');
    if (header && header.nextSibling) {
        mainContent.insertBefore(container, header.nextSibling);
    } else {
        mainContent.prepend(container);
    }

    // Initial fetch
    await fetchAndRenderCriticalAlerts();

    // Poll every 1 minute
    setInterval(fetchAndRenderCriticalAlerts, 60000);
}

async function fetchAndRenderCriticalAlerts() {
    const container = document.getElementById('critical-alerts-container');
    if (!container) return;

    try {
        const res = await TB.apiFetch('/api/v1/admin/critical-guide-alerts');
        const alerts = res || [];

        if (alerts.length === 0) {
            container.innerHTML = '';
            container.style.display = 'none';
            return;
        }

        container.style.display = 'block';
        container.style.marginBottom = '1.5rem';
        container.style.marginTop = '1.5rem';

        let html = `
            <div style="background-color: #fef2f2; border: 2px solid #dc2626; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(220, 38, 38, 0.1);">
                <div style="background-color: #dc2626; color: white; padding: 0.75rem 1.25rem; font-weight: bold; font-size: 1.1rem; display: flex; align-items: center; gap: 0.5rem;">
                    <span>🚨</span> KHẨN CẤP: ${alerts.length} TOUR CÓ KHÁCH NHƯNG THIẾU GUIDE!
                </div>
                <div style="padding: 1rem;">
                    <p style="margin-top: 0; margin-bottom: 1rem; color: #7f1d1d; font-size: 0.9rem;">
                        Các tour dưới đây đã có khách đặt nhưng hiện tại chưa được phân công Hướng dẫn viên và sắp (hoặc đã) đến giờ khởi hành. Cần xử lý ngay!
                    </p>
                    <table style="width: 100%; border-collapse: collapse; font-size: 0.9rem; text-align: left;">
                        <thead>
                            <tr style="border-bottom: 1px solid #fca5a5;">
                                <th style="padding: 0.5rem; color: #991b1b;">Schedule</th>
                                <th style="padding: 0.5rem; color: #991b1b;">Tour Name</th>
                                <th style="padding: 0.5rem; color: #991b1b;">Departure</th>
                                <th style="padding: 0.5rem; color: #991b1b;">Bookings</th>
                                <th style="padding: 0.5rem; color: #991b1b;">Total Value</th>
                                <th style="padding: 0.5rem; color: #991b1b;">Time</th>
                                <th style="padding: 0.5rem; color: #991b1b; text-align: right;">Hành động</th>
                            </tr>
                        </thead>
                        <tbody>
        `;

        alerts.forEach(a => {
            const isPast = a.minutesRemaining <= 0;
            const timeClass = isPast ? 'color: #dc2626; font-weight: bold;' : 'color: #b45309; font-weight: bold;';
            
            let timeText = '';
            if (isPast) {
                const daysLate = Math.floor(Math.abs(a.minutesRemaining) / 1440);
                if (daysLate > 0) {
                    timeText = `Đã trễ ${daysLate} ngày!`;
                } else {
                    const hoursLate = Math.floor(Math.abs(a.minutesRemaining) / 60);
                    timeText = `Đã trễ ${hoursLate} giờ!`;
                }
            } else {
                const hoursLeft = Math.floor(a.minutesRemaining / 60);
                timeText = `Còn ${hoursLeft} giờ`;
            }

            // Determine if the user is staff or admin to format the refund link
            const isStaff = window.location.pathname.includes('/staff/');
            const assignGuideUrl = '#'; // We can trigger the modal directly if available, else link to manage bookings
            const refundUrl = isStaff ? './manageRefunds.html' : './manageBookings.html'; 
            
            // Format money
            const valueStr = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(a.totalValue);
            
            // For Assign Guide, if the page has openAssignGuideModal (like manageBookings), call it.
            // Otherwise, direct them to manageBookings
            let assignBtn = '';
            if (typeof openAssignGuideModal === 'function') {
                assignBtn = `<button class="btn btn-primary" style="padding: 0.3rem 0.6rem; font-size: 0.8rem; background-color: #dc2626; border-color: #dc2626; color: white;" 
                             onclick="openAssignGuideModal(${a.scheduleId}, ${a.activeBookings}, '${a.departureDateTime}')">
                             Phân công Guide
                             </button>`;
            } else {
                const path = isStaff ? '/pages/staff/manageBookings.html' : '/pages/admin/manageBookings.html';
                assignBtn = `<button class="btn btn-primary" style="padding: 0.3rem 0.6rem; font-size: 0.8rem; background-color: #dc2626; border-color: #dc2626; color: white;" 
                             onclick="window.location.href='${path}'">
                             Đến trang Bookings
                             </button>`;
            }

            html += `
                <tr style="border-bottom: 1px dashed #fecaca; background-color: ${isPast ? '#fef2f2' : 'transparent'};">
                    <td style="padding: 0.75rem 0.5rem; font-weight: 600; color: #7f1d1d;">SD-${a.scheduleId}</td>
                    <td style="padding: 0.75rem 0.5rem; color: #7f1d1d; max-width: 200px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" title="${a.tourName}">${a.tourName}</td>
                    <td style="padding: 0.75rem 0.5rem; color: #7f1d1d;">${a.departureDateTime ? new Date(a.departureDateTime).toLocaleString('vi-VN') : 'N/A'}</td>
                    <td style="padding: 0.75rem 0.5rem; color: #7f1d1d;"><strong>${a.activeBookings}</strong> khách</td>
                    <td style="padding: 0.75rem 0.5rem; color: #7f1d1d;">${valueStr}</td>
                    <td style="padding: 0.75rem 0.5rem; ${timeClass}">${timeText}</td>
                    <td style="padding: 0.75rem 0.5rem; text-align: right; display: flex; gap: 0.5rem; justify-content: flex-end;">
                        ${assignBtn}
                        <button class="btn btn-secondary" style="padding: 0.3rem 0.6rem; font-size: 0.8rem; background-color: white; border: 1px solid #dc2626; color: #dc2626;" 
                                onclick="window.location.href='${refundUrl}'">
                                Xử lý hoàn tiền
                        </button>
                    </td>
                </tr>
            `;
        });

        html += `
                        </tbody>
                    </table>
                </div>
            </div>
        `;
        
        container.innerHTML = html;

    } catch (err) {
        console.error('Failed to fetch critical guide alerts', err);
    }
}
