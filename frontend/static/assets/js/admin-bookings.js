
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

    await loadBookings();
});

const PAGE_SIZE = 10;
let allBookings = [];
let currentPage = 0;
let totalPagesCount = 1;

async function loadBookings() {
    const tbody = document.querySelector('#bookingsTable tbody');
    try {
        const res = await TB.apiFetch(`/api/v1/bookings?page=${currentPage}&size=${PAGE_SIZE}`);


        if (res.data && Array.isArray(res.data.content)) {
            allBookings = res.data.content;

            // Spring Boot old format
            if (res.data.totalPages !== undefined) {
                totalPagesCount = res.data.totalPages;
            }
            // Spring Boot new Page serialization format
            else if (res.data.page && res.data.page.totalPages !== undefined) {
                totalPagesCount = res.data.page.totalPages;
            }
            else {
                totalPagesCount = 1;
            }
        } else {
            allBookings = res.data || [];
            totalPagesCount = Math.ceil(allBookings.length / PAGE_SIZE) || 1;
            const start = currentPage * PAGE_SIZE;
            allBookings = allBookings.slice(start, start + PAGE_SIZE);
        }
        renderBookingsPage();
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="6" style="color:red">Error: ${error.message}</td></tr>`;
    }
}

function renderBookingsPage() {
    const tbody = document.querySelector('#bookingsTable tbody');
    const container = document.getElementById('pagination');

    tbody.innerHTML = '';
    if (allBookings.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6">No bookings found.</td></tr>';
        if (container) container.innerHTML = '';
        return;
    }

    allBookings.forEach(b => {
        const isPendingCash = b.status === 'PENDING_CASH';
        const isPending = b.status === 'PENDING' || isPendingCash;
        const statusClass = isPending ? 'status-pending' : (b.status === 'CONFIRMED' ? 'status-confirmed' : 'status-cancelled');
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>#${b.id}</td>
            <td>${b.userFullName || 'Guest'}</td>
            <td>SD-${b.scheduleId}</td>
            <td>$${b.totalPrice}</td>
            <td><span class="status-badge ${statusClass}">${b.status}</span></td>
            <td>
                ${isPendingCash ? `<button class="action-btn" onclick="confirmBooking(${b.id})">Confirm</button>` : ''}
                ${(b.status === 'CONFIRMED' || b.status === 'PAID') ? `<button class="action-btn" style="background:#dc2626;" onclick="openCancelModal(${b.id})">Hủy đặt tour</button>` : ''}
            </td>
        `;
        tbody.appendChild(row);
    });

    renderPagination(totalPagesCount);
}

function renderPagination(totalPages) {
    const container = document.getElementById('pagination');
    if (!container) return;
    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }
    let html = `<nav aria-label="Page navigation"><ul class="pagination" style="display:flex; list-style:none; padding:0; gap:5px;">`;

    html += `<li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
                <button class="page-link" style="padding:8px 12px; border:1px solid #dee2e6; background:${currentPage === 0 ? '#e9ecef' : '#fff'}; color:#0f766e; border-radius:4px; cursor:${currentPage === 0 ? 'not-allowed' : 'pointer'};" ${currentPage === 0 ? 'disabled' : ''} onclick="goToPage(${currentPage - 1})">Previous</button>
             </li>`;

    for (let i = 0; i < totalPages; i++) {
        if (i === 0 || i === totalPages - 1 || Math.abs(i - currentPage) <= 1) {
            html += `<li class="page-item ${i === currentPage ? 'active' : ''}">
                        <button class="page-link" style="padding:8px 12px; border:1px solid #dee2e6; border-radius:4px; cursor:pointer; ${i === currentPage ? 'background:#0f766e; color:#fff; border-color:#0f766e;' : 'background:#fff; color:#0f766e;'}" onclick="goToPage(${i})">${i + 1}</button>
                     </li>`;
        } else if (Math.abs(i - currentPage) === 2) {
            html += `<li class="page-item disabled"><span class="page-link" style="padding:8px 12px; border:1px solid #dee2e6; background:#fff; color:#6c757d;">...</span></li>`;
        }
    }

    html += `<li class="page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}">
                <button class="page-link" style="padding:8px 12px; border:1px solid #dee2e6; background:${currentPage >= totalPages - 1 ? '#e9ecef' : '#fff'}; color:#0f766e; border-radius:4px; cursor:${currentPage >= totalPages - 1 ? 'not-allowed' : 'pointer'};" ${currentPage >= totalPages - 1 ? 'disabled' : ''} onclick="goToPage(${currentPage + 1})">Next</button>
             </li>`;

    html += `</ul></nav>`;
    container.innerHTML = html;
}

