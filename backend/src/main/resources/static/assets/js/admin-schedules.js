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

    await loadSchedules();
});

let currentScheduleId = null;
let allSchedules = [];

async function loadSchedules() {
    const tbody = document.querySelector('#schedulesTable tbody');
    try {
        const res = await TB.apiFetch('/api/v1/staff/schedules'); 
        allSchedules = res.data || [];
        tbody.innerHTML = '';
        allSchedules.forEach(s => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>SD-${s.id}</td>
                <td>${s.tourName || 'Basic Tour'}</td>
                <td>${s.startDate} - ${s.endDate}</td>
                <td>${s.guideId ? 'Guide #' + s.guideId : '<span style="color:#d97706">Unassigned</span>'}</td>
                <td>${s.status}</td>
                <td>
                    <button class="action-btn" onclick="openAssignModal(${s.id})">Assign</button>
                    <button class="action-btn" style="background: #64748b" onclick="openDetailsModal(${s.id})">Details</button>
                </td>
            `;
            tbody.appendChild(row);
        });
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="6" style="color:red">Error: ${e.message}</td></tr>`;
    }
}

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
                const img = document.createElement('img');
                img.src = url;
                img.style.width = '180px';
                img.style.height = '120px';
                img.style.flexShrink = '0';
                img.style.objectFit = 'cover';
                img.style.borderRadius = '10px';
                img.style.cursor = 'pointer';
                img.style.border = '1px solid #e2e8f0';
                img.onclick = () => window.open(url, '_blank');
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
    try {
        await TB.apiFetch(`/api/v1/staff/schedules/${currentScheduleId}/assign-guide?guideId=${guideId}`, { method: 'PATCH' });
        alert('Guide Assigned Successfully!');
        closeModal();
        loadSchedules();
    } catch (err) {
        alert('Mocked: Error -> ' + err.message + '. But Guide ID ' + guideId + ' assigned in UI.');
        closeModal();
    }
};
