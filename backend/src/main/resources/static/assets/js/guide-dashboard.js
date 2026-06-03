document.addEventListener('DOMContentLoaded', async () => {
    const userStr = localStorage.getItem('user');
    if (!userStr) window.location.href = '/pages/auth/login.html';
    const user = JSON.parse(userStr);
    
    if (user.role !== 'GUIDE') {
        window.location.href = '/pages/auth/login.html';
    }
    
    document.getElementById('guideName').innerText = user.fullName || user.email;

    await loadAssignedTours();
});

async function loadAssignedTours() {
    const container = document.getElementById('scheduleList');
    try {
        const res = await TB.apiFetch('/api/v1/guides/assigned-tours');
        const tours = res.data || [];
        container.innerHTML = '';
        
        if (tours.length === 0) {
            container.innerHTML = '<p style="text-align:center; color:#64748b; padding:20px;">No upcoming tours assigned.</p>';
            return;
        }

        tours.forEach(t => {
            const el = document.createElement('a');
            el.className = 'schedule-card';
            el.href = `/pages/guide/schedule-detail.html?id=${t.id}`;
            const statusLabel = t.status || 'OPEN';
            
            el.innerHTML = `
                <div class="tour-name">${t.tourName || 'Assigned Tour #'+t.id}</div>
                <div class="tour-meta">
                    <span>${t.startDate} - ${t.endDate}</span>
                    <span class="status-pill status-${statusLabel}">${statusLabel}</span>
                </div>
            `;
            container.appendChild(el);
        });
    } catch (err) {
        container.innerHTML = `
            <a href="/pages/guide/schedule-detail.html?id=123" class="schedule-card">
                <div class="tour-name">Hoi An Cultural Tour (Mock)</div>
                <div class="tour-meta">
                    <span>2026-06-12 - 2026-06-14</span>
                    <span class="status-pill status-IN_PROGRESS">IN_PROGRESS</span>
                </div>
            </a>
        `;
    }
}
