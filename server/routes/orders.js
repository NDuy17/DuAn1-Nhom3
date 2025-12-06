const express = require('express');
const Order = require('../models/Order');
const Cart = require('../models/Cart');
const Product = require('../models/Product');
const Voucher = require('../models/Voucher');
const Category = require('../models/Category');
const { verifyToken, requireAdmin } = require('../middleware/auth');
const router = express.Router();

/**
 * Tạo đơn hàng mới (thanh toán)
 * Headers: Authorization: Bearer <token>
 * Body: { phone, address, note?, voucherId? }
 */
router.post('/', verifyToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { receiverName, phone, address, note, voucherId, items } = req.body;

    console.log('Order request:', { receiverName, phone, address, note, voucherId, itemsCount: items ? items.length : 0 });

    if (!receiverName || !phone || !address) {
      return res.status(400).json({ message: 'Vui lòng nhập đầy đủ thông tin: tên người nhận, số điện thoại và địa chỉ' });
    }

    let orderItems = [];
    let totalAmount = 0;

    // Luôn sử dụng items từ request body (không phụ thuộc vào giỏ hàng)
    if (!items || !Array.isArray(items) || items.length === 0) {
      console.log('Error: No items in request');
      return res.status(400).json({ message: 'Vui lòng gửi danh sách sản phẩm' });
    }

    for (const item of items) {
      console.log('Processing item:', item);
      if (!item.productId || item.quantity === undefined || item.quantity === null || !item.price) {
        console.log('Error: Invalid item data:', item);
        return res.status(400).json({ message: 'Thông tin sản phẩm không hợp lệ: thiếu productId, quantity hoặc price' });
      }
      
      // Kiểm tra sản phẩm có tồn tại không
      const product = await Product.findById(item.productId);
      if (!product) {
        console.log('Error: Product not found:', item.productId);
        return res.status(400).json({ message: `Sản phẩm không tồn tại: ${item.productId}` });
      }

      const itemTotal = item.price * item.quantity;
      totalAmount += itemTotal;
      orderItems.push({
        product: item.productId,
        quantity: item.quantity,
        price: item.price,
        color: item.color || 'Mặc định',
        size: item.size || 'Free size'
      });
    }

    // Tính giảm giá nếu có voucher
    let discountAmount = 0;
    if (voucherId) {
      const voucher = await Voucher.findById(voucherId);
      if (voucher && voucher.isActive) {
        const now = new Date();
        if (voucher.startDate <= now && voucher.endDate >= now) {
          if (voucher.user === null || voucher.user.toString() === userId) {
            if (totalAmount >= voucher.minPurchaseAmount) {
              if (voucher.discountType === 'percentage') {
                discountAmount = totalAmount * voucher.discountValue / 100;
                if (voucher.maxDiscountAmount && discountAmount > voucher.maxDiscountAmount) {
                  discountAmount = voucher.maxDiscountAmount;
                }
              } else {
                discountAmount = voucher.discountValue;
                if (discountAmount > totalAmount) {
                  discountAmount = totalAmount;
                }
              }
            }
          }
        }
      }
    }

    const finalAmount = totalAmount - discountAmount;

    console.log('Creating order with data:', {
      user: userId,
      itemsCount: orderItems.length,
      totalAmount,
      discountAmount,
      finalAmount,
      phone,
      address
    });

    // Tạo đơn hàng và lưu vào MongoDB
    const order = await Order.create({
      user: userId,
      items: orderItems,
      totalAmount,
      discountAmount,
      finalAmount,
      receiverName,
      phone,
      shippingAddress: address,
      note: note || '',
      voucher: voucherId || null,
      paymentStatus: 'paid', // Mặc định là đã thanh toán
      status: 'pending'
    });

   // --- DÁN ĐOẠN NÀY VÀO ---
    // Cập nhật stock và sold (Hỗ trợ cả sản phẩm thường và biến thể)
    for (const item of orderItems) {
      try {
        const quantity = Math.abs(item.quantity); // Đảm bảo số lượng luôn dương để cộng trừ đúng

        // TH1: Nếu item có màu và size (Sản phẩm có biến thể)
        // Tìm đúng sản phẩm có chứa biến thể màu/size đó để trừ kho
        const updatedVariant = await Product.findOneAndUpdate(
          {
            _id: item.product,
            "variants.color": item.color, 
            "variants.size": item.size    
          },
          {
            $inc: {
              "variants.$.stock": -quantity, // Trừ kho của biến thể
              "variants.$.sold": quantity,   // Tăng đã bán của biến thể
              "stock": -quantity,            // Trừ kho tổng (ở ngoài)
              "sold": quantity               // Tăng đã bán tổng (ở ngoài)
            }
          },
          { new: true }
        );

        // TH2: Nếu không update được biến thể (do sp không có biến thể hoặc sai màu/size)
        // Thì chỉ update Stock/Sold ở lớp ngoài cùng
        if (!updatedVariant) {
           await Product.findByIdAndUpdate(item.product, {
              $inc: {
                stock: -quantity,
                sold: quantity
              }
           });
           console.log(`✅ Đã cập nhật sản phẩm thường (không biến thể): ${item.product}`);
        } else {
           console.log(`✅ Đã cập nhật biến thể ${item.color}/${item.size} của sp ${item.product}`);
        }

      } catch (updateError) {
        console.error(`⚠️ Lỗi cập nhật kho hàng cho sp ${item.product}:`, updateError.message);
      }
    }

    try {
      await Cart.findOneAndDelete({ user: userId });
      console.log('✅ Cart đã được xóa sau khi tạo order');
    } catch (cartError) {
      console.log('⚠️ Lỗi khi xóa cart (không ảnh hưởng đến order):', cartError.message);
    }

    // Populate order để trả về đầy đủ thông tin (không populate category để tránh lỗi parse)
    const populatedOrder = await Order.findById(order._id)
      .populate({
        path: 'items.product',
        select: 'name price image stock colors sizes description', // Chỉ lấy các field cần thiết, không lấy category
        populate: false
      })
      .populate('user', 'name email')
      .populate('voucher');

    console.log('✅ Order populated successfully, sending response');

    // Format response để đảm bảo user luôn là object (giống như admin/all route)
    const orderObj = populatedOrder.toObject({ getters: true, virtuals: false });
    
    // Đảm bảo user luôn là object với _id, name, email
    if (orderObj.user) {
      if (typeof orderObj.user === 'object' && orderObj.user._id) {
        // Đã populated
        orderObj.user._id = orderObj.user._id.toString();
        orderObj.user.name = orderObj.user.name || null;
        orderObj.user.email = orderObj.user.email || null;
      } else {
        // ObjectId hoặc string
        const userId = orderObj.user.toString ? orderObj.user.toString() : String(orderObj.user);
        orderObj.user = {
          _id: userId,
          name: null,
          email: null
        };
      }
    }
    
    // Convert dates
    if (orderObj.createdAt) {
      orderObj.createdAt = (orderObj.createdAt instanceof Date 
        ? orderObj.createdAt 
        : new Date(orderObj.createdAt)).toISOString();
    }
    if (orderObj.updatedAt) {
      orderObj.updatedAt = (orderObj.updatedAt instanceof Date 
        ? orderObj.updatedAt 
        : new Date(orderObj.updatedAt)).toISOString();
    }
    
    // Format items - convert product _id
    if (orderObj.items && Array.isArray(orderObj.items)) {
      orderObj.items = orderObj.items.map(item => {
        const itemObj = item.toObject ? item.toObject() : item;
        if (itemObj.product && typeof itemObj.product === 'object' && itemObj.product._id) {
          itemObj.product._id = itemObj.product._id.toString();
        }
        return itemObj;
      });
    }

    res.status(201).json({
      message: 'Đặt hàng thành công',
      order: orderObj
    });
  } catch (err) {
    console.error('❌ Error creating order:', err);
    console.error('Error stack:', err.stack);
    res.status(500).json({ 
      message: 'Lỗi server', 
      error: err.message,
      details: process.env.NODE_ENV === 'development' ? err.stack : undefined
    });
  }
});

