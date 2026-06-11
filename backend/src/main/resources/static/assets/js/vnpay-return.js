/**
 * vnpay-return.js — Xác nhận kết quả thanh toán VNPay với Backend
 */
window.addEventListener('load', () => {
    confirmVNPayReturn();
});

async function confirmVNPayReturn() {
    const iconEl = document.getElementById('statusIcon');
    const titleEl = document.getElementById('statusTitle');
    const msgEl = document.getElementById('statusMessage');
    const actionLinks = document.getElementById('actionLinks');

    const query = window.location.search;
    if (!query || query.length <= 1) {
        showFailure(iconEl, titleEl, msgEl, actionLinks,
            'Thiếu thông tin giao dịch từ VNPay.',
            'Vui lòng kiểm tra lại lịch sử đặt tour hoặc thử thanh toán lại.');
        return;
    }

    try {
        const response = await TB.apiFetch(`/api/v1/payments/vnpay/confirm${query}`);
        const result = response?.data ?? response;

        if (result?.success) {
            iconEl.innerText = '✅';
            iconEl.classList.add('success');
            titleEl.innerText = 'Thanh toán thành công!';
            msgEl.innerHTML =
                `${result.message || 'Giao dịch đã được ghi nhận.'}<br>` +
                `Mã tham chiếu: <b>${escapeHtml(result.transactionRef || '—')}</b><br>` +
                `Bạn sẽ được chuyển đến <b>Tour đã đặt</b> trong <span id="countdown">5</span> giây...`;

            let timeLeft = 5;
            const timer = setInterval(() => {
                timeLeft--;
                const countdownEl = document.getElementById('countdown');
                if (countdownEl) countdownEl.innerText = timeLeft;
                if (timeLeft <= 0) {
                    clearInterval(timer);
                    window.location.href = '../../user/bookings.html';
                }
            }, 1000);
            return;
        }

        showFailure(
            iconEl,
            titleEl,
            msgEl,
            actionLinks,
            'Thanh toán không thành công',
            result?.message || 'Giao dịch VNPay chưa hoàn tất. Bạn có thể thử thanh toán lại từ trang đặt tour.'
        );
    } catch (error) {
        console.error('VNPay confirm error:', error);
        showFailure(
            iconEl,
            titleEl,
            msgEl,
            actionLinks,
            'Không thể xác nhận giao dịch',
            error?.message || 'Hệ thống gặp lỗi khi xác nhận thanh toán. Vui lòng liên hệ hỗ trợ nếu tiền đã bị trừ.'
        );
    }
}

function showFailure(iconEl, titleEl, msgEl, actionLinks, title, message) {
    iconEl.innerText = '❌';
    iconEl.classList.add('error');
    titleEl.innerText = title;
    msgEl.innerText = message;
    actionLinks.style.display = 'flex';
}

function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
