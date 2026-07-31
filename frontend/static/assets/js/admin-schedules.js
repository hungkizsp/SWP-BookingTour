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
        tbody.innerHTML = `<tr><td colspan="6" style="color:red">Lỗi: ${e.message}</td></tr>`;
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
        tbody.innerHTML = '<tr><td colspan="6">Không tìm thấy lịch trình.</td></tr>';
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
            <td>
                <button class="action-btn" style="background:#0ea5e9; color:white; padding:4px 8px; font-size:0.75rem;" onclick="openAdminAttendanceModal(${s.id}, '${s.tourName}')">Điểm danh</button>
            </td>
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
    document.getElementById('detailsTitle').innerText = `Chi tiết lịch trình SD-${scheduleId}`;
    
    const progressCont = document.getElementById('detailsProgress');
    const photoCont = document.getElementById('detailsPhotos');
    const reportCont = document.getElementById('detailsReport');

    progressCont.innerHTML = '<p style="color: #64748b; font-size: 0.8rem;">Đang tải lịch sử...</p>';
    photoCont.innerHTML = '';
    reportCont.innerText = 'Đang tải báo cáo...';

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
            progressCont.innerHTML = '<p style="color: #94a3b8; font-size: 0.8rem;">Chưa có nhật ký tiến độ.</p>';
        }

        reportCont.innerText = s.reportContent || 'Báo cáo chưa được nộp.';
        
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
            photoCont.innerHTML = '<p style="color: #94a3b8; font-size: 0.8rem;">Chưa có ảnh.</p>';
        }
    } catch (e) {
        console.error('Fetch error details:', e);
        progressCont.innerHTML = `<p style="color: red; font-size: 0.8rem;">Lỗi: ${e.message}</p>`;
        reportCont.innerText = 'Lỗi khi tải báo cáo.';
    }
};

window.closeDetailsModal = function() {
    document.getElementById('detailsModal').classList.remove('active');
};