/**
 * Lấy lịch sử đơn hàng của user
 * Headers: Authorization: Bearer <token>
 * Query: ?status=paid|unpaid (optional)
 */
router.get('/', verifyToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const { status } = req.query;

    let query = { user: userId };
    if (status === 'paid' || status === 'unpaid') {
      query.paymentStatus = status;
    }

    const orders = await Order.find(query)
      .populate({
        path: 'items.product',
        select: 'name price image stock colors sizes description', // Chỉ lấy các field cần thiết, không lấy category
        populate: false
      })
      .populate('user', 'name email')
      .populate('voucher')
      .sort({ createdAt: -1 });

    // Format response để đảm bảo user luôn là object (giống như admin/all route)
    const formattedOrders = orders.map(order => {
      const orderObj = order.toObject({ getters: true, virtuals: false });
      
      // Convert _id
      orderObj._id = orderObj._id.toString();
      
      // Đảm bảo user luôn là object với _id, name, email
      if (orderObj.user) {
        if (typeof orderObj.user === 'object' && orderObj.user._id) {
          // Đã populated
          orderObj.user._id = orderObj.user._id.toString();
          orderObj.user.name = orderObj.user.name || null;
          orderObj.user.email = orderObj.user.email || null;
        } else {
          // ObjectId hoặc string
          const userId = orderObj.user.toString ? orderObj.user.toString() : String(orderObj.user);
          orderObj.user = {
            _id: userId,
            name: null,
            email: null
          };
        }
      }
      
      // Convert dates
      if (orderObj.createdAt) {
        orderObj.createdAt = (orderObj.createdAt instanceof Date 
          ? orderObj.createdAt 
          : new Date(orderObj.createdAt)).toISOString();
      }
      if (orderObj.updatedAt) {
        orderObj.updatedAt = (orderObj.updatedAt instanceof Date 
          ? orderObj.updatedAt 
          : new Date(orderObj.updatedAt)).toISOString();
      }
      
      // Format items - convert product _id
      if (orderObj.items && Array.isArray(orderObj.items)) {
        orderObj.items = orderObj.items.map(item => {
          const itemObj = item.toObject ? item.toObject() : item;
          if (itemObj.product && typeof itemObj.product === 'object' && itemObj.product._id) {
            itemObj.product._id = itemObj.product._id.toString();
          }
          return itemObj;
        });
      }
      
      return orderObj;
    });

    res.json(formattedOrders);
  } catch (err) {
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
});

