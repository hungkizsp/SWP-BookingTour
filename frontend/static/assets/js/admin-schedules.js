document.addEventListener('DOMContentLoaded', async () => {
    const userStr = sessionStorage.getItem('user');
    if (!userStr) { window.location.href = '/pages/auth/login.html'; return; }
    const user = JSON.parse(userStr);

    if (user.role === 'ADMIN') {
        window.location.href = '/pages/admin/dashboard.html';
        return;
    }
    if (user.role !== 'STAFF') {
        window.location.href = '/pages/auth/login.html';
        return;
    }
    document.getElementById('userInfo').innerText = user.fullName || user.email;

    await loadSchedules();
});

const PAGE_SIZE = 10;
let currentScheduleId = null;
let allSchedules = [];
let currentPage = 0;

async function loadSchedules() {
    const tbody = document.querySelector('#schedulesTable tbody');
    try {
        const res = await TB.apiFetch('/api/v1/staff/schedules'); 
        let data = res.data?.content || res.data || [];
        
        // Latest-first sort
        data.sort((a, b) => b.id - a.id);
        allSchedules = data;
        renderSchedulesPage();
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="6" style="color:red">Error: ${e.message}</td></tr>`;
    }
}

function renderSchedulesPage() {
    const tbody = document.querySelector('#schedulesTable tbody');
    const container = document.getElementById('pagination');
    
    const totalPages = Math.ceil(allSchedules.length / PAGE_SIZE) || 1;
    if (currentPage >= totalPages) currentPage = Math.max(0, totalPages - 1);
    
    const start = currentPage * PAGE_SIZE;
    const pageItems = allSchedules.slice(start, start + PAGE_SIZE);
    
    tbody.innerHTML = '';
    if (pageItems.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6">No schedules found.</td></tr>';
        if(container) container.innerHTML = '';
        return;
    }

    pageItems.forEach(s => {
        const STATUS_STYLE = {
            OPEN:           'background:#d1fae5; color:#059669;',
            BOOKING_CLOSED: 'background:#fef3c7; color:#d97706;',
            SOLD_OUT:       'background:#fee2e2; color:#dc2626;',
            IN_PROGRESS:    'background:#dbeafe; color:#2563eb;',
            COMPLETED:      'background:#f1f5f9; color:#64748b;',
            CANCELLED:      'background:#f3f4f6; color:#9ca3af;',
            EXPIRED_NO_BOOKING: 'background:#e5e7eb; color:#6b7280;',
        };
        const statusKey = String(s.status || '').toUpperCase();
        const statusStyle = STATUS_STYLE[statusKey] || 'background:#f3f4f6; color:#6b7280;';
        const STATUS_LABELS = {
            OPEN: 'Mở đặt', BOOKING_CLOSED: 'Đóng đặt', SOLD_OUT: 'Hết chỗ',
            IN_PROGRESS: 'Đang diễn ra', COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy',
            EXPIRED_NO_BOOKING: 'Đã hết hạn - 0 khách',
        };
        const statusLabel = STATUS_LABELS[statusKey] || s.status;
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>SD-${s.id}</td>
            <td>${s.tourName || 'Basic Tour'}</td>
            <td>${s.startDate} - ${s.endDate}</td>
            <td>${s.guideId ? 'Guide #' + s.guideId : '<span style="color:#d97706">Unassigned</span>'}</td>
            <td><span style="display:inline-block; padding: 3px 10px; border-radius: 20px; font-size: 0.75rem; font-weight: 700; ${statusStyle}">${statusLabel}</span></td>
        `;
        tbody.appendChild(row);
    });
    
    renderPagination(totalPages);
}

function renderPagination(totalPages) {
    const container = document.getElementById('pagination');
    if (!container) return;
    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }
    let html = `<button class="action-btn" style="background:#cbd5e1; color:#1e293b;" ${currentPage === 0 ? 'disabled' : ''} onclick="goToPage(${currentPage - 1})">Prev</button>`;
    html += `<span style="display:flex; align-items:center; font-weight:bold; margin: 0 10px;">Page ${currentPage + 1} of ${totalPages}</span>`;
    html += `<button class="action-btn" style="background:#cbd5e1; color:#1e293b;" ${currentPage >= totalPages - 1 ? 'disabled' : ''} onclick="goToPage(${currentPage + 1})">Next</button>`;
    container.innerHTML = html;
}

window.goToPage = function(page) {
    const totalPages = Math.ceil(allSchedules.length / PAGE_SIZE);
    if (page < 0 || page >= totalPages) return;
    currentPage = page;
    renderSchedulesPage();
};

