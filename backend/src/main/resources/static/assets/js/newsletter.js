(() => {
  const form = document.getElementById('form');
  const msg = document.getElementById('msg');
  const user = localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')) : null;
  const navActions = document.getElementById('navActions');
  if (user) {
    const pill = document.createElement('span');
    pill.className = 'pill';
    pill.textContent = `Hi, ${user.fullName || user.email}`;
    navActions.appendChild(pill);

    const logoutBtn = document.createElement('button');
    logoutBtn.className = 'btn btn-secondary';
    logoutBtn.textContent = 'Logout';
    logoutBtn.onclick = () => TB.logout();
    navActions.appendChild(logoutBtn);
  } else {
    const loginLink = document.createElement('a');
    loginLink.className = 'btn btn-secondary';
    loginLink.href = '/pages/auth/login.html';
    loginLink.textContent = 'Login';
    navActions.appendChild(loginLink);
  }

  function setMsg(text, color) {
    msg.textContent = text;
    msg.style.color = color || '#333';
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('email').value;
    setMsg('Subscribing...', '#333');
    try {
      const res = await TB.apiFetch('/api/v1/newsletters', {
        method: 'POST',
        body: JSON.stringify({ email })
      });
      setMsg(res.message || 'Subscribed!', 'green');
    } catch (err) {
      setMsg(err.message || 'Failed to subscribe', 'red');
    }
  });
})();

