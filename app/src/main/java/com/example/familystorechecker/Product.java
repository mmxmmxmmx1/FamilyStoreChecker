package com.example.familystorechecker;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "product_table")
public class Product {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "barcode")
    private String barcode;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "image_resid")
    private int imageResId;

    @ColumnInfo(name = "location")
    private String location;

    // 建構子
    public Product(@NonNull String barcode, String name, int imageResId, String location) {
        this.barcode = barcode;
        this.name = name;
        this.imageResId = imageResId;
        this.location = location;
    }

    // Getter 方法
    @NonNull
    public String getBarcode() { return barcode; }

    public String getName() { return name; }

    public int getImageResId() { return imageResId; }

    public String getLocation() { return location; }

    // Setter 方法
    public void setBarcode(@NonNull String barcode) { this.barcode = barcode; }

    public void setName(String name) { this.name = name; }

    public void setImageResId(int imageResId) { this.imageResId = imageResId; }

    public void setLocation(String location) { this.location = location; }
}
