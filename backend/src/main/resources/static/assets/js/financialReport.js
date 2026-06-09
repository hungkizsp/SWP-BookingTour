const FINANCIAL_REPORT_API = '/api/v1/admin/financial-report';
let currentReportData = [];
let revenueChart;

const EMPTY_REPORT_MESSAGE =
    'Chưa có dữ liệu doanh thu thực tế trong khoảng thời gian này';

function formatLocalDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

function handleSessionExpired() {
    const msg = 'Phiên đăng nhập hết hạn, vui lòng đăng nhập lại!';
    showToast(msg, 'error');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
    setTimeout(() => { window.location.href = '/pages/auth/login.html'; }, 1200);
}

function isAuthHttpError(error) {
    return error?.status === 401 || error?.status === 403;
}

function setDefaultDateRange() {
    const today = new Date();
    const startOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    document.getElementById('startDate').value = formatLocalDate(startOfMonth);
    document.getElementById('endDate').value = formatLocalDate(today);
    return {
        startDate: formatLocalDate(startOfMonth),
        endDate: formatLocalDate(today)
    };
}

function setReportLoading(isLoading) {
    const tbody = document.getElementById('reportBody');
    if (!tbody) return;
    if (isLoading) {
        tbody.innerHTML =
            '<tr><td colspan="5" style="text-align:center;padding:3rem;color:var(--text-muted);">Đang tải báo cáo...</td></tr>';
    }
}

async function loadReport(options = {}) {
    const { autoLoad = false } = options;
    const token = sessionStorage.getItem('token');
    if (!token) {
        handleSessionExpired();
        return;
    }

    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const reportType = document.getElementById('reportType').value;
    const status = document.getElementById('bookingStatus').value;
    const includeTest = status === 'INCLUDE_TEST';

    if (!startDate || !endDate) {
        showToast('Vui lòng chọn khoảng thời gian', 'warning');
        return;
    }

    if (startDate > endDate) {
        showToast('Ngày bắt đầu không được sau ngày kết thúc', 'warning');
        return;
    }

    setReportLoading(true);

    try {
        const response = await TB.apiFetch(
            `${FINANCIAL_REPORT_API}?start=${startDate}&end=${endDate}&type=${reportType}&status=${status}&includeTest=${includeTest}`
        );
        const data = response?.data ?? response ?? [];
        currentReportData = Array.isArray(data) ? data : [];

        renderReport(currentReportData);
        updateStats(currentReportData);
        updateChart(currentReportData);

        if (!autoLoad) {
            if (currentReportData.length > 0) {
                showToast('Tạo báo cáo thành công', 'success');
            } else {
                showToast(EMPTY_REPORT_MESSAGE, 'info');
            }
        }
    } catch (error) {
        console.error('Error loading report:', error);
        currentReportData = [];
        updateStats([]);
        updateChart([]);

        if (isAuthHttpError(error)) {
            handleSessionExpired();
            return;
        }

        const msg = 'Không thể tải báo cáo: ' + (error?.message || 'Lỗi kết nối máy chủ');
        showToast(msg, 'error');
        renderReportError(msg);
    }
}

async function seedTestData() {
    if (!confirm('Thao tác này sẽ tạo 5 booking mẫu để kiểm tra báo cáo. Tiếp tục?')) return;

    try {
        await TB.apiFetch('/api/v1/admin/generate-test-data', { method: 'POST' });
        showToast('Đã tạo dữ liệu test thành công!', 'success');
        loadReport();
    } catch (error) {
        if (isAuthHttpError(error)) {
            handleSessionExpired();
            return;
        }
        showToast('Không thể tạo dữ liệu test: ' + (error?.message || 'Lỗi hệ thống'), 'error');
    }
}

function renderReport(data) {
    const tbody = document.getElementById('reportBody');
    if (!data || data.length === 0) {
        tbody.innerHTML =
            `<tr><td colspan="5" style="text-align:center;padding:3rem;color:var(--text-muted);line-height:1.6;">${EMPTY_REPORT_MESSAGE}</td></tr>`;
        return;
    }

    tbody.innerHTML = data.map(item => `
        <tr>
            <td><strong>${item.period || item.date}</strong></td>
            <td>${item.bookingCount || 0}</td>
            <td style="color: var(--primary); font-weight: 700;">${formatCurrency(item.revenue || 0)}</td>
            <td>${formatCurrency(item.averageValue || 0)}</td>
            <td><span class="badge badge-danger" style="background: #fee2e2; color: #dc2626;">${item.cancellations || 0}</span></td>
        </tr>
    `).join('');
}

function renderReportError(message) {
    const tbody = document.getElementById('reportBody');
    tbody.innerHTML =
        `<tr><td colspan="5" style="text-align:center;padding:3rem;color:#dc2626;line-height:1.6;">${message}</td></tr>`;
}

function updateStats(data) {
    const totalRevenue = data.reduce((sum, item) => sum + (item.revenue || 0), 0);
    const totalBookings = data.reduce((sum, item) => sum + (item.bookingCount || 0), 0);
    const totalCancellations = data.reduce((sum, item) => sum + (item.cancellations || 0), 0);
    const avgBookingValue = totalBookings > 0 ? totalRevenue / totalBookings : 0;
    const cancelRate = totalBookings > 0 ? (totalCancellations / totalBookings * 100).toFixed(1) : 0;

    document.getElementById('totalRevenue').innerHTML = formatCurrency(totalRevenue);
    document.getElementById('totalBookings').innerHTML = totalBookings;
    document.getElementById('avgBookingValue').innerHTML = formatCurrency(avgBookingValue);
    document.getElementById('cancelRate').innerHTML = `${cancelRate}%`;
}

