// Script to fix MongoDB indexes for products collection
// Run this with: node scripts/fix-indexes.js

require('dotenv').config();
const mongoose = require('mongoose');

const connectDB = async () => {
  try {
    const conn = await mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/duan1');
    console.log(`Kết nối thành công`);
    console.log(`Database: ${conn.connection.name}`);
    return conn;
  } catch (error) {
    console.error(`Kết nối thất bại`);
    console.error(`Error: ${error.message}`);
    process.exit(1);
  }
};

const fixIndexes = async () => {
  try {
    await connectDB();
    
    const db = mongoose.connection.db;
    const productsCollection = db.collection('products');
    
    // Get all indexes
    const indexes = await productsCollection.indexes();
    console.log('\nCác indexes hiện tại:');
    indexes.forEach(index => {
      console.log(`- ${index.name}:`, JSON.stringify(index.key, null, 2));
    });
    
    // Try to drop the exact index from error message first
    // MongoDB might add suffixes like _1, _1_1, etc.
    const baseIndexName = 'variants.size_1_variants.color_';
    
    // First, find any index that starts with the base name
    const matchingIndex = indexes.find(idx => 
      idx.name && idx.name.startsWith(baseIndexName) && idx.name !== '_id_'
    );
    
    if (matchingIndex) {
      try {
        console.log(`\nTìm thấy index: ${matchingIndex.name}`);
        console.log(`  Key:`, JSON.stringify(matchingIndex.key));
        console.log(`Đang xóa index...`);
        await productsCollection.dropIndex(matchingIndex.name);
        console.log(`✓ Đã xóa index cũ: ${matchingIndex.name}`);
        
        // Refresh indexes list
        const updatedIndexes = await productsCollection.indexes();
        indexes.length = 0;
        indexes.push(...updatedIndexes);
      } catch (err) {
        console.log(`  ⚠ Lỗi khi xóa ${matchingIndex.name}: ${err.message}`);
        // Try dropping by key as fallback
        try {
          console.log(`  Thử xóa bằng key...`);
          await productsCollection.dropIndex(matchingIndex.key);
          console.log(`✓ Đã xóa index cũ bằng key: ${matchingIndex.name}`);
        } catch (keyErr) {
          console.log(`  ⚠ Không thể xóa bằng key: ${keyErr.message}`);
        }
      }
    } else {
      console.log(`\nKhông tìm thấy index bắt đầu với: ${baseIndexName}`);
    }
    
    // Find all indexes related to variants
    const variantIndexes = indexes.filter(idx => {
      const nameMatch = idx.name && (
        idx.name.includes('variants') || 
        idx.name.includes('size_') ||
        idx.name.includes('color_')
      );
      const keyMatch = JSON.stringify(idx.key).includes('variants');
      return (nameMatch || keyMatch) && idx.name !== '_id_';
    });
    
    if (variantIndexes.length > 0) {
      console.log(`\nTìm thấy ${variantIndexes.length} index(es) liên quan đến variants:`);
      variantIndexes.forEach(idx => {
        console.log(`  - ${idx.name}`);
      });
      
      // Try to drop each problematic index
      for (const problematicIndex of variantIndexes) {
        try {
          console.log(`\nĐang xóa index: ${problematicIndex.name}`);
          await productsCollection.dropIndex(problematicIndex.name);
          console.log(`✓ Đã xóa index cũ: ${problematicIndex.name}`);
        } catch (dropError) {
          // Try dropping by key if name doesn't work
          try {
            console.log(`  Thử xóa bằng key...`);
            await productsCollection.dropIndex(problematicIndex.key);
            console.log(`✓ Đã xóa index cũ bằng key: ${problematicIndex.name}`);
          } catch (keyError) {
            console.log(`  ⚠ Không thể xóa index ${problematicIndex.name}: ${dropError.message}`);
          }
        }
      }
    } else {
      console.log('\n✓ Không tìm thấy index có vấn đề liên quan đến variants');
    }
    
    // List remaining indexes
    const remainingIndexes = await productsCollection.indexes();
    console.log('\nCác indexes còn lại:');
    remainingIndexes.forEach(index => {
      console.log(`- ${index.name}:`, JSON.stringify(index.key, null, 2));
    });
    
    console.log('\n✓ Hoàn thành!');
    process.exit(0);
  } catch (error) {
    console.error('Lỗi khi xử lý indexes:', error.message);
    process.exit(1);
  }
};

fixIndexes();

