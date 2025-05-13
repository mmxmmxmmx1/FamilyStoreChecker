package com.example.familystorechecker;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 這裡標示該類別是資料庫，並定義資料庫的實體類別（Product）及版本
@Database(entities = {Product.class}, version = 1, exportSchema = false)
public abstract class ProductDatabase extends RoomDatabase {

    // 定義用來操作 Product 資料表的 Dao 介面
    public abstract ProductDao productDao();

    // 建立唯一的資料庫實例
    private static volatile ProductDatabase INSTANCE;

    // 建立資料庫寫入執行緒池
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // 獲取資料庫實例的方法
    public static ProductDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ProductDatabase.class) {
                if (INSTANCE == null) {
                    // 如果資料庫不存在，創建它
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    ProductDatabase.class, "product_database")
                            .fallbackToDestructiveMigration() // 當資料庫版本更新時清除舊資料
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
