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
            el.style.marginBottom = '1rem';
            const statusLabel = t.status || 'OPEN';

            let badgeClass = 'badge-info';
            if (statusLabel === 'IN_PROGRESS') badgeClass = 'badge-warning';
            if (statusLabel === 'COMPLETED') badgeClass = 'badge-success';

            const canAttend = (statusLabel === 'OPEN' || statusLabel === 'CONFIRMED'
                || statusLabel === 'IN_PROGRESS' || statusLabel === 'BOOKING_CLOSED' || statusLabel === 'SOLD_OUT');

            el.innerHTML = `
                <div style="font-weight: 700; font-size: 1.1rem; color: var(--text-main); margin-bottom: 8px; cursor:pointer;"
                     onclick="window.location.href='/pages/guide/schedule-detail.html?id=${t.id}'">${t.tourName || 'Assigned Tour #'+t.id}</div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
                    <span><span style="margin-right: 4px;">📅</span> ${t.startDate} - ${t.endDate}</span>
                    <span class="badge ${badgeClass}">${statusLabel}</span>
                </div>
                <div style="display:flex; gap:0.5rem; flex-wrap:wrap;">
                    <button class="btn btn-secondary" style="font-size:0.8rem;"
                        onclick="window.location.href='/pages/guide/schedule-detail.html?id=${t.id}'">
                        📋 Chi tiết
                    </button>
                    ${canAttend ? `<button class="btn btn-primary" style="font-size:0.8rem; background: #0891b2;"
                        onclick="openAttendanceModal(${t.id}, '${t.tourName || 'Tour'}')">
                        📋 Điểm danh
                    </button>` : ''}
                    <button class="btn btn-primary" style="font-size:0.8rem;"
                        onclick="event.stopPropagation(); window.location.href='/pages/client/group-chat.html?scheduleId=${t.id}';">
                        💬 Nhóm chat
                    </button>
                </div>
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

// ────────────────────────────────────────────────────────────────────────────
// ATTENDANCE MODAL
// ────────────────────────────────────────────────────────────────────────────

let _attScheduleId = null;
let _attTourName = '';
let _countdownInterval = null;

function openAttendanceModal(scheduleId, tourName) {
    _attScheduleId = scheduleId;
    _attTourName = tourName;

    // Create modal overlay if not exists
    let overlay = document.getElementById('attModalOverlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'attModalOverlay';
        overlay.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,.55);z-index:9999;display:flex;align-items:center;justify-content:center;';
        overlay.innerHTML = `
            <div id="attModal" style="background:#fff;border-radius:16px;padding:0;width:min(720px,96vw);max-height:88vh;display:flex;flex-direction:column;box-shadow:0 24px 80px rgba(0,0,0,.4);">
                <div style="padding:1.25rem 1.5rem;border-bottom:1px solid #e2e8f0;display:flex;justify-content:space-between;align-items:center;">
                    <div>
                        <h3 style="margin:0;font-size:1.1rem;font-weight:700;color:#1e293b;">📋 Điểm danh — <span id="attTourNameLabel"></span></h3>
                        <div id="attCountdownBar" style="margin-top:4px;font-size:0.8rem;color:#f59e0b;font-weight:600;"></div>
                    </div>
                    <button onclick="closeAttendanceModal()" style="background:none;border:none;cursor:pointer;font-size:1.4rem;color:#64748b;">✕</button>
                </div>
                <div style="padding:1rem 1.5rem;overflow-y:auto;flex:1;">
                    <div id="attStats" style="display:flex;gap:1rem;margin-bottom:1rem;flex-wrap:wrap;"></div>
                    <table style="width:100%;border-collapse:collapse;font-size:0.875rem;" id="attTable">
                        <thead>
                            <tr style="background:#f1f5f9;">
                                <th style="padding:10px;text-align:left;border-bottom:2px solid #e2e8f0;">Khách hàng</th>
                                <th style="padding:10px;text-align:left;border-bottom:2px solid #e2e8f0;">Liên hệ</th>
                                <th style="padding:10px;text-align:center;border-bottom:2px solid #e2e8f0;">Trạng thái</th>
                                <th style="padding:10px;text-align:center;border-bottom:2px solid #e2e8f0;">Hành động</th>
                            </tr>
                        </thead>
                        <tbody id="attBody">
                            <tr><td colspan="4" style="text-align:center;padding:20px;color:#94a3b8;">Đang tải...</td></tr>
                        </tbody>
                    </table>
                </div>
                <div style="padding:1rem 1.5rem;border-top:1px solid #e2e8f0;display:flex;justify-content:flex-end;gap:.75rem;">
                    <button onclick="closeAttendanceModal()" style="padding:.5rem 1.25rem;border:1px solid #cbd5e1;background:#fff;border-radius:8px;cursor:pointer;font-size:.875rem;">Đóng</button>
                </div>
            </div>
            <!-- Late note sub-modal -->
            <div id="lateNoteModal" style="display:none;position:fixed;inset:0;background:rgba(0,0,0,.4);z-index:10000;align-items:center;justify-content:center;">
                <div style="background:#fff;border-radius:12px;padding:1.5rem;width:min(400px,92vw);box-shadow:0 8px 30px rgba(0,0,0,.3);">
                    <h4 style="margin:0 0 1rem;font-size:1rem;font-weight:700;">📝 Ghi chú đến trễ</h4>
                    <label style="display:block;font-size:.85rem;color:#475569;margin-bottom:.4rem;">Số phút trễ (ước tính)</label>
                    <input id="lateMinutesInput" type="number" min="1" placeholder="Nhập số phút..."
                        style="width:100%;padding:.5rem .75rem;border:1px solid #cbd5e1;border-radius:8px;font-size:.875rem;margin-bottom:.75rem;box-sizing:border-box;">
                    <label style="display:block;font-size:.85rem;color:#475569;margin-bottom:.4rem;">Ghi chú</label>
                    <textarea id="lateNoteInput" rows="3" placeholder="Ghi chú ngắn về trường hợp đến trễ..."
                        style="width:100%;padding:.5rem .75rem;border:1px solid #cbd5e1;border-radius:8px;font-size:.875rem;box-sizing:border-box;resize:vertical;"></textarea>
                    <div style="display:flex;gap:.5rem;justify-content:flex-end;margin-top:1rem;">
                        <button onclick="closeLateNoteModal()" style="padding:.4rem 1rem;border:1px solid #cbd5e1;background:#fff;border-radius:8px;cursor:pointer;font-size:.85rem;">Bỏ qua</button>
                        <button id="saveLateNoteBtn" style="padding:.4rem 1rem;background:#0891b2;color:#fff;border:none;border-radius:8px;cursor:pointer;font-size:.85rem;">Lưu</button>
                    </div>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);
    }

    overlay.style.display = 'flex';
    document.getElementById('attTourNameLabel').textContent = tourName;
    loadAttendances();
    startCountdown(scheduleId);
}

