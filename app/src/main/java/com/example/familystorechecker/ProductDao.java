package com.example.familystorechecker;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProductDao {

    // 插入商品
    @Insert
    void insert(Product product);

    // 更新商品
    @Update
    void update(Product product);

    // 刪除商品
    @Delete
    void delete(Product product);

    // 根據條碼查詢單一商品（同步）
    @Query("SELECT * FROM product_table WHERE barcode = :barcode LIMIT 1")
    Product getProductByBarcode(String barcode);

    // 查詢所有商品（非同步回傳 LiveData，可觀察變化）
    @Query("SELECT * FROM product_table ORDER BY name ASC")
    LiveData<List<Product>> getAllProductsLive();
}