// ================== API THỐNG KÊ DOANH THU - PHẢI ĐẶT TRƯỚC /:id ==================
/**
 * GET /api/orders/revenue?startDate=2025-1-4&endDate=2025-1-31
 * KHÔNG CẦN TOKEN - PUBLIC API
 * Chỉ tính các đơn đã thanh toán và chưa bị hủy
 * Định dạng ngày: YYYY-M-D (ví dụ: 2025-1-4)
 * Trả về doanh thu theo từng category
 */
router.get('/revenue', async (req, res) => {
    console.log('📊 Revenue API called:', { startDate: req.query.startDate, endDate: req.query.endDate });
    
    try {
        const { startDate, endDate } = req.query;

        // Chỉ lấy đơn đã thanh toán và chưa bị hủy
        let query = { 
            paymentStatus: 'paid',
            status: { $ne: 'cancelled' }
        };

        if (startDate && endDate) {
            try {
                // Parse ngày từ format YYYY-M-D
                const parseDate = (dateStr) => {
                    const parts = dateStr.split('-');
                    if (parts.length === 3) {
                        const year = parseInt(parts[0], 10);
                        const month = parseInt(parts[1], 10) - 1; // Month is 0-indexed
                        const day = parseInt(parts[2], 10);
                        const date = new Date(year, month, day);
                        if (isNaN(date.getTime())) {
                            throw new Error('Invalid date');
                        }
                        return date;
                    }
                    throw new Error('Invalid date format');
                };

                // Tạo ngày BẮT ĐẦU từ 00:00:00 của startDate
                const startOfDay = parseDate(startDate);
                startOfDay.setHours(0, 0, 0, 0);

                // Tạo ngày KẾT THÚC từ 00:00:00 của ngày TIẾP THEO sau endDate
                const end = parseDate(endDate);
                const nextDay = new Date(end);
                nextDay.setDate(end.getDate() + 1);
                nextDay.setHours(0, 0, 0, 0);

                query.createdAt = {
                    $gte: startOfDay,
                    $lt: nextDay
                };

                console.log(`📅 Filtering orders from ${startOfDay.toISOString()} to before ${nextDay.toISOString()}`);
            } catch (dateError) {
                console.error('❌ Lỗi parse ngày:', dateError);
                return res.status(400).json({ 
                    message: "Định dạng ngày không hợp lệ. Vui lòng sử dụng format: YYYY-M-D (ví dụ: 2025-1-4)",
                    error: dateError.message 
                });
            }
        } else {
            console.log('📊 Fetching all paid and non-cancelled orders (no date filter)');
        }

        // Lấy tất cả categories trong hệ thống
        const allCategories = await Category.find().select('_id name');
        console.log(`📋 Found ${allCategories.length} categories in system`);

        // Lấy orders và populate items.product để có category
        const orders = await Order.find(query)
            .populate({
                path: 'items.product',
                select: 'name category',
                populate: {
                    path: 'category',
                    select: 'name _id'
                }
            });
        
        console.log(`📦 Found ${orders.length} orders matching criteria`);

        // Khởi tạo Map với tất cả categories (doanh thu = 0)
        const categoryRevenueMap = new Map();
        allCategories.forEach(cat => {
            categoryRevenueMap.set(cat._id.toString(), {
                categoryId: cat._id.toString(),
                categoryName: cat.name,
                revenue: 0
            });
        });

        // Tính doanh thu theo từng category từ orders
        // Sử dụng finalAmount (sau giảm giá) thay vì totalAmount
        let itemsWithoutCategory = 0;

        orders.forEach(order => {
            if (order.paymentStatus === 'paid' && order.status !== 'cancelled') {
                // Tính tổng giá trị gốc của order (trước giảm giá)
                const orderTotalAmount = order.totalAmount || 0;
                // Lấy giá trị thực tế sau giảm giá
                const orderFinalAmount = order.finalAmount || 0;
                
                // Tính tỷ lệ giảm giá (nếu có)
                // Nếu totalAmount = 0 thì không có giảm giá
                const discountRatio = orderTotalAmount > 0 ? (orderFinalAmount / orderTotalAmount) : 1;
                
                // Tính doanh thu của từng item trong order (sau giảm giá)
                order.items.forEach(item => {
                    if (item.product) {
                        // Kiểm tra nếu product có category
                        let categoryId = null;
                        
                        if (item.product.category) {
                            // Nếu category là object (đã populate)
                            if (typeof item.product.category === 'object' && item.product.category._id) {
                                categoryId = item.product.category._id.toString();
                            } 
                            // Nếu category là ObjectId (chưa populate)
                            else if (item.product.category.toString) {
                                categoryId = item.product.category.toString();
                            }
                        } else {
                            itemsWithoutCategory++;
                        }
                        
                        // Chỉ tính doanh thu nếu category tồn tại trong hệ thống
                        if (categoryId && categoryRevenueMap.has(categoryId)) {
                            // Tính giá trị gốc của item
                            const itemOriginalValue = (item.price || 0) * (item.quantity || 0);
                            // Áp dụng tỷ lệ giảm giá để có giá trị thực tế (sau giảm giá)
                            const itemRevenue = itemOriginalValue * discountRatio;
                            
                            const existing = categoryRevenueMap.get(categoryId);
                            existing.revenue += itemRevenue;
                        }
                    }
                });
            }
        });

        if (itemsWithoutCategory > 0) {
            console.log(`⚠️ Warning: ${itemsWithoutCategory} items without category`);
        }

        // Chuyển Map thành Array và sắp xếp theo revenue giảm dần
        const categoryRevenue = Array.from(categoryRevenueMap.values())
            .sort((a, b) => b.revenue - a.revenue);

        // Tính tổng doanh thu
        const totalRevenue = categoryRevenue.reduce((sum, cat) => sum + cat.revenue, 0);
        const totalOrders = orders.length;

        console.log(`✅ Revenue stats: ${totalOrders} orders, ${totalRevenue} total revenue, ${categoryRevenue.length} categories`);
        
        // Log chi tiết category revenue để debug
        if (categoryRevenue.length > 0) {
            console.log('📊 Category Revenue Details:');
            categoryRevenue.forEach(cat => {
                console.log(`  - ${cat.categoryName} (${cat.categoryId}): ${cat.revenue}`);
            });
        } else {
            console.log('⚠️ No category revenue found!');
        }

        res.status(200).json({
            totalOrders: totalOrders,
            totalRevenue: totalRevenue,
            categoryRevenue: categoryRevenue
        });

    } catch (err) {
        console.error('❌ Lỗi thống kê doanh thu:', err);
        res.status(500).json({ 
            message: "Lỗi server", 
            error: err.message 
        });
    }
});