function setChartEmptyState(isEmpty) {
    const emptyEl = document.getElementById('chartEmptyMessage');
    if (emptyEl) {
        emptyEl.style.display = isEmpty ? 'flex' : 'none';
    }
}

function updateChart(data) {
    const ctx = document.getElementById('revenueChart').getContext('2d');
    if (revenueChart) revenueChart.destroy();

    if (!data || data.length === 0) {
        ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
        setChartEmptyState(true);
        return;
    }

    setChartEmptyState(false);

    revenueChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: data.map(item => item.period || item.date),
            datasets: [
                {
                    label: 'Revenue',
                    data: data.map(item => item.revenue || 0),
                    borderColor: '#059669',
                    backgroundColor: 'rgba(5, 150, 105, 0.1)',
                    fill: true,
                    tension: 0.4,
                    yAxisID: 'y'
                },
                {
                    label: 'Bookings',
                    data: data.map(item => item.bookingCount || 0),
                    borderColor: '#d97706',
                    backgroundColor: 'transparent',
                    borderDash: [5, 5],
                    tension: 0.4,
                    yAxisID: 'y1'
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'top', labels: { usePointStyle: true, font: { weight: '700' } } }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        font: { weight: '600' },
                        callback: (v) => v >= 1000000 ? (v / 1000000) + 'M' : (v >= 1000 ? (v / 1000) + 'K' : v)
                    }
                },
                y1: {
                    beginAtZero: true,
                    position: 'right',
                    grid: { drawOnChartArea: false },
                    ticks: { font: { weight: '600' } }
                }
            }
        }
    });
}

function exportToExcel() {
    if (!currentReportData || currentReportData.length === 0) {
        showToast('Không có dữ liệu để xuất Excel', 'warning');
        return;
    }

    const wsData = [
        ['FINANCIAL REPORT'],
        [`Generated: ${new Date().toLocaleString()}`],
        [],
        ['Period', 'Bookings', 'Revenue (VND)', 'Avg. Value', 'Cancellations'],
        ...currentReportData.map(item => [
            item.period || item.date,
            item.bookingCount || 0,
            item.revenue || 0,
            item.averageValue || 0,
            item.cancellations || 0
        ])
    ];

    const ws = XLSX.utils.aoa_to_sheet(wsData);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'FinancialData');
    XLSX.writeFile(wb, `Report_${formatLocalDate(new Date())}.xlsx`);
    showToast('Xuất Excel thành công', 'success');
}

async function exportToPDF() {
    if (!currentReportData || currentReportData.length === 0) {
        showToast('Không có dữ liệu để xuất PDF', 'warning');
        return;
    }

    try {
        const { jsPDF } = window.jspdf;
        const doc = new jsPDF({ orientation: 'landscape' });

        doc.setFontSize(22);
        doc.setTextColor(6, 78, 59);
        doc.text('Financial Report', 14, 20);

        doc.setFontSize(10);
        doc.setTextColor(107, 114, 128);
        doc.text(`Period: ${document.getElementById('startDate').value} to ${document.getElementById('endDate').value}`, 14, 30);
        doc.text(`Generated: ${new Date().toLocaleString()}`, 14, 35);

        const tableData = currentReportData.map(item => [
            item.period || item.date,
            item.bookingCount || 0,
            formatCurrency(item.revenue || 0),
            formatCurrency(item.averageValue || 0),
            item.cancellations || 0
        ]);

        doc.autoTable({
            startY: 45,
            head: [['Period', 'Bookings', 'Revenue', 'Avg. Value', 'Cancellations']],
            body: tableData,
            theme: 'grid',
            headStyles: { fillColor: [6, 78, 59], fontSize: 11 },
            alternateRowStyles: { fillColor: [249, 250, 251] }
        });

        doc.save(`Financial_Report_${formatLocalDate(new Date())}.pdf`);
        showToast('Xuất PDF thành công', 'success');
    } catch (error) {
        console.error('PDF Export error:', error);
        showToast('Xuất PDF thất bại: ' + (error?.message || 'Lỗi hệ thống'), 'error');
    }
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function initFinancialReportPage() {
    const currentUser = JSON.parse(sessionStorage.getItem('user') || 'null');
    if (!currentUser || currentUser.role !== 'ADMIN') {
        window.location.href = '/pages/auth/login.html';
        return;
    }

    document.getElementById('userInfo').innerText =
        currentUser.fullName || currentUser.FullName || currentUser.email;

    document.getElementById('logoutBtn').addEventListener('click', () => {
        if (window.TB && TB.logout) {
            TB.logout();
        } else {
            sessionStorage.removeItem('user');
            sessionStorage.removeItem('token');
            window.location.href = '/pages/auth/login.html';
        }
    });

    window.addEventListener('load', () => {
        const today = new Date();
        const startOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
        const startDate = formatLocalDate(startOfMonth);
        const endDate = formatLocalDate(today);

        document.getElementById('startDate').value = startDate;
        document.getElementById('endDate').value = endDate;

        loadReport({ autoLoad: true });
    });
}

initFinancialReportPage();
