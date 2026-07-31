document.addEventListener('DOMContentLoaded', () => {
    const userStr = sessionStorage.getItem('user');
    if (!userStr || JSON.parse(userStr).role !== 'GUIDE') {
        window.location.href = '/pages/auth/login.html';
    }
    
    const urlParams = new URLSearchParams(window.location.search);
    const scheduleId = urlParams.get('id');
    if (!scheduleId) {
        alert("Mã lịch trình không hợp lệ");
        window.location.href = '/pages/guide/dashboard.html';
        return;
    }

    const groupChatLink = document.getElementById('groupChatLink');
    if (groupChatLink) groupChatLink.href = `/pages/client/group-chat.html?scheduleId=${scheduleId}`;

    loadScheduleDetails(scheduleId);
});

async function loadScheduleDetails(sid) {
    try {
        const ts = new Date().getTime();
        const res = await TB.apiFetch(`/api/v1/guides/tours/${sid}?t=${ts}`, { cache: 'no-store' });
        if (res.code !== 200) throw new Error(res.message || 'Failed to fetch');
        
        const s = res.data;
        if (!s) return;
        
        document.getElementById('tourTitle').innerText = s.tourName || ('Schedule SD-' + sid);
        renderProgressHistory(s.progressLogs || []);
        renderUploadedGallery(s.imageUrls || []);

        // Check attendance status to enable/disable Update Progress
        checkAttendanceStatusForProgress(sid);
    } catch (err) {
        console.error('Error loading details:', err);
        document.getElementById('tourTitle').innerText = 'Error Loading Details';
    }
}

async function checkAttendanceStatusForProgress(sid) {
    try {
        const res = await TB.apiFetch(`/api/v1/guides/assigned-tours/${sid}/attendances`);
        const list = res.data || [];
        const btn = document.querySelector('button[onclick="updateProgress()"]');
        if (btn) {
            const hasPending = list.some(a => a.status === 'PENDING');
            if (hasPending || list.length === 0) {
                btn.disabled = true;
                btn.title = 'Vui lòng hoàn thành điểm danh tất cả khách hàng (Có mặt hoặc Vắng) để cập nhật tiến độ.';
                btn.style.opacity = '0.5';
                btn.style.cursor = 'not-allowed';
            } else {
                btn.disabled = false;
                btn.title = '';
                btn.style.opacity = '1';
                btn.style.cursor = 'pointer';
            }
        }
    } catch (e) {
        // Ignored
    }
}

function renderUploadedGallery(urls) {
    const cont = document.getElementById('uploadedGallery');
    if (!cont) return;
    
    if (urls.length === 0) {
        cont.innerHTML = '<div style="width:100%; font-size:0.85rem; color:var(--text-muted); padding:10px; border:1px dashed var(--border); border-radius:8px; text-align:center;">Chưa có ảnh nào.</div>';
        return;
    }

    cont.innerHTML = '<div style="width:100%; font-size:0.8rem; font-weight:700; color:var(--text-muted); margin-bottom:8px; text-transform:uppercase; letter-spacing:0.5px;">Ảnh hoạt động</div>';
    
    urls.forEach(url => {
        const fullUrl = TB.normalizeImageUrl(url);
        const img = document.createElement('img');
        img.src = fullUrl;
        img.className = 'preview-img';
        img.style.cssText = 'width:80px; height:80px; object-fit:cover; border-radius:10px; cursor:pointer; border:1px solid var(--border); transition:transform 0.2s;';
        img.onmouseover = () => img.style.transform = 'scale(1.05)';
        img.onmouseout = () => img.style.transform = 'scale(1)';
        img.onclick = () => window.open(url, '_blank');
        cont.appendChild(img);
    });
}

function renderProgressHistory(logs) {
    const cont = document.getElementById('progressHistory');
    if (!cont) return;
    
    if (logs.length === 0) {
        cont.innerHTML = ''; // Hide if no history
        return;
    }

    cont.innerHTML = '<h4 style="margin-bottom:10px; font-size: 0.9rem; color: var(--text-muted); text-transform: uppercase;">Lịch sử</h4>';
    logs.forEach(log => {
        const timeStr = new Date(log.createdAt).toLocaleString();
        cont.innerHTML += `
            <div style="margin-bottom: 10px; border-left: 2px solid var(--primary); padding-left: 10px; background: var(--bg-main); padding: 8px; border-radius: 4px;">
                <div style="font-size: 0.7rem; color: var(--text-muted);">${timeStr}</div>
                <div style="font-size: 0.85rem; color: var(--text-main); margin-top: 4px;">${log.content}</div>
            </div>
        `;
    });
}

function getScheduleId() {
    return new URLSearchParams(window.location.search).get('id');
}

// UC28
window.updateProgress = async function() {
    const progress = document.getElementById('progressInput').value.trim();
    if (!progress) return;
    
    const sid = getScheduleId();
    try {
        await TB.apiFetch(`/api/v1/guides/tours/${sid}/progress?progress=${encodeURIComponent(progress)}`, { method: 'PATCH' });
        alert('Cập nhật tiến độ thành công!');
        document.getElementById('progressInput').value = '';
        loadScheduleDetails(sid); // Refresh history
    } catch (err) {
        alert('Error: ' + err.message);
    }
};

// Preview
const photosInput = document.getElementById('photosInput');
const previewCont = document.getElementById('previewImages');
photosInput.addEventListener('change', () => {
    previewCont.innerHTML = '';
    Array.from(photosInput.files).forEach(file => {
        const url = URL.createObjectURL(file);
        const img = document.createElement('img');
        img.src = url;
        img.className = 'preview-img';
        previewCont.appendChild(img);
    });
});

// UC29 (Multipart Form)
window.uploadPhotos = async function() {
    const files = photosInput.files;
    if (files.length === 0) return alert('Vui lòng chọn ảnh trước.');
    const sid = getScheduleId();
    
    const formData = new FormData();
    for (let i = 0; i < files.length; i++) {
        formData.append('photos', files[i]);
    }
    
    const token = TB.getToken();
    try {
        const res = await fetch(`http://localhost:8080/api/v1/guides/tours/${sid}/photos`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
            body: formData
        });
        if (!res.ok) throw new Error('Upload failed');
        alert('Tải ảnh lên thành công!');
        photosInput.value = '';
        previewCont.innerHTML = '';
        loadScheduleDetails(sid); // Refresh gallery
    } catch (err) {
        alert('Error uploading photos: ' + err.message);
        photosInput.value = '';
        previewCont.innerHTML = '';
    }
};

// UC30
window.submitReport = async function() {
    const content = document.getElementById('reportInput').value.trim();
    if (!content) return alert('Vui lòng nhập nội dung báo cáo.');
    
    if (!confirm('Bạn có chắc muốn hoàn thành tour và lưu báo cáo không?')) return;
    
    const sid = getScheduleId();
    try {
        await TB.apiFetch(`/api/v1/guides/tours/${sid}/report?content=${encodeURIComponent(content)}`, { method: 'POST' });
        alert('Đã nộp báo cáo! Tour đã được đánh dấu hoàn thành.');
        window.location.href = '/pages/guide/dashboard.html';
    } catch (err) {
        alert('Mocked: Report saved. ' + err.message);
        window.location.href = '/pages/guide/dashboard.html';
    }
};
