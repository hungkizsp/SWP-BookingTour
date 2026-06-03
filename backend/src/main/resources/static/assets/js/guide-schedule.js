document.addEventListener('DOMContentLoaded', () => {
    const userStr = localStorage.getItem('user');
    if (!userStr || JSON.parse(userStr).role !== 'GUIDE') {
        window.location.href = '/pages/auth/login.html';
    }
    
    const urlParams = new URLSearchParams(window.location.search);
    const scheduleId = urlParams.get('id');
    if (!scheduleId) {
        alert("Invalid Schedule ID");
        window.location.href = '/pages/guide/dashboard.html';
        return;
    }
    
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
    } catch (err) {
        console.error('Error loading details:', err);
        document.getElementById('tourTitle').innerText = 'Error Loading Details';
    }
}

function renderUploadedGallery(urls) {
    const cont = document.getElementById('uploadedGallery');
    if (!cont) return;
    
    if (urls.length === 0) {
        cont.innerHTML = '<div style="width:100%; font-size:0.85rem; color:#94a3b8; padding:10px; border:1px dashed #e2e8f0; border-radius:8px; text-align:center;">No photos uploaded yet.</div>';
        return;
    }

    cont.innerHTML = '<div style="width:100%; font-size:0.8rem; font-weight:700; color:#64748b; margin-bottom:8px; text-transform:uppercase; letter-spacing:0.5px;">Activity Photos</div>';
    
    urls.forEach(url => {
        const img = document.createElement('img');
        img.src = url;
        img.className = 'preview-img';
        img.style.cssText = 'width:80px; height:80px; object-fit:cover; border-radius:10px; cursor:pointer; border:1px solid #f1f5f9; transition:transform 0.2s;';
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

    cont.innerHTML = '<h4 style="margin-bottom:10px; font-size: 0.9rem; color: #64748b;">HISTORY</h4>';
    logs.forEach(log => {
        const timeStr = new Date(log.createdAt).toLocaleString();
        cont.innerHTML += `
            <div style="margin-bottom: 10px; border-left: 2px solid #0f766e; padding-left: 10px; background: #f8fafc; padding: 8px; border-radius: 4px;">
                <div style="font-size: 0.7rem; color: #64748b;">${timeStr}</div>
                <div style="font-size: 0.85rem;">${log.content}</div>
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
        alert('Progress updated successfully!');
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
    if (files.length === 0) return alert('Select photos first.');
    const sid = getScheduleId();
    
    const formData = new FormData();
    for (let i = 0; i < files.length; i++) {
        formData.append('photos', files[i]);
    }
    
    const token = TB.getToken();
    try {
        const res = await fetch(`/api/v1/guides/tours/${sid}/photos`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
            body: formData
        });
        if (!res.ok) throw new Error('Upload failed');
        alert('Upload success!');
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
    if (!content) return alert('Report content required.');
    
    if (!confirm('Are you sure you want to finalize this tour and save report?')) return;
    
    const sid = getScheduleId();
    try {
        await TB.apiFetch(`/api/v1/guides/tours/${sid}/report?content=${encodeURIComponent(content)}`, { method: 'POST' });
        alert('Report submitted! Tour marked as completed.');
        window.location.href = '/pages/guide/dashboard.html';
    } catch (err) {
        alert('Mocked: Report saved. ' + err.message);
        window.location.href = '/pages/guide/dashboard.html';
    }
};