/**
 * Lấy chi tiết đơn hàng
 * Headers: Authorization: Bearer <token>
 */
router.get('/:id', verifyToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const orderId = req.params.id;

    const order = await Order.findOne({ _id: orderId, user: userId })
      .populate({
        path: 'items.product',
        select: 'name price image stock colors sizes description',
        populate: false
      })
      .populate('user', 'name email')
      .populate('voucher');

    if (!order) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
    }

    // Format response để đảm bảo user luôn là object
    const orderObj = order.toObject({ getters: true, virtuals: false });
    
    // Đảm bảo user luôn là object với _id, name, email
    if (orderObj.user) {
      if (typeof orderObj.user === 'object' && orderObj.user._id) {
        // Đã populated
        orderObj.user._id = orderObj.user._id.toString();
        orderObj.user.name = orderObj.user.name || null;
        orderObj.user.email = orderObj.user.email || null;
      } else {
        // ObjectId hoặc string
        const userId = orderObj.user.toString ? orderObj.user.toString() : String(orderObj.user);
        orderObj.user = {
          _id: userId,
          name: null,
          email: null
        };
      }
    }
    
    // Convert dates
    if (orderObj.createdAt) {
      orderObj.createdAt = (orderObj.createdAt instanceof Date 
        ? orderObj.createdAt 
        : new Date(orderObj.createdAt)).toISOString();
    }
    if (orderObj.updatedAt) {
      orderObj.updatedAt = (orderObj.updatedAt instanceof Date 
        ? orderObj.updatedAt 
        : new Date(orderObj.updatedAt)).toISOString();
    }
    
    // Format items - convert product _id
    if (orderObj.items && Array.isArray(orderObj.items)) {
      orderObj.items = orderObj.items.map(item => {
        const itemObj = item.toObject ? item.toObject() : item;
        if (itemObj.product && typeof itemObj.product === 'object' && itemObj.product._id) {
          itemObj.product._id = itemObj.product._id.toString();
        }
        return itemObj;
      });
    }

    res.json(orderObj);
  } catch (err) {
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
});

