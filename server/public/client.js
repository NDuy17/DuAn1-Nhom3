/**
 * Admin Client - Quản lý tất cả chức năng admin
 * File này chứa tất cả các hàm để quản lý admin từ phía client (JavaScript)
 */

// Get base URL from current location or use default
const BASE_URL = (typeof window !== 'undefined' && window.location.origin) 
  ? window.location.origin 
  : (typeof process !== 'undefined' && process.env && process.env.API_URL) 
    ? process.env.API_URL 
    : 'http://localhost:3000';

class AdminClient {
  constructor(token) {
    this.token = token;
    this.baseURL = BASE_URL;
  }

  /**
   * Tạo headers với token
   */
  getHeaders() {
    return {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${this.token}`
    };
  }

  /**
   * Xử lý response
   */
  async handleResponse(response) {
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || data.error || 'Lỗi không xác định');
    }
    return data;
  }


}