window.openAssignModal = async function(scheduleId) {
    currentScheduleId = scheduleId;
    document.getElementById('targetScheduleId').innerText = 'Lịch trình được chọn: SD-' + scheduleId;
    document.getElementById('assignModal').classList.add('active');
    
    const select = document.getElementById('guideSelect');
    select.innerHTML = '<option>Đang tải danh sách hướng dẫn viên...</option>';
    try {
        const res = await TB.apiFetch('/api/v1/staff/guides');
        select.innerHTML = '';
        if (res.data && res.data.length) {
            res.data.forEach(g => {
                select.innerHTML += `<option value="${g.id}">${g.fullName} (${g.email})</option>`;
            });
        } else {
            select.innerHTML = '<option>Không tìm thấy hướng dẫn viên</option>';
        }
    } catch (e) {
         select.innerHTML = '<option style="color:red">Lỗi khi tải hướng dẫn viên</option>';
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
        btn.innerText = 'Đang phân công...';
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
            btn.innerText = 'Phân công';
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

// --- Attendance Modal for Admin/Staff ---
window.openAdminAttendanceModal = async function(scheduleId, tourName) {
    let overlay = document.getElementById('adminAttModalOverlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'adminAttModalOverlay';
        overlay.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,.55);z-index:9999;display:flex;align-items:center;justify-content:center;';
        overlay.innerHTML = `
            <div style="background:#fff;border-radius:16px;padding:0;width:min(800px,96vw);max-height:88vh;display:flex;flex-direction:column;box-shadow:0 24px 80px rgba(0,0,0,.4);">
                <div style="padding:1.25rem 1.5rem;border-bottom:1px solid #e2e8f0;display:flex;justify-content:space-between;align-items:center;">
                    <h3 style="margin:0;font-size:1.1rem;font-weight:700;color:#1e293b;">📋 Tình hình điểm danh — <span id="adminAttTourNameLabel"></span></h3>
                    <button onclick="document.getElementById('adminAttModalOverlay').style.display='none'" style="background:none;border:none;cursor:pointer;font-size:1.4rem;color:#64748b;">✕</button>
                </div>
                <div style="padding:1rem 1.5rem;overflow-y:auto;flex:1;">
                    <div id="adminAttStats" style="display:flex;gap:1rem;margin-bottom:1rem;flex-wrap:wrap;"></div>
                    <table style="width:100%;border-collapse:collapse;font-size:0.875rem;">
                        <thead>
                            <tr style="background:#f1f5f9;">
                                <th style="padding:10px;text-align:left;border-bottom:2px solid #e2e8f0;">Khách hàng</th>
                                <th style="padding:10px;text-align:left;border-bottom:2px solid #e2e8f0;">Liên hệ</th>
                                <th style="padding:10px;text-align:center;border-bottom:2px solid #e2e8f0;">Trạng thái</th>
                                <th style="padding:10px;text-align:center;border-bottom:2px solid #e2e8f0;">Thời điểm</th>
                            </tr>
                        </thead>
                        <tbody id="adminAttBody">
                            <tr><td colspan="4" style="text-align:center;padding:20px;color:#94a3b8;">Đang tải...</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);
    }
    overlay.style.display = 'flex';
    document.getElementById('adminAttTourNameLabel').textContent = tourName || ('Lịch #' + scheduleId);
    const body = document.getElementById('adminAttBody');
    const stats = document.getElementById('adminAttStats');
    body.innerHTML = '<tr><td colspan="4" style="text-align:center;padding:20px;color:#94a3b8;">Đang tải...</td></tr>';
    stats.innerHTML = '';
    
    try {
        const res = await TB.apiFetch(`/api/v1/admin/attendance/${scheduleId}`);
        const data = res.data || res;
        
        stats.innerHTML = `
            <span style="background:#dcfce7;color:#15803d;border-radius:8px;padding:4px 12px;font-size:.8rem;font-weight:600;">✅ Có mặt: ${data.presentCount || 0}</span>
            <span style="background:#fee2e2;color:#dc2626;border-radius:8px;padding:4px 12px;font-size:.8rem;font-weight:600;">❌ Vắng: ${data.absentCount || 0}</span>
            <span style="background:#fef9c3;color:#92400e;border-radius:8px;padding:4px 12px;font-size:.8rem;font-weight:600;">⏳ Chưa điểm: ${data.pendingCount || 0}</span>
        `;
        
        if (!data.attendances || data.attendances.length === 0) {
            body.innerHTML = '<tr><td colspan="4" style="text-align:center;padding:20px;color:#94a3b8;">Không có dữ liệu điểm danh.</td></tr>';
            return;
        }
        
        body.innerHTML = data.attendances.map(a => {
            const statusBadge = {
                PRESENT: '<span style="background:#dcfce7;color:#15803d;border-radius:6px;padding:2px 10px;font-size:.78rem;font-weight:600;">✅ Có mặt</span>',
                ABSENT: '<span style="background:#fee2e2;color:#dc2626;border-radius:6px;padding:2px 10px;font-size:.78rem;font-weight:600;">❌ Vắng</span>',
                PENDING: '<span style="background:#fef9c3;color:#92400e;border-radius:6px;padding:2px 10px;font-size:.78rem;font-weight:600;">⏳ Chưa điểm</span>',
            }[a.status] || a.status;

            const lateTag = (a.status === 'PRESENT' && a.lateMinutes) 
                ? `<br><span title="${a.lateNote || ''}" style="background:#fff7ed;color:#c2410c;border-radius:6px;padding:2px 8px;font-size:.75rem;font-weight:600;display:inline-block;margin-top:4px;cursor:help;">⏰ Trễ ${a.lateMinutes}p</span>` 
                : '';

            return `<tr style="border-bottom:1px solid #f1f5f9;">
                <td style="padding:10px;">
                    <div style="font-weight:600;color:#1e293b;">${a.customerName}</div>
                    <div style="font-size:.75rem;color:#94a3b8;">Booking #${a.bookingId}</div>
                </td>
                <td style="padding:10px;">
                    <div style="font-size:.82rem;color:#475569;">📱 ${a.customerPhone}</div>
                    <div style="font-size:.82rem;color:#475569;">✉ ${a.customerEmail}</div>
                </td>
                <td style="padding:10px;text-align:center;">${statusBadge}${lateTag}</td>
                <td style="padding:10px;text-align:center;font-size:0.8rem;color:#64748b;">
                    ${a.markedAt ? new Date(a.markedAt).toLocaleString('vi-VN') : '—'}
                </td>
            </tr>`;
        }).join('');
    } catch (e) {
        body.innerHTML = `<tr><td colspan="4" style="text-align:center;padding:20px;color:red;">Lỗi: ${e.message}</td></tr>`;
    }
};