/**
 * Admin lấy tất cả đơn hàng
 * Headers: Authorization: Bearer <token>
 * Query: ?status=pending|processing|shipped|delivered|cancelled (optional)
 */
router.get('/admin/all', verifyToken, requireAdmin, async (req, res) => {
  try {
    console.log('📦 GET /api/orders/admin/all');
    console.log('📦 User ID:', req.user.id, ', Role:', req.user.role);
    const { status } = req.query;
    let query = {};
    
    if (status && ['pending', 'processing', 'shipped', 'delivered', 'cancelled'].includes(status)) {
      query.status = status;
      console.log('📦 Filtering by status:', status);
    } else {
      console.log('📦 No status filter - getting all orders');
    }

    const orders = await Order.find(query)
      .populate({
        path: 'items.product',
        select: 'name price image',
        populate: false
      })
      .populate('user', 'name email')
      .sort({ createdAt: -1 });

    console.log('📦 Found', orders.length, 'orders in database');
    
    // Format response - đơn giản và nhất quán cho Android
    const formattedOrders = orders.map(order => {
      const orderObj = order.toObject({ getters: true, virtuals: false });
      
      // Convert _id
      orderObj._id = orderObj._id.toString();
      
      // Đảm bảo user luôn là object với _id, name, email
      if (orderObj.user) {
        if (typeof orderObj.user === 'object' && orderObj.user._id) {
          // Đã populated
          orderObj.user._id = orderObj.user._id.toString();
          orderObj.user.name = orderObj.user.name || null;
          orderObj.user.email = orderObj.user.email || null;
        } else {
          // ObjectId hoặc string
          const userId = orderObj.user.toString ? orderObj.user.toString() : String(orderObj.user);
          orderObj.user = {
            _id: userId,
            name: null,
            email: null
          };
        }
      }
      
      // Convert dates
      if (orderObj.createdAt) {
        orderObj.createdAt = (orderObj.createdAt instanceof Date 
          ? orderObj.createdAt 
          : new Date(orderObj.createdAt)).toISOString();
      }
      if (orderObj.updatedAt) {
        orderObj.updatedAt = (orderObj.updatedAt instanceof Date 
          ? orderObj.updatedAt 
          : new Date(orderObj.updatedAt)).toISOString();
      }
      
      // Format items - convert product _id
      if (orderObj.items && Array.isArray(orderObj.items)) {
        orderObj.items = orderObj.items.map(item => {
          const itemObj = item.toObject ? item.toObject() : item;
          if (itemObj.product && typeof itemObj.product === 'object' && itemObj.product._id) {
            itemObj.product._id = itemObj.product._id.toString();
          }
          return itemObj;
        });
      }
      
      return orderObj;
    });

    console.log('📦 Returning', formattedOrders.length, 'formatted orders');
    if (formattedOrders.length > 0) {
      console.log('📦 Sample order (first order):');
      console.log(JSON.stringify(formattedOrders[0], null, 2));
    }
    res.json(formattedOrders);
  } catch (err) {
    console.error('❌ Error getting all orders:', err);
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
});

/**
 * Admin cập nhật trạng thái đơn hàng
 * Headers: Authorization: Bearer <token>
 * Body: { status: 'pending'|'processing'|'shipped'|'delivered'|'cancelled' }
 */
router.put('/:id/status', verifyToken, requireAdmin, async (req, res) => {
  try {
    const orderId = req.params.id;
    const { status } = req.body;

    if (!status || !['pending', 'processing', 'shipped', 'delivered', 'cancelled'].includes(status)) {
      return res.status(400).json({ message: 'Trạng thái không hợp lệ' });
    }

    const order = await Order.findById(orderId);
    if (!order) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
    }

    // Nếu hủy đơn, cần hoàn trả stock
    if (status === 'cancelled' && order.status !== 'cancelled') {
      for (const item of order.items) {
        try {
          const quantity = Math.abs(item.quantity);
          const updatedVariant = await Product.findOneAndUpdate(
            {
              _id: item.product,
              "variants.color": item.color,
              "variants.size": item.size
            },
            {
              $inc: {
                "variants.$.stock": quantity,
                "variants.$.sold": -quantity,
                "stock": quantity,
                "sold": -quantity
              }
            },
            { new: true }
          );

          if (!updatedVariant) {
            await Product.findByIdAndUpdate(item.product, {
              $inc: {
                stock: quantity,
                sold: -quantity
              }
            });
          }
        } catch (updateError) {
          console.error(`⚠️ Lỗi cập nhật kho hàng khi hủy đơn:`, updateError.message);
        }
      }
    }

    order.status = status;
    order.updatedAt = new Date();
    await order.save();

    // Populate và format response
    const populatedOrder = await Order.findById(order._id)
      .populate({
        path: 'items.product',
        select: 'name price image stock colors sizes description',
        populate: false
      })
      .populate('user', 'name email')
      .populate('voucher');

    const orderObj = populatedOrder.toObject({ getters: true, virtuals: false });
    
    // Format user
    if (orderObj.user) {
      if (typeof orderObj.user === 'object' && orderObj.user._id) {
        orderObj.user._id = orderObj.user._id.toString();
        orderObj.user.name = orderObj.user.name || null;
        orderObj.user.email = orderObj.user.email || null;
      } else {
        const userId = orderObj.user.toString ? orderObj.user.toString() : String(orderObj.user);
        orderObj.user = {
          _id: userId,
          name: null,
          email: null
        };
      }
    }
    
    // Convert dates
    if (orderObj.createdAt) {
      orderObj.createdAt = (orderObj.createdAt instanceof Date 
        ? orderObj.createdAt 
        : new Date(orderObj.createdAt)).toISOString();
    }
    if (orderObj.updatedAt) {
      orderObj.updatedAt = (orderObj.updatedAt instanceof Date 
        ? orderObj.updatedAt 
        : new Date(orderObj.updatedAt)).toISOString();
    }
    
    // Format items
    if (orderObj.items && Array.isArray(orderObj.items)) {
      orderObj.items = orderObj.items.map(item => {
        const itemObj = item.toObject ? item.toObject() : item;
        if (itemObj.product && typeof itemObj.product === 'object' && itemObj.product._id) {
          itemObj.product._id = itemObj.product._id.toString();
        }
        return itemObj;
      });
    }

    res.json(orderObj);
  } catch (err) {
    console.error('❌ Error updating order status:', err);
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
});

/**
 * Admin cập nhật toàn bộ thông tin đơn hàng
 * Headers: Authorization: Bearer <token>
 * Body: { receiverName?, phone?, shippingAddress?, note?, status?, paymentStatus? }
 */
router.put('/:id', verifyToken, requireAdmin, async (req, res) => {
  try {
    const orderId = req.params.id;
    const { receiverName, phone, shippingAddress, note, status, paymentStatus } = req.body;

    const order = await Order.findById(orderId);
    if (!order) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
    }

    // Validate status nếu có
    if (status && !['pending', 'processing', 'shipped', 'delivered', 'cancelled'].includes(status)) {
      return res.status(400).json({ message: 'Trạng thái không hợp lệ' });
    }

    // Validate paymentStatus nếu có
    if (paymentStatus && !['paid', 'unpaid'].includes(paymentStatus)) {
      return res.status(400).json({ message: 'Trạng thái thanh toán không hợp lệ' });
    }

    const oldStatus = order.status;

    // Cập nhật các field nếu có trong request
    if (receiverName !== undefined) order.receiverName = receiverName;
    if (phone !== undefined) order.phone = phone;
    if (shippingAddress !== undefined) order.shippingAddress = shippingAddress;
    if (note !== undefined) order.note = note;
    if (status !== undefined) order.status = status;
    if (paymentStatus !== undefined) order.paymentStatus = paymentStatus;

    // Nếu hủy đơn, cần hoàn trả stock
    if (status === 'cancelled' && oldStatus !== 'cancelled') {
      for (const item of order.items) {
        try {
          const quantity = Math.abs(item.quantity);
          const updatedVariant = await Product.findOneAndUpdate(
            {
              _id: item.product,
              "variants.color": item.color,
              "variants.size": item.size
            },
            {
              $inc: {
                "variants.$.stock": quantity,
                "variants.$.sold": -quantity,
                "stock": quantity,
                "sold": -quantity
              }
            },
            { new: true }
          );

          if (!updatedVariant) {
            await Product.findByIdAndUpdate(item.product, {
              $inc: {
                stock: quantity,
                sold: -quantity
              }
            });
          }
        } catch (updateError) {
          console.error(`⚠️ Lỗi cập nhật kho hàng khi hủy đơn:`, updateError.message);
        }
      }
    }

    // Nếu đơn từ cancelled chuyển sang trạng thái khác, trừ lại stock
    if (oldStatus === 'cancelled' && status && status !== 'cancelled') {
      for (const item of order.items) {
        try {
          const quantity = Math.abs(item.quantity);
          const updatedVariant = await Product.findOneAndUpdate(
            {
              _id: item.product,
              "variants.color": item.color,
              "variants.size": item.size
            },
            {
              $inc: {
                "variants.$.stock": -quantity,
                "variants.$.sold": quantity,
                "stock": -quantity,
                "sold": quantity
              }
            },
            { new: true }
          );

          if (!updatedVariant) {
            await Product.findByIdAndUpdate(item.product, {
              $inc: {
                stock: -quantity,
                sold: quantity
              }
            });
          }
        } catch (updateError) {
          console.error(`⚠️ Lỗi cập nhật kho hàng khi khôi phục đơn:`, updateError.message);
        }
      }
    }

    order.updatedAt = new Date();
    await order.save();

    // Populate và format response
    const populatedOrder = await Order.findById(order._id)
      .populate({
        path: 'items.product',
        select: 'name price image stock colors sizes description',
        populate: false
      })
      .populate('user', 'name email')
      .populate('voucher');

    const orderObj = populatedOrder.toObject({ getters: true, virtuals: false });
    
    // Format user
    if (orderObj.user) {
      if (typeof orderObj.user === 'object' && orderObj.user._id) {
        orderObj.user._id = orderObj.user._id.toString();
        orderObj.user.name = orderObj.user.name || null;
        orderObj.user.email = orderObj.user.email || null;
      } else {
        const userId = orderObj.user.toString ? orderObj.user.toString() : String(orderObj.user);
        orderObj.user = {
          _id: userId,
          name: null,
          email: null
        };
      }
    }
    
    // Convert dates
    if (orderObj.createdAt) {
      orderObj.createdAt = (orderObj.createdAt instanceof Date 
        ? orderObj.createdAt 
        : new Date(orderObj.createdAt)).toISOString();
    }
    if (orderObj.updatedAt) {
      orderObj.updatedAt = (orderObj.updatedAt instanceof Date 
        ? orderObj.updatedAt 
        : new Date(orderObj.updatedAt)).toISOString();
    }
    
    // Format items
    if (orderObj.items && Array.isArray(orderObj.items)) {
      orderObj.items = orderObj.items.map(item => {
        const itemObj = item.toObject ? item.toObject() : item;
        if (itemObj.product && typeof itemObj.product === 'object' && itemObj.product._id) {
          itemObj.product._id = itemObj.product._id.toString();
        }
        return itemObj;
      });
    }

    res.json(orderObj);
  } catch (err) {
    console.error('❌ Error updating order:', err);
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
});

/**
 * Hủy đơn hàng
 * Headers: Authorization: Bearer <token>
 */
router.put('/:id/cancel', verifyToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const orderId = req.params.id;

    const order = await Order.findOne({ _id: orderId, user: userId });

    if (!order) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
    }

    // Chỉ cho phép hủy đơn hàng nếu đang ở trạng thái chờ xác nhận
    if (order.status !== 'pending') {
      return res.status(400).json({ message: 'Chỉ có thể hủy đơn hàng đang chờ xác nhận' });
    }

    // Cập nhật stock và sold khi hủy đơn
    for (const item of order.items) {
      try {
        const quantity = Math.abs(item.quantity);

        // Tìm và cập nhật biến thể nếu có
        const updatedVariant = await Product.findOneAndUpdate(
          {
            _id: item.product,
            "variants.color": item.color,
            "variants.size": item.size
          },
          {
            $inc: {
              "variants.$.stock": quantity,
              "variants.$.sold": -quantity,
              "stock": quantity,
              "sold": -quantity
            }
          },
          { new: true }
        );

        // Nếu không có biến thể, chỉ cập nhật stock/sold ở ngoài
        if (!updatedVariant) {
          await Product.findByIdAndUpdate(item.product, {
            $inc: {
              stock: quantity,
              sold: -quantity
            }
          });
        }
      } catch (updateError) {
        console.error(`⚠️ Lỗi cập nhật kho hàng khi hủy đơn cho sp ${item.product}:`, updateError.message);
      }
    }

    // Cập nhật trạng thái đơn hàng thành cancelled
    order.status = 'cancelled';
    order.updatedAt = new Date();
    await order.save();

    res.json({ message: 'Đã hủy đơn hàng thành công', order });
  } catch (err) {
    console.error('❌ Error cancelling order:', err);
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
});

module.exports = router;
