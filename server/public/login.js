// Login Page JavaScript

// Get API base URL
function getApiBaseUrl() {
    // Nếu đang chạy trên localhost:3000 thì dùng relative path
    if (window.location.port === '3000' || window.location.hostname === 'localhost') {
        return '';
    }
    // Nếu chạy qua Live Server hoặc port khác, dùng localhost:3000
    return 'http://localhost:3000';
}

document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const togglePassword = document.getElementById('togglePassword');
    const rememberMe = document.getElementById('rememberMe');
    const errorMessage = document.getElementById('errorMessage');
    const btnLogin = document.getElementById('btnLogin');

    // Toggle password visibility
    togglePassword.addEventListener('click', () => {
        const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
        passwordInput.setAttribute('type', type);
        
        const icon = togglePassword.querySelector('i');
        icon.classList.toggle('fa-eye');
        icon.classList.toggle('fa-eye-slash');
    });

    // Check if already logged in
    const token = localStorage.getItem('adminToken');
    if (token) {
        // Verify token by checking user info
        verifyTokenAndRedirect(token);
    }

    // Handle form submission
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const email = emailInput.value.trim();
        const password = passwordInput.value;
        
        if (!email || !password) {
            showError('Vui lòng nhập đầy đủ email và mật khẩu');
            return;
        }

        await login(email, password);
    });

    // Get API base URL
    function getApiBaseUrl() {
        // Nếu đang chạy trên localhost:3000 thì dùng relative path
        if (window.location.port === '3000' || window.location.hostname === 'localhost') {
            return '';
        }
        // Nếu chạy qua Live Server hoặc port khác, dùng localhost:3000
        return 'http://localhost:3000';
    }

    // Login function
    async function login(email, password) {
        // Show loading state
        btnLogin.disabled = true;
        btnLogin.classList.add('loading');
        hideError();

        try {
            const apiBaseUrl = getApiBaseUrl();
            const response = await fetch(`${apiBaseUrl}/api/users/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email, password })
            });

            // Kiểm tra response có content không
            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                const text = await response.text();
                console.error('Server response is not JSON:', text);
                throw new Error('Không thể kết nối đến server. Vui lòng đảm bảo server đang chạy trên port 3000.');
            }

            // Kiểm tra response có body không
            if (!response.body) {
                throw new Error('Server không phản hồi. Vui lòng kiểm tra server có đang chạy không.');
            }

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || 'Đăng nhập thất bại');
            }

            // Check if user is admin
            if (data.user.role !== 'admin') {
                throw new Error('Bạn không có quyền truy cập. Chỉ admin mới có thể đăng nhập.');
            }

            // Save token and user info
            if (rememberMe.checked) {
                localStorage.setItem('adminToken', data.token);
                localStorage.setItem('adminUser', JSON.stringify(data.user));
            } else {
                sessionStorage.setItem('adminToken', data.token);
                sessionStorage.setItem('adminUser', JSON.stringify(data.user));
            }

            // Redirect to admin panel
            window.location.href = `${apiBaseUrl}/admin.html`;

        } catch (error) {
            console.error('Login error:', error);
            if (error.message.includes('fetch')) {
                showError('Không thể kết nối đến server. Vui lòng đảm bảo server Node.js đang chạy trên port 3000.');
            } else {
                showError(error.message || 'Đăng nhập thất bại. Vui lòng thử lại.');
            }
        } finally {
            btnLogin.disabled = false;
            btnLogin.classList.remove('loading');
        }
    }

    // Verify token and redirect
    async function verifyTokenAndRedirect(token) {
        try {
            const apiBaseUrl = getApiBaseUrl();
            const response = await fetch(`${apiBaseUrl}/api/users/profile`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    const data = await response.json();
                    if (data.user && data.user.role === 'admin') {
                        window.location.href = `${apiBaseUrl}/admin.html`;
                    }
                }
            }
        } catch (error) {
            // Token invalid, stay on login page
            localStorage.removeItem('adminToken');
            sessionStorage.removeItem('adminToken');
        }
    }

    // Show error message
    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.classList.add('show');
        
        // Auto hide after 5 seconds
        setTimeout(() => {
            hideError();
        }, 5000);
    }

    // Hide error message
    function hideError() {
        errorMessage.classList.remove('show');
    }

    // Focus on email input
    emailInput.focus();
});

