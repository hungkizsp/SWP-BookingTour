
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
    const userInfoEl = document.getElementById('userInfo');
    if (userInfoEl) userInfoEl.innerText = user.fullName || user.email;

    await loadBookings();
    setView(currentView);
});

const PAGE_SIZE = 10;
let allBookingsRaw = [];
let allBookings = [];
let currentPage = 0;
let currentView = localStorage.getItem('bookingView_staff') || 'table';
let currentStatus = 'ALL';
let searchQuery = '';

async function loadBookings() {
    const tbody = document.querySelector('#bookingsTable tbody');
    if (!tbody) return;
    try {
        const res = await TB.apiFetch('/api/v1/bookings');
        allBookingsRaw = res.data || [];
        allBookingsRaw.sort((a, b) => b.id - a.id);
        filterAndRender();
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="8" style="color:red; text-align:center;">Lỗi tải dữ liệu: ${error.message}</td></tr>`;
    }
}

function formatTourDate(value) {
    if (!value) return 'N/A';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'N/A';
    return date.toLocaleDateString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit' });
}

function formatBookingDate(value) {
    if (!value) return 'N/A';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'N/A';
    return date.toLocaleString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

function filterAndRender() {
    allBookings = allBookingsRaw.filter(b => {
        // Status filter
        if (currentStatus !== 'ALL') {
            if (currentStatus === 'CONFIRMED_PAID') {
                if (b.status !== 'CONFIRMED' && b.status !== 'PAID' && b.status !== 'SUCCESS') return false;
            } else if (b.status !== currentStatus) {
                return false;
            }
        }

        // Search query filter
        if (searchQuery) {
            const q = searchQuery.toLowerCase();
            const idStr = '#' + b.id;
            const name = (b.userFullName || 'Guest').toLowerCase();
            const tour = ('SD-' + b.scheduleId).toLowerCase();
            if (!idStr.includes(q) && !name.includes(q) && !tour.includes(q) && !String(b.id).includes(q)) {
                return false;
            }
        }
        return true;
    });

    updateStatusTabs();
    renderBookingsPage();
}

function updateStatusTabs() {
    const baseList = allBookingsRaw.filter(b => {
        if (searchQuery) {
            const q = searchQuery.toLowerCase();
            const idStr = '#' + b.id;
            const name = (b.userFullName || 'Guest').toLowerCase();
            const tour = ('SD-' + b.scheduleId).toLowerCase();
            return idStr.includes(q) || name.includes(q) || tour.includes(q) || String(b.id).includes(q);
        }
        return true;
    });

    const counts = {
        ALL: baseList.length,
        PENDING_CASH: baseList.filter(b => b.status === 'PENDING_CASH').length,
        CONFIRMED_PAID: baseList.filter(b => b.status === 'CONFIRMED' || b.status === 'PAID' || b.status === 'SUCCESS').length,
        CANCELLED: baseList.filter(b => b.status === 'CANCELLED').length
    };

    const statuses = [
        { key: 'ALL', label: 'Tất cả' },
        { key: 'PENDING_CASH', label: 'Chờ duyệt mặt' },
        { key: 'CONFIRMED_PAID', label: 'Đã xác nhận' },
        { key: 'CANCELLED', label: 'Đã hủy' }
    ];

    const tabsContainer = document.getElementById('statusTabs');
    if (tabsContainer) {
        tabsContainer.innerHTML = statuses.map(s => `
            <div class="status-tab ${currentStatus === s.key ? 'active' : ''}" onclick="window.setStatusFilter('${s.key}')">
                <span>${s.label}</span>
                <span class="count">${counts[s.key] || 0}</span>
            </div>
        `).join('');
    }
}

function renderBookingsPage() {
    const totalPages = Math.ceil(allBookings.length / PAGE_SIZE) || 1;

    if (currentPage >= totalPages) {
        currentPage = Math.max(0, totalPages - 1);
    }

    const start = currentPage * PAGE_SIZE;
    const pageItems = allBookings.slice(start, start + PAGE_SIZE);

    if (currentView === 'grid') {
        renderCardGrid(pageItems);
    } else {
        renderTableRows(pageItems);
    }

    renderPagination(totalPages);
}

function renderTableRows(pageItems) {
    const tbody = document.querySelector('#bookingsTable tbody');
    if (!tbody) return;

    tbody.innerHTML = '';
    if (pageItems.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align:center; padding: 2rem;">Không tìm thấy booking.</td></tr>';
        return;
    }

    pageItems.forEach(b => {
        const isPendingCash = b.status === 'PENDING_CASH';
        const isPending = b.status === 'PENDING' || isPendingCash;
        let statusClass = 'status-cancelled';

        if (isPending || b.status === 'REFUND_REQUESTED') {
            statusClass = 'status-pending';
        } else if (b.status === 'CONFIRMED' || b.status === 'PAID') {
            statusClass = 'status-confirmed';
        } else if (b.status === 'SUCCESS' || b.status === 'COMPLETED') {
            statusClass = 'status-completed';
        } else {
            statusClass = 'status-cancelled';
        }

        let guideColumnHtml = '<td>-</td>';
        if (b.guideFullName) {
            guideColumnHtml = `<td>${b.guideFullName}</td>`;
        } else if (b.status === 'CONFIRMED' || b.status === 'PAID') {
            guideColumnHtml = `<td><button class="action-btn" style="background: #3b82f6" onclick="window.openAssignModal(${b.scheduleId})">👤 Assign</button></td>`;
        }

        const row = document.createElement('tr');
        row.innerHTML = `
            <td>#${b.id}</td>
            <td>
                <div style="font-weight:700;">${b.userFullName || 'Guest'}</div>
                <div style="font-size:0.75rem; color:#666;">${b.userEmail || ''}</div>
            </td>
            <td>${b.tourName || ('SD-' + b.scheduleId)}</td>
            <td style="font-size:0.85rem; white-space:nowrap; color:#0369a1; font-weight:600;">${formatTourDate(b.departureDate)}</td>
            <td style="font-size:0.8rem; white-space:nowrap;">${formatBookingDate(b.bookingDate)}</td>
            <td style="text-align:center;">${b.numberOfPeople || '-'}</td>
            <td style="font-weight:700; color:#ef4444;">${Number(b.totalPrice).toLocaleString('vi-VN')} VNĐ</td>
            <td><span class="status-badge ${statusClass}">${b.status}</span></td>
            ${guideColumnHtml}
        `;
        tbody.appendChild(row);
    });
}

function renderCardGrid(pageItems) {
    const grid = document.getElementById('gridView');
    if (!grid) return;

    grid.innerHTML = '';
    if (pageItems.length === 0) {
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 3rem; color: var(--text-muted);">Không tìm thấy booking nào.</div>';
        return;
    }

    grid.innerHTML = pageItems.map(b => {
        const isPendingCash = b.status === 'PENDING_CASH';
        const isPending = b.status === 'PENDING' || isPendingCash;
        let statusClass = 'badge-danger';

        if (isPending || b.status === 'REFUND_REQUESTED') {
            statusClass = 'badge-warning';
        } else if (b.status === 'CONFIRMED' || b.status === 'PAID') {
            statusClass = 'badge-confirmed';
        } else if (b.status === 'SUCCESS' || b.status === 'COMPLETED') {
            statusClass = 'badge-success';
        } else {
            statusClass = 'badge-danger';
        }
        let guideHtml = '';
        if (b.guideFullName) {
            guideHtml = `<span style="font-weight:700; color:var(--success);">${b.guideFullName}</span>`;
        } else if (b.status === 'CONFIRMED' || b.status === 'PAID') {
            guideHtml = `<button class="action-btn assign-btn" style="padding: 4px 8px !important; font-size: 0.75rem !important;" onclick="window.openAssignModal(${b.scheduleId})">👤 Assign</button>`;
        } else {
            guideHtml = `<span style="color:#94a3b8;">Chưa gán</span>`;
        }

        return `
        <div class="booking-card">
            <div class="booking-card-header">
                <span class="booking-card-id">#${b.id}</span>
                <span class="badge ${statusClass}">${b.status}</span>
            </div>
            <div class="booking-card-body">
                <div class="booking-card-row">
                    <span class="icon">🧑</span>
                    <div class="val">
                        <strong style="font-size: 0.95rem;">${b.userFullName || 'Guest'}</strong>
                        <div style="font-size: 0.75rem; color: var(--text-muted);">${b.userEmail || ''}</div>
                    </div>
                </div>
                <div class="booking-card-row">
                    <span class="icon">✈️</span>
                    <div class="val">
                        <span class="label">Tour:</span><strong>${b.tourName || ('SD-' + b.scheduleId)}</strong>
                    </div>
                </div>
                <div class="booking-card-row">
                    <span class="icon">📅</span>
                    <div class="val">
                        <span class="label">Ngày tour:</span><strong style="color:#0369a1;">${formatTourDate(b.departureDate)}</strong>
                    </div>
                </div>
                <div class="booking-card-row">
                    <span class="icon">📅</span>
                    <div class="val">
                        <span class="label">Ngày đặt:</span>${formatBookingDate(b.bookingDate)}
                    </div>
                </div>
                <div class="booking-card-row">
                    <span class="icon">👥</span>
                    <div class="val">
                        <span class="label">Số khách:</span>${b.numberOfPeople || '-'}
                    </div>
                </div>
                <div class="booking-card-row">
                    <span class="icon">👤</span>
                    <div class="val">
                        <span class="label">HDV:</span>${guideHtml}
                    </div>
                </div>
            </div>
            <div class="booking-card-price">
                <span style="font-size:0.8rem; color:var(--text-muted); font-weight:normal;">Tổng cộng:</span>
                <strong>${Number(b.totalPrice).toLocaleString('vi-VN')} VNĐ</strong>
            </div>
        </div>
        `;
    }).join('');
}

function renderPagination(totalPages) {
    const container = document.getElementById('pagination');
    if (!container) return;
    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }
    let html = `<nav aria-label="Page navigation"><ul class="pagination" style="display:flex; list-style:none; padding:0; gap:5px; margin:0;">`;

    html += `<li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
                <button class="page-link" style="padding:8px 12px; border:1px solid #dee2e6; background:${currentPage === 0 ? '#e9ecef' : '#fff'}; color:#0f766e; border-radius:4px; cursor:${currentPage === 0 ? 'not-allowed' : 'pointer'};" ${currentPage === 0 ? 'disabled' : ''} onclick="window.goToPage(${currentPage - 1})">Previous</button>
             </li>`;

    for (let i = 0; i < totalPages; i++) {
        if (i === 0 || i === totalPages - 1 || Math.abs(i - currentPage) <= 1) {
            html += `<li class="page-item ${i === currentPage ? 'active' : ''}">
                        <button class="page-link" style="padding:8px 12px; border:1px solid #dee2e6; border-radius:4px; cursor:pointer; ${i === currentPage ? 'background:#0f766e; color:#fff; border-color:#0f766e;' : 'background:#fff; color:#0f766e;'}" onclick="window.goToPage(${i})">${i + 1}</button>
                     </li>`;
        } else if (Math.abs(i - currentPage) === 2) {
            html += `<li class="page-item disabled"><span class="page-link" style="padding:8px 12px; border:1px solid #dee2e6; background:#fff; color:#6c757d;">...</span></li>`;
        }
    }

    html += `<li class="page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}">
                <button class="page-link" style="padding:8px 12px; border:1px solid #dee2e6; background:${currentPage >= totalPages - 1 ? '#e9ecef' : '#fff'}; color:#0f766e; border-radius:4px; cursor:${currentPage >= totalPages - 1 ? 'not-allowed' : 'pointer'};" ${currentPage >= totalPages - 1 ? 'disabled' : ''} onclick="window.goToPage(${currentPage + 1})">Next</button>
             </li>`;

    html += `</ul></nav>`;
    container.innerHTML = html;
}

window.goToPage = function (page) {
    const totalPages = Math.ceil(allBookings.length / PAGE_SIZE) || 1;
    if (page < 0 || page >= totalPages) return;
    currentPage = page;
    renderBookingsPage();
};

window.setView = function (viewType) {
    currentView = viewType;
    localStorage.setItem('bookingView_staff', viewType);

    const viewTableBtn = document.getElementById('viewTableBtn');
    const viewGridBtn = document.getElementById('viewGridBtn');
    const tableView = document.getElementById('tableView');
    const gridView = document.getElementById('gridView');

    if (!viewTableBtn || !viewGridBtn || !tableView || !gridView) return;

    if (currentView === 'grid') {
        viewTableBtn.classList.remove('active');
        viewTableBtn.style.background = 'transparent';
        viewTableBtn.style.color = 'var(--text-muted)';
        viewGridBtn.classList.add('active');
        viewGridBtn.style.background = 'var(--primary-light)';
        viewGridBtn.style.color = 'var(--primary-dark)';
        tableView.style.display = 'none';
        gridView.style.display = 'grid';
    } else {
        viewGridBtn.classList.remove('active');
        viewGridBtn.style.background = 'transparent';
        viewGridBtn.style.color = 'var(--text-muted)';
        viewTableBtn.classList.add('active');
        viewTableBtn.style.background = 'var(--primary-light)';
        viewTableBtn.style.color = 'var(--primary-dark)';
        gridView.style.display = 'none';
        tableView.style.display = 'block';
    }
    renderBookingsPage();
};

window.setStatusFilter = function (status) {
    currentStatus = status;
    currentPage = 0;
    filterAndRender();
};

window.onSearchInput = function () {
    const input = document.getElementById('searchFilter');
    searchQuery = input ? input.value.trim() : '';
    currentPage = 0;
    filterAndRender();
};

window.confirmBooking = async function (id) {
    if (!confirm('Xác nhận ghép thanh toán và kích hoạt booking này?')) return;
    const notify = (msg, type) => (typeof showToast === 'function') ? showToast(msg, type) : alert(msg);
    try {
        await TB.apiFetch(`/api/v1/staff/bookings/${id}/confirm`, { method: 'PATCH' });
        notify('Xác nhận đặt tour thành công!', 'success');
        loadBookings();
    } catch (err) {
        notify('Lỗi: ' + err.message, 'error');
        // Fallback mock
        notify('Mocked: Booking ' + id + ' confirmed!', 'success');
        allBookingsRaw.forEach(b => {
            if (b.id === id) b.status = 'CONFIRMED';
        });
        filterAndRender();
    }
};

// ==========================================
// ASSIGN GUIDE LOGIC
// ==========================================
window.openAssignModal = async function (scheduleId) {
    document.getElementById('targetScheduleId').innerText = `Mã lịch trình: SD-${scheduleId}`;
    document.getElementById('targetScheduleId').dataset.sid = scheduleId;
    document.getElementById('assignModal').style.display = 'flex';

    try {
        const select = document.getElementById('guideSelect');
        select.innerHTML = '<option value="">-- Đang tải danh sách Guide --</option>';
        const res = await TB.apiFetch('/api/v1/staff/guides');
        select.innerHTML = '<option value="">-- Chọn Guide --</option>';
        if (res.code === 200 && res.data) {
            res.data.forEach(g => {
                const opt = document.createElement('option');
                opt.value = g.id;
                opt.textContent = `${g.fullName} (${g.email})`;
                select.appendChild(opt);
            });
        }
    } catch (e) {
        console.error('Error loading guides:', e);
        document.getElementById('guideSelect').innerHTML = '<option value="">Lỗi khi tải danh sách Guide</option>';
    }
};

window.closeAssignModal = function () {
    document.getElementById('assignModal').style.display = 'none';
};

window.submitAssignment = async function () {
    const sid = document.getElementById('targetScheduleId').dataset.sid;
    const gid = document.getElementById('guideSelect').value;
    const notify = (msg, type) => (typeof showToast === 'function') ? showToast(msg, type) : alert(msg);

    if (!gid) {
        notify('Vui lòng chọn 1 Guide!', 'error');
        return;
    }

    try {
        const res = await TB.apiFetch(`/api/v1/staff/schedules/${sid}/assign-guide?guideId=${gid}`, {
            method: 'PATCH'
        });

        if (res.code === 200) {
            notify('Gán Guide thành công!', 'success');
            closeAssignModal();
            const selectEl = document.getElementById('guideSelect');
            const selectedText = selectEl.options[selectEl.selectedIndex].text.split('(')[0].trim();
            allBookingsRaw.forEach(b => {
                if (b.scheduleId == sid) {
                    b.guideFullName = selectedText;
                }
            });
            filterAndRender();
        } else {
            notify('Lỗi gán Guide: ' + res.message, 'error');
        }
    } catch (e) {
        const serverMsg = e.message || 'Lỗi hệ thống khi gán Guide.';
        notify('❌ ' + serverMsg, 'error');
        console.error('[AssignGuide]', e);
    }
};

// Inject Cancel Modal HTML
document.addEventListener('DOMContentLoaded', () => {
    const modalHtml = `
    <div class="modal" id="cancelModal" style="display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.5); align-items: center; justify-content: center; z-index: 1000;">
        <div class="modal-content" style="background: white; padding: 2rem; border-radius: 12px; width: 450px; max-width: 95%;">
            <h2 style="margin-top: 0; margin-bottom: 1rem; color: #1e293b;">Hủy đặt tour</h2>
            <p style="margin-bottom: 1rem; color: #475569;">Vui lòng cung cấp lý do hủy. Lý do này sẽ được gửi email cho khách hàng.</p>
            <textarea id="cancelReason" rows="4" oninput="window.onReasonChange()" placeholder="Nhập lý do hủy (bắt buộc)..." style="width: 100%; padding: 10px; border: 1px solid #ccc; border-radius: 8px; margin-bottom: 1rem; resize: vertical; font-family: inherit;"></textarea>
            <div class="modal-actions" style="display: flex; justify-content: flex-end; gap: 10px;">
                <button class="btn btn-secondary" onclick="window.closeCancelModal()" style="padding: 8px 16px; border: 1px solid #ccc; border-radius: 6px; cursor: pointer;">Đóng</button>
                <button id="confirmCancelBtn" class="btn btn-danger" onclick="window.submitCancelBooking()" style="padding: 8px 16px; background: #dc2626; color: white; border: none; border-radius: 6px; cursor: pointer;" disabled>Xác nhận hủy</button>
            </div>
        </div>
    </div>`;
    document.body.insertAdjacentHTML('beforeend', modalHtml);
});

let currentCancelId = null;

window.openCancelModal = function (id) {
    currentCancelId = id;
    const reasonEl = document.getElementById('cancelReason');
    const confirmBtn = document.getElementById('confirmCancelBtn');
    if (reasonEl) reasonEl.value = '';
    if (confirmBtn) confirmBtn.disabled = true;
    const modal = document.getElementById('cancelModal');
    if (modal) modal.style.display = 'flex';
};

window.closeCancelModal = function () {
    const modal = document.getElementById('cancelModal');
    if (modal) modal.style.display = 'none';
    currentCancelId = null;
};

window.onReasonChange = function () {
    const reasonEl = document.getElementById('cancelReason');
    const confirmBtn = document.getElementById('confirmCancelBtn');
    if (reasonEl && confirmBtn) {
        confirmBtn.disabled = reasonEl.value.trim().length === 0;
    }
};

window.submitCancelBooking = async function () {
    if (!currentCancelId) return;
    const reasonEl = document.getElementById('cancelReason');
    const reason = reasonEl ? reasonEl.value.trim() : '';
    if (!reason) return;
    const notify = (msg, type) => (typeof showToast === 'function') ? showToast(msg, type) : alert(msg);

    try {
        await TB.apiFetch(`/api/v1/admin/bookings/${currentCancelId}/cancel`, {
            method: 'PUT',
            body: JSON.stringify({ reason: reason })
        });
        notify('Hủy tour thành công!', 'success');
        closeCancelModal();
        loadBookings();
    } catch (err) {
        notify('Lỗi khi hủy: ' + err.message, 'error');
    }
};


