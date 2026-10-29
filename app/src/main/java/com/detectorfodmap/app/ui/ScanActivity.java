package com.detectorfodmap.app.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.detectorfodmap.app.R;
import com.detectorfodmap.app.databinding.ActivityScanBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Lee un código de barras con la cámara y abre el producto en Open Food Facts. */
public class ScanActivity extends AppCompatActivity {

    private static final String TAG = "ScanActivity";

    private ActivityScanBinding binding;
    private BarcodeScanner scanner;
    private ExecutorService analysisExecutor;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private boolean torchOn;
    private final AtomicBoolean handled = new AtomicBoolean(false);

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    showPermissionDialog();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        applyInsets();

        binding.closeButton.setOnClickListener(view -> finish());
        binding.torchButton.setOnClickListener(view -> toggleTorch());

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_128)
                .build();
        scanner = BarcodeScanning.getClient(options);
        analysisExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void applyInsets() {
        View controls = binding.controls;
        ViewCompat.setOnApplyWindowInsetsListener(controls, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(controls);
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(analysisExecutor, this::analyze);

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(
                        this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
            } catch (Exception exception) {
                Log.e(TAG, "No se pudo abrir la cámara", exception);
                Toast.makeText(this, R.string.product_error_body, Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyze(@NonNull ImageProxy proxy) {
        if (handled.get()) {
            proxy.close();
            return;
        }
        android.media.Image mediaImage = proxy.getImage();
        if (mediaImage == null) {
            proxy.close();
            return;
        }
        InputImage input = InputImage.fromMediaImage(
                mediaImage, proxy.getImageInfo().getRotationDegrees());
        scanner.process(input)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String value = barcode.getRawValue();
                        if (isValidBarcode(value)) {
                            onBarcode(value);
                            break;
                        }
                    }
                })
                .addOnFailureListener(exception -> Log.w(TAG, "Fallo al leer el código", exception))
                .addOnCompleteListener(task -> proxy.close());
    }

    private static boolean isValidBarcode(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.length() < 8 || trimmed.length() > 14) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void onBarcode(String value) {
        if (!handled.compareAndSet(false, true)) {
            return;
        }
        binding.progress.setVisibility(View.VISIBLE);
        binding.scanFrame.setVisibility(View.INVISIBLE);
        feedback();
        Intent intent = new Intent(this, ProductActivity.class);
        intent.putExtra(ProductActivity.EXTRA_BARCODE, value);
        startActivity(intent);
        finish();
    }

    @SuppressWarnings("deprecation")
    private void feedback() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(
                            60, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(60);
                }
            }
        } catch (Exception ignored) {
            // Sin vibración disponible: no es crítico.
        }
    }

    private void toggleTorch() {
        if (camera == null) {
            return;
        }
        torchOn = !torchOn;
        camera.getCameraControl().enableTorch(torchOn);
        binding.torchButton.setAlpha(torchOn ? 1f : 0.65f);
    }

    private void showPermissionDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.scan_permission_title)
                .setMessage(R.string.scan_permission_message)
                .setPositiveButton(R.string.scan_permission_ok, (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (scanner != null) {
            scanner.close();
        }
        if (analysisExecutor != null) {
            analysisExecutor.shutdown();
        }
    }
}