window.goToPage = function (page) {
    if (page < 0 || page >= totalPagesCount) return;
    currentPage = page;
    loadBookings();
};

window.confirmBooking = async function (id) {
    if (!confirm('Confirm mapping payment and activating this booking?')) return;
    try {
        await TB.apiFetch(`/api/v1/staff/bookings/${id}/confirm`, { method: 'PATCH' });
        alert('Booking confirmed!');
        loadBookings();
    } catch (err) {
        alert('Error: ' + err.message);
        // Fallback for UI visualization
        alert('Mocked: Booking ' + id + ' confirmed!');
        loadBookings();
    }
};

// Inject Cancel Modal HTML
document.addEventListener('DOMContentLoaded', () => {
    const modalHtml = `
    <div class="modal" id="cancelModal" style="display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.5); align-items: center; justify-content: center; z-index: 1000;">
        <div class="modal-content" style="background: white; padding: 2rem; border-radius: 12px; width: 450px; max-width: 95%;">
            <h2 style="margin-top: 0; margin-bottom: 1rem; color: #1e293b;">Hủy đặt tour</h2>
            <p style="margin-bottom: 1rem; color: #475569;">Vui lòng cung cấp lý do hủy. Lý do này sẽ được gửi email cho khách hàng.</p>
            <textarea id="cancelReason" rows="4" oninput="onReasonChange()" placeholder="Nhập lý do hủy (bắt buộc)..." style="width: 100%; padding: 10px; border: 1px solid #ccc; border-radius: 8px; margin-bottom: 1rem; resize: vertical; font-family: inherit;"></textarea>
            <div class="modal-actions" style="display: flex; justify-content: flex-end; gap: 10px;">
                <button class="btn btn-secondary" onclick="closeCancelModal()" style="padding: 8px 16px; border: 1px solid #ccc; border-radius: 6px; cursor: pointer;">Đóng</button>
                <button id="confirmCancelBtn" class="btn btn-danger" onclick="submitCancelBooking()" style="padding: 8px 16px; background: #dc2626; color: white; border: none; border-radius: 6px; cursor: pointer;" disabled>Xác nhận hủy</button>
            </div>
        </div>
    </div>`;
    document.body.insertAdjacentHTML('beforeend', modalHtml);
});

let currentCancelId = null;

window.openCancelModal = function(id) {
    currentCancelId = id;
    document.getElementById('cancelReason').value = '';
    document.getElementById('confirmCancelBtn').disabled = true;
    document.getElementById('cancelModal').style.display = 'flex';
};

window.closeCancelModal = function() {
    document.getElementById('cancelModal').style.display = 'none';
    currentCancelId = null;
};

window.onReasonChange = function() {
    const reason = document.getElementById('cancelReason').value.trim();
    document.getElementById('confirmCancelBtn').disabled = reason.length === 0;
};

window.submitCancelBooking = async function() {
    if (!currentCancelId) return;
    const reason = document.getElementById('cancelReason').value.trim();
    if (!reason) return;

    try {
        await TB.apiFetch(`/api/v1/admin/bookings/${currentCancelId}/cancel`, { 
            method: 'PUT',
            body: JSON.stringify({ reason: reason })
        });
        alert('Hủy tour thành công!');
        closeCancelModal();
        loadBookings();
    } catch (err) {
        alert('Lỗi khi hủy: ' + err.message);
    }
};


