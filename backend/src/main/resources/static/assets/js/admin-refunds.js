let mockRefunds = [
    { id: 1, bookingId: 1002, amount: 300.00, status: 'REQUESTED', reason: 'Flight delayed' },
    { id: 2, bookingId: 1005, amount: 150.00, status: 'PROCESSED', reason: 'Customer sick' }
];

document.addEventListener('DOMContentLoaded', async () => {
    const userStr = localStorage.getItem('user');
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

    await loadRefunds();
});

async function loadRefunds() {
    const tbody = document.querySelector('#refundsTable tbody');
    let data = [];
    try {
        const res = await TB.apiFetch('/api/v1/staff/refunds');
        data = res.data || [];
    } catch (e) {
        console.warn('API Error, using Mock Data instead:', e);
        data = mockRefunds; // Fallback to stateful mock array
    }

    tbody.innerHTML = '';
    data.forEach(r => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>REQ-${r.id}</td>
            <td>#${r.bookingId}</td>
            <td>$${r.amount}</td>
            <td><span class="status-badge status-${(r.status||'').toLowerCase()}">${r.status}</span></td>
            <td>${r.reason || 'N/A'}</td>
            <td>
                ${r.status === 'PENDING' ? `
                    <button class="action-btn" onclick="processRefund(${r.id}, 'APPROVED')">Approve</button>
                    <button class="action-btn btn-danger" onclick="processRefund(${r.id}, 'REJECTED')">Reject</button>
                ` : ''}
            </td>
        `;
        tbody.appendChild(row);
    });
}

window.processRefund = async function(id, newStatus) {
    if (!confirm('Are you sure you want to mark this as ' + newStatus + '?')) return;
    try {
        await TB.apiFetch(`/api/v1/staff/refunds/${id}/process?status=${newStatus}`, { method: 'PATCH' });
        alert('Refund ' + newStatus + ' applied on Server!');
        loadRefunds();
    } catch (err) {
        // Mock State Update
        const target = mockRefunds.find(r => r.id === id);
        if (target) target.status = newStatus;
        alert('Mocked action ' + newStatus + ' applied locally!');
        loadRefunds();
    }
};
