(() => {
  function setActiveTab(tab) {
    document.querySelectorAll('.tab').forEach(t => {
      t.classList.toggle('active', t.dataset.tab === tab);
    });
    document.querySelectorAll('.form-section').forEach(s => {
      s.classList.toggle('active', s.id === `${tab}Section`);
    });
    document.querySelectorAll('.message').forEach(m => m.textContent = '');
  }

  document.addEventListener('click', (e) => {
    const tab = e.target?.dataset?.tab;
    if (tab) setActiveTab(tab);
  });

  const loginForm = document.getElementById('loginForm');
  const registerForm = document.getElementById('registerForm');
  const forgotForm = document.getElementById('forgotForm');

  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        email: document.getElementById('loginEmail').value,
        password: document.getElementById('loginPassword').value
      };
      const msgEl = document.getElementById('loginMessage');
      msgEl.style.color = '#333';
      msgEl.textContent = 'Logging in...';
      try {
        const res = await TB.apiFetch('/api/v1/auth/login', {
          method: 'POST',
          body: JSON.stringify(payload)
        });
        localStorage.setItem('token', res.data.token);
        localStorage.setItem('user', JSON.stringify(res.data.user));
        msgEl.style.color = 'green';
        msgEl.textContent = 'Login successful!';
        
        // Điều hướng theo vai trò (Role-based redirection)
        const role = (res.data.user.role || '').toString().toUpperCase();
        console.log('Login successful. Role:', role);
        
        let target = '/pages/index.html';
        if (role === 'STAFF') target = '/pages/staff/dashboard.html';
        else if (role === 'GUIDE') target = '/pages/guide/dashboard.html';
        
        console.log('Redirecting to:', target);
        setTimeout(() => window.location.href = target, 700);
      } catch (err) {
        msgEl.style.color = 'red';
        msgEl.textContent = err.message || 'Login failed';
      }
    });
  }

  if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        email: document.getElementById('regEmail').value,
        fullName: document.getElementById('regName').value,
        password: document.getElementById('regPassword').value
      };
      const msgEl = document.getElementById('registerMessage');
      msgEl.style.color = '#333';
      msgEl.textContent = 'Registering...';
      try {
        const res = await TB.apiFetch('/api/v1/auth/register', {
          method: 'POST',
          body: JSON.stringify(payload)
        });
        msgEl.style.color = 'green';
        msgEl.textContent = res.message || 'Registration successful. Please check your email to verify.';
      } catch (err) {
        msgEl.style.color = 'red';
        msgEl.textContent = err.message || 'Registration failed';
      }
    });
  }

  if (forgotForm) {
    forgotForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        email: document.getElementById('forgotEmail').value
      };
      const msgEl = document.getElementById('forgotMessage');
      msgEl.style.color = '#333';
      msgEl.textContent = 'Sending request...';
      try {
        const res = await TB.apiFetch('/api/v1/auth/forgot-password', {
          method: 'POST',
          body: JSON.stringify(payload)
        });
        msgEl.style.color = 'green';
        msgEl.textContent = res.message || 'If email exists, reset link has been sent.';
      } catch (err) {
        msgEl.style.color = 'red';
        msgEl.textContent = err.message || 'Failed to send reset link';
      }
    });
  }
  // Check for tab parameter on load
  window.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    const tab = params.get('tab');
    if (tab === 'login' || tab === 'register') {
      setActiveTab(tab);
    }
  });
})();
