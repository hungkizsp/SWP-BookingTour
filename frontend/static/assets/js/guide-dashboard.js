document.addEventListener('DOMContentLoaded', async () => {
    const userStr = sessionStorage.getItem('user');
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
            const el = document.createElement('div');
            el.className = 'card';
            el.style.cursor = 'pointer';
            el.style.marginBottom = '1rem';
            el.onclick = () => { window.location.href = `/pages/guide/schedule-detail.html?id=${t.id}`; };
            const statusLabel = t.status || 'OPEN';
            
            let badgeClass = 'badge-info';
            if (statusLabel === 'IN_PROGRESS') badgeClass = 'badge-warning';
            if (statusLabel === 'COMPLETED') badgeClass = 'badge-success';

            el.innerHTML = `
                <div style="font-weight: 700; font-size: 1.1rem; color: var(--text-main); margin-bottom: 8px;">${t.tourName || 'Assigned Tour #'+t.id}</div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
                    <span><span style="margin-right: 4px;">📅</span> ${t.startDate} - ${t.endDate}</span>
                    <span class="badge ${badgeClass}">${statusLabel}</span>
                </div>
                <button class="btn btn-primary" style="font-size:0.8rem;"
                    onclick="event.stopPropagation(); window.location.href='/pages/client/group-chat.html?scheduleId=${t.id}';">
                    💬 Nhóm chat
                </button>
            `;
            container.appendChild(el);
        });
    } catch (err) {
        container.innerHTML = `
            <a href="/pages/guide/schedule-detail.html?id=123" class="card" style="text-decoration:none; display:block;">
                <div style="font-weight: 700; font-size: 1.1rem; color: var(--text-main); margin-bottom: 8px;">Hoi An Cultural Tour (Mock)</div>
                <div style="font-size: 0.85rem; color: var(--text-muted); display: flex; justify-content: space-between; align-items: center;">
                    <span><span style="margin-right: 4px;">📅</span> 2026-06-12 - 2026-06-14</span>
                    <span class="badge badge-warning">IN_PROGRESS</span>
                </div>
            </a>
        `;
    }
}