function closeAttendanceModal() {
    const overlay = document.getElementById('attModalOverlay');
    if (overlay) overlay.style.display = 'none';
    if (_countdownInterval) clearInterval(_countdownInterval);
}

function closeLateNoteModal() {
    const m = document.getElementById('lateNoteModal');
    if (m) m.style.display = 'none';
}

let _absentThreshold = null; // departure + 15 mins

async function startCountdown(scheduleId) {
    try {
        const res = await TB.apiFetch(`/api/v1/guides/tours/${scheduleId}`);
        const s = res.data || res;
        const dept = s.departureTime; // format HH:mm
        const startDate = s.startDate;

        if (!dept || !startDate) return;

        const [h, m] = dept.split(':').map(Number);
        const departure = new Date(startDate);
        departure.setHours(h, m, 0, 0);
        _absentThreshold = new Date(departure.getTime() + 15 * 60000);

        const bar = document.getElementById('attCountdownBar');
        if (!bar) return;

        if (_countdownInterval) clearInterval(_countdownInterval);
        _countdownInterval = setInterval(() => {
            const now = new Date();
            const diff = Math.ceil((_absentThreshold - now) / 1000);
            if (diff <= 0) {
                bar.textContent = '✅ Đã qua mốc 15 phút — Có thể đánh vắng';
                bar.style.color = '#16a34a';
                clearInterval(_countdownInterval);
                // Re-render to enable absent buttons
                loadAttendances();
            } else {
                const mins = Math.floor(diff / 60);
                const secs = diff % 60;
                bar.textContent = `⏳ Có thể đánh vắng sau: ${mins}p ${secs}s (15 phút kể từ giờ khởi hành)`;
                bar.style.color = '#f59e0b';
            }
        }, 1000);
    } catch (e) { /* ignore */ }
}