window.openDetailsModal = async function(scheduleId) {
    document.getElementById('detailsTitle').innerText = `Details for SD-${scheduleId}`;
    
    const progressCont = document.getElementById('detailsProgress');
    const photoCont = document.getElementById('detailsPhotos');
    const reportCont = document.getElementById('detailsReport');

    progressCont.innerHTML = '<p style="color: #64748b; font-size: 0.8rem;">Loading history...</p>';
    photoCont.innerHTML = '';
    reportCont.innerText = 'Loading report...';

    document.getElementById('detailsModal').classList.add('active');

    try {
        const ts = new Date().getTime();
        const res = await TB.apiFetch(`/api/v1/staff/schedules/${scheduleId}?t=${ts}`, { cache: 'no-store' });
        if (res.code !== 200) throw new Error(res.message || 'Failed to load details');
        
        const s = res.data;
        if (!s) return;

        // Render Progress History Timeline
        progressCont.innerHTML = '';
        if (s.progressLogs && s.progressLogs.length) {
            s.progressLogs.forEach(log => {
                const timeStr = new Date(log.createdAt).toLocaleString();
                progressCont.innerHTML += `
                    <div style="margin-bottom: 12px; border-bottom: 1px dashed #e2e8f0; padding-bottom: 8px;">
                        <div style="font-size: 0.75rem; color: #64748b; font-weight: 600;">${timeStr}</div>
                        <div style="font-size: 0.9rem; color: #334155;">${log.content}</div>
                    </div>
                `;
            });
        } else {
            progressCont.innerHTML = '<p style="color: #94a3b8; font-size: 0.8rem;">No progress logs yet.</p>';
        }

        reportCont.innerText = s.reportContent || 'Report not yet submitted.';
        
        photoCont.innerHTML = '';
        if (s.imageUrls && s.imageUrls.length) {
            s.imageUrls.forEach(url => {
                const imgUrl = url.startsWith('/uploads') ? 'http://localhost:8080' + url : url;
                const img = document.createElement('img');
                img.src = imgUrl;
                img.style.width = '180px';
                img.style.height = '120px';
                img.style.flexShrink = '0';
                img.style.objectFit = 'cover';
                img.style.borderRadius = '10px';
                img.style.cursor = 'pointer';
                img.style.border = '1px solid #e2e8f0';
                img.onclick = () => window.open(imgUrl, '_blank');
                photoCont.appendChild(img);
            });
        } else {
            photoCont.innerHTML = '<p style="color: #94a3b8; font-size: 0.8rem;">No photos available.</p>';
        }
    } catch (e) {
        console.error('Fetch error details:', e);
        progressCont.innerHTML = `<p style="color: red; font-size: 0.8rem;">Error: ${e.message}</p>`;
        reportCont.innerText = 'Error loading report.';
    }
};

window.closeDetailsModal = function() {
    document.getElementById('detailsModal').classList.remove('active');
};

window.openAssignModal = async function(scheduleId) {
    currentScheduleId = scheduleId;
    document.getElementById('targetScheduleId').innerText = 'Selected Schedule: SD-' + scheduleId;
    document.getElementById('assignModal').classList.add('active');
    
    const select = document.getElementById('guideSelect');
    select.innerHTML = '<option>Loading guides...</option>';
    try {
        const res = await TB.apiFetch('/api/v1/staff/guides');
        select.innerHTML = '';
        if (res.data && res.data.length) {
            res.data.forEach(g => {
                select.innerHTML += `<option value="${g.id}">${g.fullName} (${g.email})</option>`;
            });
        } else {
            select.innerHTML = '<option>No guides found</option>';
        }
    } catch (e) {
         select.innerHTML = '<option style="color:red">Error loading guides</option>';
    }
};

window.closeModal = function() {
    document.getElementById('assignModal').classList.remove('active');
};

window.submitAssignment = async function() {
    const guideId = document.getElementById('guideSelect').value;
    const btn = document.querySelector('#assignModal .btn:not(.btn-secondary)');
    if (btn) {
        btn.disabled = true;
        btn.innerText = 'Assigning...';
    }
    try {
        await TB.apiFetch(`/api/v1/staff/schedules/${currentScheduleId}/assign-guide?guideId=${guideId}`, { method: 'PATCH' });
        alert('Guide Assigned Successfully!');
        closeModal();
        loadSchedules();
    } catch (err) {
        alert('Mocked: Error -> ' + err.message + '. But Guide ID ' + guideId + ' assigned in UI.');
        closeModal();
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerText = 'Assign';
        }
    }
};

window.searchSchedule = async function() {
    const inputId = document.getElementById('scheduleSearchId').value.trim();
    if (!inputId) {
        return loadSchedules();
    }
    
    const tbody = document.querySelector('#schedulesTable tbody');
    try {
        const res = await TB.apiFetch(`/api/v1/staff/schedules/${inputId}`);
        const schedule = res.data;
        if (!schedule) throw new Error('Not found');
        allSchedules = [schedule];
        currentPage = 0;
        renderSchedulesPage();
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="6" style="color:red; text-align:center;">Không tìm thấy lịch trình với ID này.</td></tr>`;
        const container = document.getElementById('pagination');
        if(container) container.innerHTML = '';
    }
};

window.resetScheduleSearch = function() {
    document.getElementById('scheduleSearchId').value = '';
    currentPage = 0;
    loadSchedules();
};
