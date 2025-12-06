// TOCO Admin JavaScript
// Kết nối với client.js để quản lý admin

let adminClient;
let currentUser = null;
let categories = [];

// Khởi tạo
document.addEventListener('DOMContentLoaded', () => {
    // Kiểm tra đăng nhập
    checkAuth();
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

// Kiểm tra authentication
async function checkAuth() {
    // Lấy token từ localStorage hoặc sessionStorage
    const token = localStorage.getItem('adminToken') || sessionStorage.getItem('adminToken');
    
    if (!token) {
        // Chưa đăng nhập, redirect đến trang login
        const apiBaseUrl = getApiBaseUrl();
        window.location.href = `${apiBaseUrl}/login.html`;
        return;
    }
    
    // Verify token và kiểm tra role admin
    try {
        const apiBaseUrl = getApiBaseUrl();
        const response = await fetch(`${apiBaseUrl}/api/users/profile`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            throw new Error('Token không hợp lệ');
        }
        
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            throw new Error('Server response không hợp lệ');
        }
        
        const data = await response.json();
        
        // Kiểm tra role admin
        if (!data.user || data.user.role !== 'admin') {
            // Không phải admin, xóa token và redirect
            localStorage.removeItem('adminToken');
            sessionStorage.removeItem('adminToken');
            localStorage.removeItem('adminUser');
            sessionStorage.removeItem('adminUser');
            alert('Bạn không có quyền truy cập. Chỉ admin mới có thể đăng nhập.');
            const apiBaseUrl = getApiBaseUrl();
            window.location.href = `${apiBaseUrl}/login.html`;
            return;
        }
        
        // Lưu user info
        currentUser = data.user;
        const userInfo = localStorage.getItem('adminUser') || sessionStorage.getItem('adminUser');
        if (userInfo) {
            currentUser = JSON.parse(userInfo);
        }
        
        // Khởi tạo admin client
        adminClient = new AdminClient(token);
        
        // Lấy thông tin user
        loadUserInfo();
        
        // Setup navigation
        setupNavigation();
        
        // Setup event listeners
        setupEventListeners();
        
        // Load dashboard
        showPage('dashboard');
        
    } catch (error) {
        console.error('Auth error:', error);
        // Token không hợp lệ, xóa và redirect
        localStorage.removeItem('adminToken');
        sessionStorage.removeItem('adminToken');
        localStorage.removeItem('adminUser');
        sessionStorage.removeItem('adminUser');
        const apiBaseUrl = getApiBaseUrl();
        window.location.href = `${apiBaseUrl}/login.html`;
    }
}

// Setup Navigation
function setupNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const page = item.getAttribute('data-page');
            showPage(page);
            
            // Update active state
            navItems.forEach(nav => nav.classList.remove('active'));
            item.classList.add('active');
        });
    });
}

// Show Page
function showPage(pageName) {
    // Hide all pages
    document.querySelectorAll('.page-content').forEach(page => {
        page.classList.add('hidden');
    });
    
    // Show selected page
    const page = document.getElementById(`page-${pageName}`);
    if (page) {
        page.classList.remove('hidden');
    }
    
    // Update page title
    const titles = {
        dashboard: 'Trang chủ',
        categories: 'Quản lý danh mục',
        products: 'Quản lý sản phẩm',
        users: 'Quản lý người dùng',
        orders: 'Quản lý đơn hàng',
        vouchers: 'Quản lý Voucher',
        reviews: 'Quản lý đánh giá',
        statistics: 'Thống kê doanh thu'
    };
    document.getElementById('pageTitle').textContent = titles[pageName] || 'TOCO';
    
    // Load page data
    switch(pageName) {
        case 'dashboard':
            loadDashboard();
            break;
        case 'categories':
            loadCategories();
            break;
        case 'products':
            loadProducts();
            loadCategoriesForProduct();
            break;
        case 'users':
            loadUsers();
            break;
        case 'orders':
            loadOrders();
            break;
        case 'vouchers':
            loadVouchers();
            break;
        case 'reviews':
            loadReviews();
            break;
        case 'statistics':
            // Statistics page doesn't need initial load
            break;
    }
}

// Setup Event Listeners
function setupEventListeners() {
    // Toggle sidebar
    document.getElementById('toggleSidebar').addEventListener('click', () => {
        document.querySelector('.sidebar').classList.toggle('show');
    });
    
    // Logout
    document.getElementById('btnLogout').addEventListener('click', () => {
        if (confirm('Bạn có chắc muốn đăng xuất?')) {
            // Xóa tất cả thông tin đăng nhập
            localStorage.removeItem('adminToken');
            localStorage.removeItem('adminUser');
            sessionStorage.removeItem('adminToken');
            sessionStorage.removeItem('adminUser');
            window.location.href = '/login.html';
        }
    });
    
    // Products
    document.getElementById('btnAddProduct').addEventListener('click', () => {
        openProductModal();
    });
    document.getElementById('btnSaveProduct').addEventListener('click', saveProduct);
    document.getElementById('searchProducts').addEventListener('input', debounce(loadProducts, 300));
    document.getElementById('filterCategory').addEventListener('change', loadProducts);
    
    // Variants
    document.getElementById('btnAddVariant').addEventListener('click', addVariant);
    
    // Refresh variant dropdowns when colors/sizes change
    document.getElementById('productColors').addEventListener('blur', refreshVariantDropdowns);
    document.getElementById('productSizes').addEventListener('blur', refreshVariantDropdowns);
    
    // Update total stock when variants change
    document.addEventListener('input', function(e) {
        if (e.target.classList.contains('variant-stock')) {
            updateTotalStock();
        }
    });
    
    // Vouchers
    document.getElementById('btnAddVoucher').addEventListener('click', () => {
        openVoucherModal();
    });
    document.getElementById('btnSaveVoucher').addEventListener('click', saveVoucher);
    
    // Orders
    document.getElementById('filterOrderStatus').addEventListener('change', loadOrders);
    document.getElementById('btnSaveOrderStatus').addEventListener('click', saveOrderStatus);
    
    // Reviews
    document.getElementById('btnSaveReviewReply').addEventListener('click', saveReviewReply);
    
    // Statistics
    document.getElementById('btnGetRevenue').addEventListener('click', loadRevenue);
    
    // Categories
    document.getElementById('btnAddCategory').addEventListener('click', () => {
        openCategoryModal();
    });
    document.getElementById('btnSaveCategory').addEventListener('click', saveCategory);
}