async function loadAttendances() {
    const body = document.getElementById('attBody');
    const stats = document.getElementById('attStats');
    if (!body || !_attScheduleId) return;

    body.innerHTML = '<tr><td colspan="4" style="text-align:center;padding:20px;color:#94a3b8;">Đang tải...</td></tr>';

    try {
        const res = await TB.apiFetch(`/api/v1/guides/assigned-tours/${_attScheduleId}/attendances`);
        const list = res.data || [];

        const present = list.filter(a => a.status === 'PRESENT').length;
        const absent = list.filter(a => a.status === 'ABSENT').length;
        const pending = list.filter(a => a.status === 'PENDING').length;
        const allDone = pending === 0 && list.length > 0;

        // Stats row
        stats.innerHTML = `
            <span style="background:#dcfce7;color:#15803d;border-radius:8px;padding:4px 12px;font-size:.8rem;font-weight:600;">✅ Có mặt: ${present}</span>
            <span style="background:#fee2e2;color:#dc2626;border-radius:8px;padding:4px 12px;font-size:.8rem;font-weight:600;">❌ Vắng: ${absent}</span>
            <span style="background:#fef9c3;color:#92400e;border-radius:8px;padding:4px 12px;font-size:.8rem;font-weight:600;">⏳ Chưa điểm: ${pending}</span>
            ${allDone ? '<span style="background:#0891b2;color:#fff;border-radius:8px;padding:4px 12px;font-size:.8rem;font-weight:600;">✔ Đủ điều kiện cập nhật tiến độ tour</span>' : ''}
        `;

        if (list.length === 0) {
            body.innerHTML = '<tr><td colspan="4" style="text-align:center;padding:20px;color:#94a3b8;">Không có booking nào trong lịch trình này.</td></tr>';
            return;
        }

        const now = new Date();
        const canMarkAbsent = _absentThreshold && now >= _absentThreshold;

        body.innerHTML = list.map(a => {
            const statusBadge = {
                PRESENT: '<span style="background:#dcfce7;color:#15803d;border-radius:6px;padding:2px 10px;font-size:.78rem;font-weight:600;">✅ Có mặt</span>',
                ABSENT: '<span style="background:#fee2e2;color:#dc2626;border-radius:6px;padding:2px 10px;font-size:.78rem;font-weight:600;">❌ Vắng</span>',
                PENDING: '<span style="background:#fef9c3;color:#92400e;border-radius:6px;padding:2px 10px;font-size:.78rem;font-weight:600;">⏳ Chưa điểm</span>',
            }[a.status] || a.status;

            const lateTag = (a.status === 'PRESENT' && a.lateMinutes) 
                ? `<span title="${a.lateNote || ''}" style="background:#fff7ed;color:#c2410c;border-radius:6px;padding:2px 8px;font-size:.75rem;font-weight:600;cursor:pointer;margin-left:4px;">⏰ Trễ ${a.lateMinutes}p</span>` 
                : '';

            const absentBtn = a.status !== 'ABSENT' ? `
                <button onclick="markAttendance(${a.id}, 'ABSENT')" 
                    ${canMarkAbsent ? '' : 'disabled title="Chỉ được đánh vắng sau 15 phút kể từ giờ khởi hành"'}
                    style="padding:4px 10px;border:1px solid ${canMarkAbsent ? '#dc2626' : '#cbd5e1'};
                           color:${canMarkAbsent ? '#dc2626' : '#94a3b8'};
                           background:#fff;border-radius:6px;cursor:${canMarkAbsent ? 'pointer' : 'not-allowed'};font-size:.78rem;">
                    ❌ Đánh vắng
                </button>` : '';

            const presentBtn = a.status !== 'PRESENT' ? `
                <button onclick="markAttendance(${a.id}, 'PRESENT')"
                    style="padding:4px 10px;border:none;background:#16a34a;color:#fff;border-radius:6px;cursor:pointer;font-size:.78rem;">
                    ✅ Có mặt
                </button>` : '';

            const lateNoteBtn = a.status === 'PRESENT' ? `
                <button onclick="openLateNoteModal(${a.id})"
                    style="padding:4px 10px;border:1px solid #0891b2;color:#0891b2;background:#fff;border-radius:6px;cursor:pointer;font-size:.78rem;">
                    📝 Ghi chú trễ
                </button>` : '';

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
                <td style="padding:10px;text-align:center;">
                    <div style="display:flex;gap:4px;justify-content:center;flex-wrap:wrap;">
                        ${presentBtn}${absentBtn}${lateNoteBtn}
                    </div>
                </td>
            </tr>`;
        }).join('');

    } catch (err) {
        body.innerHTML = `<tr><td colspan="4" style="text-align:center;padding:20px;color:#dc2626;">Lỗi tải danh sách: ${err.message}</td></tr>`;
    }
}

async function markAttendance(attendanceId, status) {
    try {
        await TB.apiFetch(`/api/v1/guides/assigned-tours/${_attScheduleId}/attendances/${attendanceId}`, {
            method: 'PUT',
            body: JSON.stringify({ status })
        });
        await loadAttendances();
    } catch (err) {
        alert('Lỗi: ' + (err.message || 'Không thể cập nhật điểm danh'));
    }
}

let _lateNoteTargetId = null;

function openLateNoteModal(attendanceId) {
    _lateNoteTargetId = attendanceId;
    const m = document.getElementById('lateNoteModal');
    if (!m) return;
    document.getElementById('lateMinutesInput').value = '';
    document.getElementById('lateNoteInput').value = '';
    m.style.display = 'flex';
    document.getElementById('saveLateNoteBtn').onclick = async () => {
        const lateMinutes = parseInt(document.getElementById('lateMinutesInput').value) || null;
        const lateNote = document.getElementById('lateNoteInput').value.trim() || null;
        try {
            await TB.apiFetch(`/api/v1/guides/assigned-tours/${_attScheduleId}/attendances/${_lateNoteTargetId}`, {
                method: 'PUT',
                body: JSON.stringify({ status: 'PRESENT', lateMinutes, lateNote })
            });
            closeLateNoteModal();
            await loadAttendances();
        } catch (err) {
            alert('Lỗi: ' + (err.message || 'Không thể lưu ghi chú'));
        }
    };
}
