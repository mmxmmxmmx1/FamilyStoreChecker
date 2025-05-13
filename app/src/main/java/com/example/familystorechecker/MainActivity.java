package com.example.familystorechecker;

// 導入需要的類別
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    // 相機掃描 UI
    private PreviewView previewView;
    private TextView textResult;
    // 商品顯示 UI
    private ImageView imgProduct;
    private TextView textLocation;
    // 動態管理 UI
    private EditText etBarcode;
    private EditText etName;
    private EditText etLocation;
    private Button btnAdd;
    private Button btnRemove;

    private ProductDatabase productDatabase;
    private ProductDao productDao;
    private ExecutorService cameraExecutor;
    private final AtomicBoolean isScanning = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 綁定掃描相關元件
        previewView = findViewById(R.id.previewView);
        textResult = findViewById(R.id.textResult);
        // 綁定商品顯示元件
        imgProduct = findViewById(R.id.imgProduct);
        textLocation = findViewById(R.id.textLocation);
        // 綁定動態管理元件
        etBarcode = findViewById(R.id.etBarcode);
        etName = findViewById(R.id.etName);
        etLocation = findViewById(R.id.etLocation);
        btnAdd = findViewById(R.id.btnAdd);
        btnRemove = findViewById(R.id.btnRemove);

        // 初始化資料庫與DAO
        productDatabase = ProductDatabase.getDatabase(this);
        productDao = productDatabase.productDao();

        // 啟動相機分析執行緒
        cameraExecutor = Executors.newSingleThreadExecutor();

        // 新增商品按鈕
        btnAdd.setOnClickListener(v -> {
            String code = etBarcode.getText().toString().trim();
            String name = etName.getText().toString().trim();
            String loc = etLocation.getText().toString().trim();
            if (code.isEmpty() || name.isEmpty() || loc.isEmpty()) {
                Toast.makeText(this, "請完整輸入條碼、名稱與位置", Toast.LENGTH_SHORT).show();
                return;
            }
            Product product = new Product(code, name, R.drawable.ic_no_product, loc);
            ProductDatabase.databaseWriteExecutor.execute(() -> productDao.insert(product));
            Toast.makeText(this, "新增成功: " + name, Toast.LENGTH_SHORT).show();
        });

        // 刪除商品按鈕
        btnRemove.setOnClickListener(v -> {
            String code = etBarcode.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(this, "請輸入欲刪除的條碼", Toast.LENGTH_SHORT).show();
                return;
            }
            ProductDatabase.databaseWriteExecutor.execute(() -> {
                Product product = productDao.getProductByBarcode(code);
                if (product != null) {
                    productDao.delete(product);
                    runOnUiThread(() -> Toast.makeText(this, "已刪除條碼: " + code, Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "未找到該商品", Toast.LENGTH_SHORT).show());
                }
            });
        });

        // 檢查相機權限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                bindPreviewAndAnalyzer(provider);
            } catch (Exception e) {
                Toast.makeText(this, "無法啟動相機", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreviewAndAnalyzer(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysis.setAnalyzer(cameraExecutor, imageProxy -> {
            if (isScanning.get()) {
                imageProxy.close();
                return;
            }
            isScanning.set(true);

            InputImage image = InputImage.fromMediaImage(
                    imageProxy.getImage(),
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            scanner.process(image)
                    .addOnSuccessListener(this::handleBarcodes)
                    .addOnCompleteListener(task -> {
                        isScanning.set(false);
                        imageProxy.close();
                    });
        });

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
        );
    }

    /** 條碼掃描成功後，顯示商品名稱、圖片與位置 */
    private void handleBarcodes(List<Barcode> barcodes) {
        if (barcodes.isEmpty()) return;

        String code = barcodes.get(0).getRawValue();

        // 改為背景執行查詢，避免觸發 IllegalStateException
        ProductDatabase.databaseWriteExecutor.execute(() -> {
            Product p = productDao.getProductByBarcode(code);

            runOnUiThread(() -> {
                if (p != null) {
                    textResult.setText("已登錄: " + p.getName());
                    imgProduct.setImageResource(p.getImageResId());
                    textLocation.setText("位置: " + p.getLocation());
                } else {
                    textResult.setText("未登錄商品\n條碼: " + code);
                    imgProduct.setImageResource(R.drawable.ic_no_product);
                    textLocation.setText("");
                }
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] perms,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, perms, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this,
                    "相機權限未授權，無法掃描條碼",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
