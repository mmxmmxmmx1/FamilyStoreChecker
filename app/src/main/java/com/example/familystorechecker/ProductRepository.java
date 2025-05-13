package com.example.familystorechecker;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class ProductRepository {
    private final ProductDao productDao;
    private final LiveData<List<Product>> allProducts;

    // 建構子：取得資料庫與 DAO 實例
    public ProductRepository(Application application) {
        ProductDatabase db = ProductDatabase.getDatabase(application);
        productDao = db.productDao();
        allProducts = db.productDao().getAllProductsLive(); // 若使用 LiveData
    }

    // 回傳所有商品（LiveData 形式）
    public LiveData<List<Product>> getAllProducts() {
        return allProducts;
    }

    // 新增商品（非同步）
    public void insert(Product product) {
        ProductDatabase.databaseWriteExecutor.execute(() -> productDao.insert(product));
    }

    // 更新商品（非同步）
    public void update(Product product) {
        ProductDatabase.databaseWriteExecutor.execute(() -> productDao.update(product));
    }

    // 刪除商品（非同步）
    public void delete(Product product) {
        ProductDatabase.databaseWriteExecutor.execute(() -> productDao.delete(product));
    }

    // 根據條碼查詢商品（同步，建議改用 LiveData 配合 ViewModelScope）
    public Product getProductByBarcode(String barcode) {
        return productDao.getProductByBarcode(barcode);
    }
}