// Load User Info
async function loadUserInfo() {
    try {
        if (currentUser) {
            document.getElementById('userName').textContent = currentUser.name || currentUser.email || 'Admin';
        } else {
            // Fallback: lấy từ localStorage/sessionStorage
            const userInfo = localStorage.getItem('adminUser') || sessionStorage.getItem('adminUser');
            if (userInfo) {
                const user = JSON.parse(userInfo);
                document.getElementById('userName').textContent = user.name || user.email || 'Admin';
            } else {
                document.getElementById('userName').textContent = 'Admin';
            }
        }
    } catch (error) {
        showToast('Lỗi tải thông tin người dùng', 'error');
        document.getElementById('userName').textContent = 'Admin';
    }
}

// Load Trang chủ
async function loadDashboard() {
    showLoading();
    try {
        const [products, users, orders] = await Promise.all([
            adminClient.getAllProducts(),
            adminClient.getAllUsers(),
            adminClient.getAllOrders()
        ]);
        
        document.getElementById('totalProducts').textContent = products.length || 0;
        document.getElementById('totalUsers').textContent = users.length || 0;
        document.getElementById('totalOrders').textContent = orders.length || 0;
        
        // Calculate revenue for current month
        const now = new Date();
        const startDate = `${now.getFullYear()}-${now.getMonth() + 1}-1`;
        const endDate = `${now.getFullYear()}-${now.getMonth() + 1}-${new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()}`;
        
        try {
            const revenue = await adminClient.getRevenue(startDate, endDate);
            document.getElementById('totalRevenue').textContent = AdminClient.formatMoney(revenue.totalRevenue) + '₫';
        } catch (error) {
            document.getElementById('totalRevenue').textContent = '0₫';
        }
    } catch (error) {
        showToast('Lỗi tải dashboard: ' + error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Load Products
async function loadProducts() {
    showLoading();
    try {
        const search = document.getElementById('searchProducts').value;
        const category = document.getElementById('filterCategory').value;
        const products = await adminClient.getAllProducts(search, category);
        
        const tbody = document.getElementById('productsTableBody');
        if (products.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">Không có sản phẩm nào</td></tr>';
            return;
        }
        
        tbody.innerHTML = products.map(product => {
            const imageUrl = product.image?.startsWith('/') 
                ? `http://localhost:3000${product.image}` 
                : product.image || '/placeholder.jpg';
            
            return `
                <tr>
                    <td><img src="${imageUrl}" alt="${product.name}"></td>
                    <td>${product.name}</td>
                    <td>${AdminClient.formatMoney(product.price)}₫</td>
                    <td>${product.stock || 0}</td>
                    <td>${product.sold || 0}</td>
                    <td>
                        <div class="action-buttons">
                            <button class="action-btn action-btn-edit" onclick="editProduct('${product._id || product.id}')">
                                <i class="fas fa-edit"></i> Sửa
                            </button>
                            <button class="action-btn action-btn-delete" onclick="deleteProduct('${product._id || product.id}')">
                                <i class="fas fa-trash"></i> Xóa
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        showToast('Lỗi tải sản phẩm: ' + error.message, 'error');
        document.getElementById('productsTableBody').innerHTML = 
            '<tr><td colspan="6" class="text-center">Lỗi tải dữ liệu</td></tr>';
    } finally {
        hideLoading();
    }
}

// Load Categories for Product Form (dropdown)
async function loadCategoriesForProduct() {
    try {
        categories = await adminClient.getCategories();
        const select = document.getElementById('productCategory');
        const filterSelect = document.getElementById('filterCategory');
        
        if (select) {
            const options = categories.map(cat => 
                `<option value="${cat._id || cat.id}">${cat.name}</option>`
            ).join('');
            select.innerHTML = '<option value="">Chọn danh mục</option>' + options;
        }
        
        if (filterSelect) {
            const options = categories.map(cat => 
                `<option value="${cat._id || cat.id}">${cat.name}</option>`
            ).join('');
            filterSelect.innerHTML = '<option value="">Tất cả danh mục</option>' + options;
        }
    } catch (error) {
        showToast('Lỗi tải danh mục: ' + error.message, 'error');
    }
}

// Load Categories (for Categories Management Page)
async function loadCategories() {
    showLoading();
    try {
        const categories = await adminClient.getCategories();
        const tbody = document.getElementById('categoriesTableBody');
        
        if (categories.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center">Không có danh mục nào</td></tr>';
            return;
        }
        
        tbody.innerHTML = categories.map(category => {
            const createdAt = AdminClient.formatDate(category.createdAt || new Date());
            
            return `
                <tr>
                    <td><strong>${category.name}</strong></td>
                    <td>${category.description || 'N/A'}</td>
                    <td>${createdAt}</td>
                    <td>
                        <div class="action-buttons">
                            <button class="action-btn action-btn-edit" onclick="editCategory('${category._id || category.id}')">
                                <i class="fas fa-edit"></i> Sửa
                            </button>
                            <button class="action-btn action-btn-delete" onclick="deleteCategory('${category._id || category.id}')">
                                <i class="fas fa-trash"></i> Xóa
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        showToast('Lỗi tải danh mục: ' + error.message, 'error');
        document.getElementById('categoriesTableBody').innerHTML = 
            '<tr><td colspan="4" class="text-center">Lỗi tải dữ liệu</td></tr>';
    } finally {
        hideLoading();
    }
}

// Open Category Modal
function openCategoryModal(categoryId = null) {
    const modal = document.getElementById('categoryModal');
    const form = document.getElementById('categoryForm');
    form.reset();
    
    if (categoryId) {
        document.getElementById('categoryModalTitle').textContent = 'Sửa danh mục';
        document.getElementById('categoryId').value = categoryId;
        loadCategoryData(categoryId);
    } else {
        document.getElementById('categoryModalTitle').textContent = 'Thêm danh mục';
        document.getElementById('categoryId').value = '';
    }
    
    modal.classList.add('show');
}

// Load Category Data
async function loadCategoryData(categoryId) {
    try {
        const categories = await adminClient.getCategories();
        const category = categories.find(c => (c._id || c.id) === categoryId);
        
        if (category) {
            document.getElementById('categoryName').value = category.name || '';
            document.getElementById('categoryDescription').value = category.description || '';
        }
    } catch (error) {
        showToast('Lỗi tải dữ liệu danh mục: ' + error.message, 'error');
    }
}

// Edit Category
function editCategory(categoryId) {
    openCategoryModal(categoryId);
}

// Save Category
async function saveCategory() {
    const form = document.getElementById('categoryForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    showLoading();
    try {
        const categoryId = document.getElementById('categoryId').value;
        const categoryData = {
            name: document.getElementById('categoryName').value.trim(),
            description: document.getElementById('categoryDescription').value.trim()
        };
        
        if (!categoryData.name) {
            showToast('Vui lòng nhập tên danh mục', 'error');
            return;
        }
        
        if (categoryId) {
            await adminClient.updateCategory(categoryId, categoryData);
            showToast('Cập nhật danh mục thành công!', 'success');
        } else {
            await adminClient.createCategory(categoryData);
            showToast('Thêm danh mục thành công!', 'success');
        }
        
        closeModal('categoryModal');
        loadCategories();
        // Reload categories for product form if on products page
        if (document.getElementById('page-products') && !document.getElementById('page-products').classList.contains('hidden')) {
            loadCategoriesForProduct();
        }
    } catch (error) {
        showToast('Lỗi: ' + error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Delete Category
async function deleteCategory(categoryId) {
    if (!confirm('Bạn có chắc muốn xóa danh mục này? Các sản phẩm thuộc danh mục này sẽ bị ảnh hưởng.')) return;
    
    showLoading();
    try {
        await adminClient.deleteCategory(categoryId);
        showToast('Xóa danh mục thành công!', 'success');
        loadCategories();
        // Reload categories for product form if on products page
        if (document.getElementById('page-products') && !document.getElementById('page-products').classList.contains('hidden')) {
            loadCategoriesForProduct();
        }
    } catch (error) {
        showToast('Lỗi: ' + error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Open Product Modal
function openProductModal(productId = null) {
    const modal = document.getElementById('productModal');
    const form = document.getElementById('productForm');
    form.reset();
    document.getElementById('productImagePreview').style.display = 'none';
    clearVariants(); // Clear variants list
    
    if (productId) {
        document.getElementById('productModalTitle').textContent = 'Sửa sản phẩm';
        document.getElementById('productId').value = productId;
        // Load product data
        loadProductData(productId);
    } else {
        document.getElementById('productModalTitle').textContent = 'Thêm sản phẩm';
        document.getElementById('productId').value = '';
        // Add one empty variant by default
        addVariant();
    }
    
    modal.classList.add('show');
}

// Load Product Data
async function loadProductData(productId) {
    try {
        const products = await adminClient.getAllProducts();
        const product = products.find(p => (p._id || p.id) === productId);
        
        if (product) {
            document.getElementById('productName').value = product.name || '';
            document.getElementById('productDescription').value = product.description || '';
            document.getElementById('productPrice').value = product.price || 0;
            document.getElementById('productStock').value = product.stock || 0;
            document.getElementById('productCategory').value = product.category?._id || product.category || '';
            document.getElementById('productColors').value = Array.isArray(product.colors) 
                ? product.colors.join(', ') 
                : (product.colors || '');
            document.getElementById('productSizes').value = Array.isArray(product.sizes) 
                ? product.sizes.join(', ') 
                : (product.sizes || '');
            
            // Load variants
            clearVariants();
            if (product.variants && product.variants.length > 0) {
                product.variants.forEach(variant => {
                    addVariant(variant.color, variant.size, variant.stock);
                });
                // Update total stock
                updateTotalStock();
            } else {
                // If no variants, add one empty
                addVariant();
                updateTotalStock();
            }
            
            if (product.image) {
                const imageUrl = product.image.startsWith('/') 
                    ? `http://localhost:3000${product.image}` 
                    : product.image;
                document.getElementById('productImagePreview').src = imageUrl;
                document.getElementById('productImagePreview').style.display = 'block';
            }
        }
    } catch (error) {
        showToast('Lỗi tải dữ liệu sản phẩm: ' + error.message, 'error');
    }
}

// Edit Product
function editProduct(productId) {
    openProductModal(productId);
}

// Save Product
async function saveProduct() {
    const form = document.getElementById('productForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    // Validate variants
    const variants = getVariants();
    if (variants.length === 0) {
        showToast('Vui lòng thêm ít nhất một biến thể (màu + size + số lượng)', 'error');
        return;
    }
    
    // Validate all variants have color, size and stock
    for (let i = 0; i < variants.length; i++) {
        const variant = variants[i];
        if (!variant.color || !variant.size || variant.stock === undefined || variant.stock === '') {
            showToast(`Biến thể ${i + 1} thiếu thông tin (màu, size hoặc số lượng)`, 'error');
            return;
        }
    }
    
    // Calculate total stock from variants
    const totalStock = variants.reduce((sum, v) => sum + (parseInt(v.stock) || 0), 0);
    
    showLoading();
    try {
        const productId = document.getElementById('productId').value;
        const productData = {
            name: document.getElementById('productName').value,
            description: document.getElementById('productDescription').value,
            price: parseFloat(document.getElementById('productPrice').value),
            stock: totalStock, // Use calculated total stock
            category: document.getElementById('productCategory').value,
            colors: document.getElementById('productColors').value,
            sizes: document.getElementById('productSizes').value,
            variants: variants
        };
        
        const imageFile = document.getElementById('productImage').files[0];
        const formData = AdminClient.createProductFormData(productData, imageFile);
        
        if (productId) {
            await adminClient.updateProduct(productId, formData);
            showToast('Cập nhật sản phẩm thành công!', 'success');
        } else {
            await adminClient.createProduct(formData);
            showToast('Thêm sản phẩm thành công!', 'success');
        }
        
        closeModal('productModal');
        loadProducts();
    } catch (error) {
        showToast('Lỗi: ' + error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Delete Product
async function deleteProduct(productId) {
    if (!confirm('Bạn có chắc muốn xóa sản phẩm này?')) return;
    
    showLoading();
    try {
        await adminClient.deleteProduct(productId);
        showToast('Xóa sản phẩm thành công!', 'success');
        loadProducts();
    } catch (error) {
        showToast('Lỗi: ' + error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Load Users
async function loadUsers() {
    showLoading();
    try {
        const users = await adminClient.getAllUsers();
        const tbody = document.getElementById('usersTableBody');
        
        if (users.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">Không có người dùng nào</td></tr>';
            return;
        }
        
        tbody.innerHTML = users.map(user => {
            const role = user.role === 'admin' ? 'Admin' : 'Người dùng';
            const roleClass = user.role === 'admin' ? 'badge-danger' : 'badge-info';
            const createdAt = AdminClient.formatDate(user.createdAt || new Date());
            
            return `
                <tr>
                    <td>${user.name || 'N/A'}</td>
                    <td>${user.email || 'N/A'}</td>
                    <td><span class="badge ${roleClass}">${role}</span></td>
                    <td>${createdAt}</td>
                    <td>
                        <div class="action-buttons">
                            <button class="action-btn action-btn-edit" onclick="changeUserRole('${user.id || user._id}', '${user.role}')">
                                <i class="fas fa-user-edit"></i> Đổi role
                            </button>
                            <button class="action-btn action-btn-delete" onclick="deleteUser('${user.id || user._id}')">
                                <i class="fas fa-trash"></i> Xóa
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        showToast('Lỗi tải người dùng: ' + error.message, 'error');
        document.getElementById('usersTableBody').innerHTML = 
            '<tr><td colspan="5" class="text-center">Lỗi tải dữ liệu</td></tr>';
    } finally {
        hideLoading();
    }
}

// Change User Role
async function changeUserRole(userId, currentRole) {
    const newRole = currentRole === 'admin' ? 'user' : 'admin';
    if (!confirm(`Bạn có chắc muốn đổi role của người dùng này thành ${newRole === 'admin' ? 'Admin' : 'Người dùng'}?`)) return;
    
    showLoading();
    try {
        await adminClient.updateUserRole(userId, newRole);
        showToast('Đổi role thành công!', 'success');
        loadUsers();
    } catch (error) {
        showToast('Lỗi: ' + error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Delete User
async function deleteUser(userId) {
    if (!confirm('Bạn có chắc muốn xóa người dùng này?')) return;
    
    showLoading();
    try {
        await adminClient.deleteUser(userId);
        showToast('Xóa người dùng thành công!', 'success');
        loadUsers();
    } catch (error) {
        showToast('Lỗi: ' + error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Load Orders
async function loadOrders() {
    showLoading();
    try {
        const status = document.getElementById('filterOrderStatus').value;
        const orders = await adminClient.getAllOrders(status);
        const tbody = document.getElementById('ordersTableBody');
        
        if (orders.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">Không có đơn hàng nào</td></tr>';
            return;
        }
        
        tbody.innerHTML = orders.map(order => {
            const statusMap = {
                pending: { text: 'Chờ xác nhận', class: 'badge-warning' },
                processing: { text: 'Đang xử lý', class: 'badge-info' },
                shipped: { text: 'Đang giao', class: 'badge-info' },
                delivered: { text: 'Hoàn thành', class: 'badge-success' },
                cancelled: { text: 'Đã hủy', class: 'badge-danger' }
            };
            const statusInfo = statusMap[order.status] || { text: order.status, class: 'badge-secondary' };
            const createdAt = AdminClient.formatDate(order.createdAt || new Date());
            const customerName = order.user?.name || 'N/A';
            const isDelivered = order.status === 'delivered';
            const isCancelled = order.status === 'cancelled';
            const canEdit = !isDelivered && !isCancelled;
            
            return `
                <tr>
                    <td>${order._id || order.id}</td>
                    <td>${customerName}</td>
                    <td>${AdminClient.formatMoney(order.finalAmount || order.totalAmount || 0)}₫</td>
                    <td><span class="badge ${statusInfo.class}">${statusInfo.text}</span></td>
                    <td>${createdAt}</td>
                    <td>
                        <div class="action-buttons">
                            <button class="action-btn action-btn-edit" 
                                    onclick="updateOrderStatus('${order._id || order.id}', '${order.status}')"
                                    ${canEdit ? '' : 'disabled style="opacity: 0.5; cursor: not-allowed;"'}
                                    title="${isDelivered ? 'Đơn hàng đã hoàn thành, không thể chỉnh sửa' : (isCancelled ? 'Đơn hàng đã hủy, không thể chỉnh sửa' : '')}">
                                <i class="fas fa-edit"></i> Cập nhật
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        showToast('Lỗi tải đơn hàng: ' + error.message, 'error');
        document.getElementById('ordersTableBody').innerHTML = 
            '<tr><td colspan="6" class="text-center">Lỗi tải dữ liệu</td></tr>';
    } finally {
        hideLoading();
    }
}

// Update Order Status
function updateOrderStatus(orderId, currentStatus) {
    // Không cho phép cập nhật nếu đã hoàn thành hoặc đã hủy
    if (currentStatus === 'delivered') {
        showToast('Đơn hàng đã hoàn thành, không thể chỉnh sửa', 'error');
        return;
    }
    if (currentStatus === 'cancelled') {
        showToast('Đơn hàng đã hủy, không thể chỉnh sửa', 'error');
        return;
    }
    
    document.getElementById('orderStatusId').value = orderId;
    document.getElementById('orderStatus').value = currentStatus;
    document.getElementById('orderStatusModal').classList.add('show');
}

// Save Order Status
async function saveOrderStatus() {
    const orderId = document.getElementById('orderStatusId').value;
    const status = document.getElementById('orderStatus').value;
    
    // Không cho phép cập nhật thành delivered nếu đã là delivered (bảo vệ thêm)
    if (status === 'delivered') {
        // Kiểm tra order hiện tại
        try {
            const orders = await adminClient.getAllOrders();
            const currentOrder = orders.find(o => (o._id || o.id) === orderId);
            if (currentOrder && currentOrder.status === 'delivered') {
                showToast('Đơn hàng đã hoàn thành, không thể chỉnh sửa', 'error');
                closeModal('orderStatusModal');
                return;
            }
        } catch (error) {
            // Continue if check fails
        }
    }
    
    showLoading();
    try {
        await adminClient.updateOrderStatus(orderId, status);
        showToast('Cập nhật trạng thái đơn hàng thành công!', 'success');
        closeModal('orderStatusModal');
        loadOrders();
    } catch (error) {
        showToast('Lỗi: ' + error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Load Vouchers
async function loadVouchers() {
    showLoading();
    try {
        const vouchers = await adminClient.getAllVouchers();
        const tbody = document.getElementById('vouchersTableBody');
        
        if (vouchers.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center">Không có voucher nào</td></tr>';
            return;
        }
        
        tbody.innerHTML = vouchers.map(voucher => {
            const discountText = voucher.discountType === 'percentage' 
                ? `Giảm ${voucher.discountValue}%` 
                : `Giảm ${AdminClient.formatMoney(voucher.discountValue)}₫`;
            const statusClass = voucher.isActive ? 'badge-success' : 'badge-secondary';
            const statusText = voucher.isActive ? 'Đang hoạt động' : 'Đã tắt';
            const startDate = AdminClient.formatDate(voucher.startDate);
            const endDate = AdminClient.formatDate(voucher.endDate);
            
            return `
                <tr>
                    <td><strong>${voucher.code}</strong></td>
                    <td>${voucher.name}</td>
                    <td>${discountText}</td>
                    <td><span class="badge ${statusClass}">${statusText}</span></td>
                    <td>${startDate}</td>
                    <td>${endDate}</td>
                    <td>
                        <div class="action-buttons">
                            <button class="action-btn action-btn-edit" onclick="editVoucher('${voucher._id || voucher.id}')">
                                <i class="fas fa-edit"></i> Sửa
                            </button>
                            <button class="action-btn action-btn-delete" onclick="deleteVoucher('${voucher._id || voucher.id}')">
                                <i class="fas fa-trash"></i> Xóa
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        showToast('Lỗi tải vouchers: ' + error.message, 'error');
        document.getElementById('vouchersTableBody').innerHTML = 
            '<tr><td colspan="7" class="text-center">Lỗi tải dữ liệu</td></tr>';
    } finally {
        hideLoading();
    }
}

// Open Voucher Modal
function openVoucherModal(voucherId = null) {
    const modal = document.getElementById('voucherModal');
    const form = document.getElementById('voucherForm');
    form.reset();
    
    if (voucherId) {
        document.getElementById('voucherModalTitle').textContent = 'Sửa voucher';
        document.getElementById('voucherId').value = voucherId;
        loadVoucherData(voucherId);
    } else {
        document.getElementById('voucherModalTitle').textContent = 'Thêm voucher';
        document.getElementById('voucherId').value = '';
    }
    
    modal.classList.add('show');
}

// Load Voucher Data
async function loadVoucherData(voucherId) {
    try {
        const vouchers = await adminClient.getAllVouchers();
        const voucher = vouchers.find(v => (v._id || v.id) === voucherId);
        
        if (voucher) {
            document.getElementById('voucherCode').value = voucher.code || '';
            document.getElementById('voucherCode').disabled = true; // Không cho sửa code
            document.getElementById('voucherName').value = voucher.name || '';
            document.getElementById('voucherDescription').value = voucher.description || '';
            document.getElementById('voucherDiscountType').value = voucher.discountType || 'percentage';
            document.getElementById('voucherDiscountValue').value = voucher.discountValue || 0;
            document.getElementById('voucherMinPurchase').value = voucher.minPurchaseAmount || 0;
            document.getElementById('voucherMaxDiscount').value = voucher.maxDiscountAmount || '';
            document.getElementById('voucherUsageLimit').value = voucher.usageLimit || '';
            
            const startDate = new Date(voucher.startDate).toISOString().split('T')[0];
            const endDate = new Date(voucher.endDate).toISOString().split('T')[0];
            document.getElementById('voucherStartDate').value = startDate;
            document.getElementById('voucherEndDate').value = endDate;
        }
    } catch (error) {
        showToast('Lỗi tải dữ liệu voucher: ' + error.message, 'error');
    }
}

// Edit Voucher
function editVoucher(voucherId) {
    openVoucherModal(voucherId);
}

// Save Voucher
async function saveVoucher() {
    const form = document.getElementById('voucherForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    showLoading();
    try {
        const voucherId = document.getElementById('voucherId').value;
        const voucherData = {
            code: document.getElementById('voucherCode').value.toUpperCase(),
            name: document.getElementById('voucherName').value,
            description: document.getElementById('voucherDescription').value,
            discountType: document.getElementById('voucherDiscountType').value,
            discountValue: parseFloat(document.getElementById('voucherDiscountValue').value),
            minPurchaseAmount: parseFloat(document.getElementById('voucherMinPurchase').value) || 0,
            maxDiscountAmount: document.getElementById('voucherMaxDiscount').value 
                ? parseFloat(document.getElementById('voucherMaxDiscount').value) 
                : null,
            startDate: document.getElementById('voucherStartDate').value,
            endDate: document.getElementById('voucherEndDate').value,
            usageLimit: document.getElementById('voucherUsageLimit').value 
                ? parseInt(document.getElementById('voucherUsageLimit').value) 
                : null
        };
        
        const validation = AdminClient.validateVoucher(voucherData);
        if (!validation.valid) {
            showToast(validation.errors.join(', '), 'error');
            return;
        }
        
        if (voucherId) {
            await adminClient.updateVoucher(voucherId, voucherData);
            showToast('Cập nhật voucher thành công!', 'success');
        } else {
            await adminClient.createVoucher(voucherData);
            showToast('Thêm voucher thành công!', 'success');
        }
        
        closeModal('voucherModal');
        loadVouchers();
    } catch (error) {
        showToast('Lỗi: ' + error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Delete Voucher
async function deleteVoucher(voucherId) {
    if (!confirm('Bạn có chắc muốn xóa voucher này?')) return;
    
    showLoading();
    try {
        await adminClient.deleteVoucher(voucherId);
        showToast('Xóa voucher thành công!', 'success');
        loadVouchers();
    } catch (error) {
        showToast('Lỗi: ' + error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Load Reviews
async function loadReviews() {
    showLoading();
    try {
        const reviews = await adminClient.getAdminReviews();
        const tbody = document.getElementById('reviewsTableBody');
        
        if (reviews.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">Không có đánh giá nào</td></tr>';
            return;
        }
        
        tbody.innerHTML = reviews.map(review => {
            const userName = review.user?.name || 'N/A';
            const productName = review.product?.name || 'N/A';
            const rating = '⭐'.repeat(review.rating || 0);
            const hasReply = review.adminReply ? 'Có' : 'Chưa có';
            
            return `
                <tr>
                    <td>${userName}</td>
                    <td>${productName}</td>
                    <td>${rating}</td>
                    <td>${review.comment || 'N/A'}</td>
                    <td>${hasReply}</td>
                    <td>
                        <div class="action-buttons">
                            <button class="action-btn action-btn-reply" onclick="replyReview('${review._id || review.id}', '${(review.adminReply || '').replace(/'/g, "\\'")}')">
                                <i class="fas fa-reply"></i> Phản hồi
                            </button>
                            <button class="action-btn action-btn-delete" onclick="deleteReview('${review._id || review.id}')">
                                <i class="fas fa-trash"></i> Xóa
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (error) {
        showToast('Lỗi tải đánh giá: ' + error.message, 'error');
        document.getElementById('reviewsTableBody').innerHTML = 
            '<tr><td colspan="6" class="text-center">Lỗi tải dữ liệu</td></tr>';
    } finally {
        hideLoading();
    }
}

// Reply Review
function replyReview(reviewId, currentReply = '') {
    document.getElementById('reviewReplyId').value = reviewId;
    document.getElementById('reviewReply').value = currentReply;
    document.getElementById('reviewReplyModal').classList.add('show');
}

// Save Review Reply
async function saveReviewReply() {
    const reviewId = document.getElementById('reviewReplyId').value;
    const reply = document.getElementById('reviewReply').value;
    
    if (!reply.trim()) {
        showToast('Vui lòng nhập phản hồi', 'error');
        return;
    }
    
    showLoading();
    try {
        await adminClient.replyReview(reviewId, reply);
        showToast('Phản hồi đánh giá thành công!', 'success');
        closeModal('reviewReplyModal');
        loadReviews();
    } catch (error) {
        showToast('Lỗi: ' + error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Delete Review
async function deleteReview(reviewId) {
    if (!confirm('Bạn có chắc muốn xóa đánh giá này?')) return;
    
    showLoading();
    try {
        await adminClient.deleteReview(reviewId);
        showToast('Xóa đánh giá thành công!', 'success');
        loadReviews();
    } catch (error) {
        showToast('Lỗi: ' + error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Load Revenue
async function loadRevenue() {
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    
    if (!startDate || !endDate) {
        showToast('Vui lòng chọn đầy đủ ngày bắt đầu và ngày kết thúc', 'error');
        return;
    }
    
    showLoading();
    try {
        const revenue = await adminClient.getRevenue(startDate, endDate);
        
        document.getElementById('totalRevenueAmount').textContent = 
            AdminClient.formatMoney(revenue.totalRevenue) + '₫';
        document.getElementById('totalOrdersCount').textContent = revenue.totalOrders;
        
        const tbody = document.getElementById('categoryRevenueBody');
        if (revenue.categoryRevenue && revenue.categoryRevenue.length > 0) {
            tbody.innerHTML = revenue.categoryRevenue.map(cat => `
                <tr>
                    <td>${cat.categoryName}</td>
                    <td><strong>${AdminClient.formatMoney(cat.revenue)}₫</strong></td>
                </tr>
            `).join('');
        } else {
            tbody.innerHTML = '<tr><td colspan="2" class="text-center">Không có dữ liệu</td></tr>';
        }
        
        document.getElementById('revenueStats').style.display = 'block';
        showToast('Tải thống kê thành công!', 'success');
    } catch (error) {
        showToast('Lỗi: ' + error.message, 'error');
    } finally {
        hideLoading();
    }
}

// Utility Functions
function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
}

function showLoading() {
    document.getElementById('loadingOverlay').classList.add('show');
}

function hideLoading() {
    document.getElementById('loadingOverlay').classList.remove('show');
}

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast ${type} show`;
    
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Image Preview
document.getElementById('productImage')?.addEventListener('change', function(e) {
    const file = e.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            document.getElementById('productImagePreview').src = e.target.result;
            document.getElementById('productImagePreview').style.display = 'block';
        };
        reader.readAsDataURL(file);
    }
});

// ==================== VARIANT MANAGEMENT ====================

// Add Variant
function addVariant(color = '', size = '', stock = '') {
    const variantsList = document.getElementById('variantsList');
    const variantIndex = variantsList.children.length;
    
    const variantItem = document.createElement('div');
    variantItem.className = 'variant-item';
    variantItem.dataset.index = variantIndex;
    
    // Get available colors and sizes from inputs
    const colorsInput = document.getElementById('productColors').value;
    const sizesInput = document.getElementById('productSizes').value;
    const colors = colorsInput ? colorsInput.split(',').map(c => c.trim()).filter(c => c) : [];
    const sizes = sizesInput ? sizesInput.split(',').map(s => s.trim()).filter(s => s) : [];
    
    // Get all existing colors and sizes from current variants
    const existingVariants = getVariants();
    const allColors = [...new Set([...colors, ...existingVariants.map(v => v.color).filter(c => c)])];
    const allSizes = [...new Set([...sizes, ...existingVariants.map(v => v.size).filter(s => s)])];
    
    variantItem.innerHTML = `
        <select class="variant-color" required>
            <option value="">Chọn màu</option>
            ${allColors.map(c => `<option value="${c}" ${c === color ? 'selected' : ''}>${c}</option>`).join('')}
            <option value="custom">+ Thêm màu mới</option>
        </select>
        <input type="text" class="variant-color-custom" placeholder="Nhập màu mới" style="display: none;" value="${!allColors.includes(color) && color ? color : ''}">
        <select class="variant-size" required>
            <option value="">Chọn size</option>
            ${allSizes.map(s => `<option value="${s}" ${s === size ? 'selected' : ''}>${s}</option>`).join('')}
            <option value="custom">+ Thêm size mới</option>
        </select>
        <input type="text" class="variant-size-custom" placeholder="Nhập size mới" style="display: none;" value="${!allSizes.includes(size) && size ? size : ''}">
        <input type="number" class="variant-stock" min="0" value="${stock}" placeholder="Số lượng" required>
        <button type="button" class="btn-remove-variant" onclick="removeVariant(${variantIndex})">
            <i class="fas fa-trash"></i>
        </button>
    `;
    
    variantsList.appendChild(variantItem);
    
    // Handle custom color/size
    const colorSelect = variantItem.querySelector('.variant-color');
    const colorCustom = variantItem.querySelector('.variant-color-custom');
    const sizeSelect = variantItem.querySelector('.variant-size');
    const sizeCustom = variantItem.querySelector('.variant-size-custom');
    
    colorSelect.addEventListener('change', function() {
        if (this.value === 'custom') {
            colorCustom.style.display = 'block';
            colorCustom.required = true;
        } else {
            colorCustom.style.display = 'none';
            colorCustom.required = false;
            // Add to colors list if not exists
            if (this.value && !allColors.includes(this.value)) {
                const currentColors = document.getElementById('productColors').value;
                document.getElementById('productColors').value = currentColors 
                    ? `${currentColors}, ${this.value}` 
                    : this.value;
            }
        }
        updateTotalStock();
    });
    
    sizeSelect.addEventListener('change', function() {
        if (this.value === 'custom') {
            sizeCustom.style.display = 'block';
            sizeCustom.required = true;
        } else {
            sizeCustom.style.display = 'none';
            sizeCustom.required = false;
            // Add to sizes list if not exists
            if (this.value && !allSizes.includes(this.value)) {
                const currentSizes = document.getElementById('productSizes').value;
                document.getElementById('productSizes').value = currentSizes 
                    ? `${currentSizes}, ${this.value}` 
                    : this.value;
            }
        }
        updateTotalStock();
    });
    
    // If color/size is not in list, show custom input
    if (color && !allColors.includes(color)) {
        colorSelect.value = 'custom';
        colorCustom.style.display = 'block';
        colorCustom.required = true;
    }
    if (size && !allSizes.includes(size)) {
        sizeSelect.value = 'custom';
        sizeCustom.style.display = 'block';
        sizeCustom.required = true;
    }
    
    // Update total stock when stock changes
    const stockInput = variantItem.querySelector('.variant-stock');
    stockInput.addEventListener('input', updateTotalStock);
}

// Remove Variant
function removeVariant(index) {
    const variantsList = document.getElementById('variantsList');
    const variantItem = variantsList.querySelector(`[data-index="${index}"]`);
    if (variantItem) {
        variantItem.remove();
        // Re-index remaining items
        Array.from(variantsList.children).forEach((item, idx) => {
            item.dataset.index = idx;
            const removeBtn = item.querySelector('.btn-remove-variant');
            if (removeBtn) {
                removeBtn.setAttribute('onclick', `removeVariant(${idx})`);
            }
        });
        updateTotalStock();
    }
}

// Clear Variants
function clearVariants() {
    const variantsList = document.getElementById('variantsList');
    variantsList.innerHTML = '';
}

// Get Variants
function getVariants() {
    const variantsList = document.getElementById('variantsList');
    const variants = [];
    
    Array.from(variantsList.children).forEach(item => {
        const colorSelect = item.querySelector('.variant-color');
        const colorCustom = item.querySelector('.variant-color-custom');
        const sizeSelect = item.querySelector('.variant-size');
        const sizeCustom = item.querySelector('.variant-size-custom');
        const stockInput = item.querySelector('.variant-stock');
        
        if (!colorSelect || !sizeSelect || !stockInput) return;
        
        const color = colorSelect.value === 'custom' 
            ? (colorCustom ? colorCustom.value.trim() : '') 
            : colorSelect.value.trim();
        const size = sizeSelect.value === 'custom' 
            ? (sizeCustom ? sizeCustom.value.trim() : '') 
            : sizeSelect.value.trim();
        const stock = parseInt(stockInput.value) || 0;
        
        if (color && size && stock >= 0) {
            // Check if variant already exists (same color + size)
            const existing = variants.find(v => v.color === color && v.size === size);
            if (existing) {
                // Merge stock if duplicate
                existing.stock += stock;
            } else {
                variants.push({ color, size, stock, sold: 0 });
            }
        }
    });
    
    return variants;
}

// Generate Variants from Colors and Sizes
function generateVariantsFromColorsSizes() {
    const colorsInput = document.getElementById('productColors').value;
    const sizesInput = document.getElementById('productSizes').value;
    
    if (!colorsInput || !sizesInput) return;
    
    const colors = colorsInput.split(',').map(c => c.trim()).filter(c => c);
    const sizes = sizesInput.split(',').map(s => s.trim()).filter(s => s);
    
    if (colors.length === 0 || sizes.length === 0) return;
    
    // Ask user if they want to auto-generate variants
    const existingVariants = getVariants();
    if (existingVariants.length > 0) {
        if (!confirm('Bạn có muốn tự động tạo biến thể từ danh sách màu và size? (Sẽ xóa các biến thể hiện tại)')) {
            return;
        }
    }
    
    // Clear and generate
    clearVariants();
    colors.forEach(color => {
        sizes.forEach(size => {
            // Find existing stock if editing
            const existing = existingVariants.find(v => v.color === color && v.size === size);
            addVariant(color, size, existing ? existing.stock : '');
        });
    });
    
    // Update total stock
    updateTotalStock();
}

// Update Total Stock from Variants
function updateTotalStock() {
    const variants = getVariants();
    const totalStock = variants.reduce((sum, v) => sum + (parseInt(v.stock) || 0), 0);
    const stockInput = document.getElementById('productStock');
    if (stockInput) {
        stockInput.value = totalStock;
    }
}

// Refresh Variant Dropdowns (when colors/sizes change)
function refreshVariantDropdowns() {
    const variantsList = document.getElementById('variantsList');
    const colorsInput = document.getElementById('productColors').value;
    const sizesInput = document.getElementById('productSizes').value;
    const colors = colorsInput ? colorsInput.split(',').map(c => c.trim()).filter(c => c) : [];
    const sizes = sizesInput ? sizesInput.split(',').map(s => s.trim()).filter(s => s) : [];
    
    // Get all existing colors and sizes from current variants
    const existingVariants = getVariants();
    const allColors = [...new Set([...colors, ...existingVariants.map(v => v.color).filter(c => c)])];
    const allSizes = [...new Set([...sizes, ...existingVariants.map(v => v.size).filter(s => s)])];
    
    // Update each variant's dropdown
    Array.from(variantsList.children).forEach(item => {
        const colorSelect = item.querySelector('.variant-color');
        const sizeSelect = item.querySelector('.variant-size');
        const currentColor = colorSelect.value === 'custom' 
            ? item.querySelector('.variant-color-custom').value 
            : colorSelect.value;
        const currentSize = sizeSelect.value === 'custom' 
            ? item.querySelector('.variant-size-custom').value 
            : sizeSelect.value;
        
        // Rebuild color dropdown
        colorSelect.innerHTML = `
            <option value="">Chọn màu</option>
            ${allColors.map(c => `<option value="${c}" ${c === currentColor ? 'selected' : ''}>${c}</option>`).join('')}
            <option value="custom" ${!allColors.includes(currentColor) && currentColor ? 'selected' : ''}>+ Thêm màu mới</option>
        `;
        
        // Rebuild size dropdown
        sizeSelect.innerHTML = `
            <option value="">Chọn size</option>
            ${allSizes.map(s => `<option value="${s}" ${s === currentSize ? 'selected' : ''}>${s}</option>`).join('')}
            <option value="custom" ${!allSizes.includes(currentSize) && currentSize ? 'selected' : ''}>+ Thêm size mới</option>
        `;
        
        // Show custom inputs if needed
        if (currentColor && !allColors.includes(currentColor)) {
            colorSelect.value = 'custom';
            item.querySelector('.variant-color-custom').style.display = 'block';
            item.querySelector('.variant-color-custom').value = currentColor;
        }
        if (currentSize && !allSizes.includes(currentSize)) {
            sizeSelect.value = 'custom';
            item.querySelector('.variant-size-custom').style.display = 'block';
            item.querySelector('.variant-size-custom').value = currentSize;
        }
    });
}

