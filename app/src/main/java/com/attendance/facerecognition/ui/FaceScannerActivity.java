package com.attendance.facerecognition.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.attendance.facerecognition.R;
import com.attendance.facerecognition.database.FirebaseManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceScannerActivity extends AppCompatActivity {
    private static final String TAG = "FaceScanner";

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;

    private Interpreter tflite;
    private ImageView facePreview;
    private FaceBoxOverlay faceOverlay;

    private FirebaseManager db;
    private String currentStudentId;
    private String mode;
    private String subjectName;
    private String branchName;

    private Map<String, float[]> faceDatabase = new HashMap<>();
    private ArrayList<String> alreadyMarkedStudents = new ArrayList<>();
    private boolean isDatabaseLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_scanner);

        viewFinder = findViewById(R.id.viewFinder);
        Button captureButton = findViewById(R.id.capture_button);
        facePreview = findViewById(R.id.face_preview);
        faceOverlay = findViewById(R.id.faceOverlay);

        db = new FirebaseManager();
        currentStudentId = getIntent().getStringExtra("STUDENT_ID");
        mode = getIntent().getStringExtra("MODE");
        subjectName = getIntent().getStringExtra("SUBJECT");
        branchName = getIntent().getStringExtra("BRANCH");

        cameraExecutor = Executors.newSingleThreadExecutor();

        try {
            tflite = new Interpreter(loadModelFile("mobile_face_net.tflite"));
            Log.d(TAG, "TFLite model loaded successfully!");
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load AI model. Check assets folder.", Toast.LENGTH_LONG).show();
        }

        if ("ATTENDANCE".equals(mode)) {
            loadFaceDatabase();
            captureButton.setText("Scan Classroom");
        }

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        captureButton.setOnClickListener(v -> takePhoto());
    }

    private void loadFaceDatabase() {
        Toast.makeText(this, "Downloading Face Bank...", Toast.LENGTH_SHORT).show();

        db.getAllFaceEmbeddings(new FirebaseManager.OnAllEmbeddingsRetrieved() {
            @Override
            public void onRetrieved(Map<String, float[]> data) {
                faceDatabase = data;
                isDatabaseLoaded = true;
                runOnUiThread(() -> Toast.makeText(FaceScannerActivity.this, "Face Bank Loaded!", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(FaceScannerActivity.this, "Database Error: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera setup failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;
        if ("ATTENDANCE".equals(mode) && !isDatabaseLoaded) {
            Toast.makeText(this, "Still downloading database...", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "📸 Scanning...", Toast.LENGTH_SHORT).show();

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                processImage(image);
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed", exception);
            }
        });
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void processImage(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setMinFaceSize(0.05f)
                    .build();

            FaceDetector detector = FaceDetection.getClient(options);

            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (!faces.isEmpty()) {
                            ArrayList<String> recognizedInThisFrame = new ArrayList<>();
                            ArrayList<String> newlyMarkedThisFrame = new ArrayList<>();
                            int unrecognizedCount = 0;
                            List<FaceBoxOverlay.FaceData> boxesToDraw = new ArrayList<>();

                            for (int i = 0; i < faces.size(); i++) {
                                Face currentFace = faces.get(i);
                                Rect bounds = currentFace.getBoundingBox();

                                try {
                                    Bitmap fullBitmap = imageProxy.toBitmap();
                                    Matrix matrix = new Matrix();
                                    matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());
                                    Bitmap rotatedBitmap = Bitmap.createBitmap(fullBitmap, 0, 0, fullBitmap.getWidth(), fullBitmap.getHeight(), matrix, true);

                                    int padding = 25;
                                    int x = Math.max(bounds.left - padding, 0);
                                    int y = Math.max(bounds.top - padding, 0);
                                    int width = Math.min(bounds.width() + (padding * 2), rotatedBitmap.getWidth() - x);
                                    int height = Math.min(bounds.height() + (padding * 2), rotatedBitmap.getHeight() - y);

                                    Bitmap croppedFace = Bitmap.createBitmap(rotatedBitmap, x, y, width, height);

                                    // 🚨 SAFE BASELINE: 112x112
                                    Bitmap scaledFace = Bitmap.createScaledBitmap(croppedFace, 112, 112, false);

                                    ByteBuffer inputBuffer = convertBitmapToByteBuffer(scaledFace);

                                    // 🚨 SAFE BASELINE: 192 output
                                    float[][] faceEmbedding = new float[1][192];

                                    if (tflite != null) {
                                        tflite.run(inputBuffer, faceEmbedding);
                                    }

                                    float[] currentEmbedding = faceEmbedding[0];

                                    if ("ATTENDANCE".equals(mode)) {
                                        String matchedStudentId = "Unknown";
                                        float minDistance = 0.80f;

                                        for (Map.Entry<String, float[]> entry : faceDatabase.entrySet()) {
                                            if (entry.getValue().length == currentEmbedding.length) {
                                                float distance = calculateDistance(currentEmbedding, entry.getValue());
                                                if (distance < minDistance) {
                                                    minDistance = distance;
                                                    matchedStudentId = entry.getKey();
                                                }
                                            }
                                        }

                                        boxesToDraw.add(new FaceBoxOverlay.FaceData(bounds, matchedStudentId));

                                        if (!matchedStudentId.equals("Unknown")) {
                                            recognizedInThisFrame.add(matchedStudentId);
                                            if (!alreadyMarkedStudents.contains(matchedStudentId)) {
                                                alreadyMarkedStudents.add(matchedStudentId);
                                                newlyMarkedThisFrame.add(matchedStudentId);
                                            }
                                        } else {
                                            unrecognizedCount++;
                                        }

                                    } else {
                                        if (currentStudentId != null && !currentStudentId.isEmpty()) {
                                            db.saveFaceEmbedding(currentStudentId, currentEmbedding, 1, new FirebaseManager.OnCompleteListener() {
                                                @Override
                                                public void onSuccess(String message) {
                                                    runOnUiThread(() -> {
                                                        Toast.makeText(FaceScannerActivity.this, "Face secured!", Toast.LENGTH_LONG).show();
                                                        finish();
                                                    });
                                                }
                                                @Override
                                                public void onError(String error) {
                                                    runOnUiThread(() -> Toast.makeText(FaceScannerActivity.this, "Error: " + error, Toast.LENGTH_LONG).show());
                                                }
                                            });
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Face processing error", e);
                                }
                            }

                            final int finalUnrecognized = unrecognizedCount;

                            if ("ATTENDANCE".equals(mode)) {
                                runOnUiThread(() -> {
                                    faceOverlay.drawFaceBoxes(boxesToDraw, imageProxy.getWidth(), imageProxy.getHeight());
                                    new android.os.Handler().postDelayed(() -> faceOverlay.clear(), 3000);

                                    if (!newlyMarkedThisFrame.isEmpty()) {
                                        Toast.makeText(FaceScannerActivity.this, "✅ Marked: " + String.join(", ", newlyMarkedThisFrame), Toast.LENGTH_LONG).show();
                                        String finalSubject = (subjectName != null) ? subjectName : "Unknown";
                                        String finalBranch = (branchName != null) ? branchName : "Unknown";

                                        db.markAttendance(finalSubject, alreadyMarkedStudents, finalBranch, new FirebaseManager.OnCompleteListener() {
                                            @Override
                                            public void onSuccess(String message) { Log.d(TAG, "Saved!"); }
                                            @Override
                                            public void onError(String error) { Log.e(TAG, "Save failed"+error); }
                                        });

                                    } else if (finalUnrecognized > 0 && recognizedInThisFrame.isEmpty()) {
                                        Toast.makeText(FaceScannerActivity.this, "❌ Unrecognized Face", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    // 🚨 SAFE BASELINE: 112x112 Buffer
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 112 * 112 * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[112 * 112];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < 112; ++i) {
            for (int j = 0; j < 112; ++j) {
                int val = intValues[pixel++];
                byteBuffer.putFloat((((val >> 16) & 0xFF) - 127.5f) / 127.5f);
                byteBuffer.putFloat((((val >> 8) & 0xFF) - 127.5f) / 127.5f);
                byteBuffer.putFloat(((val & 0xFF) - 127.5f) / 127.5f);
            }
        }
        return byteBuffer;
    }

    private MappedByteBuffer loadModelFile(String modelName) throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private float calculateDistance(float[] face1, float[] face2) {
        float distance = 0f;
        for (int i = 0; i < face1.length; i++) {
            float diff = face1[i] - face2[i];
            distance += diff * diff;
        }
        return (float) Math.sqrt(distance);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (tflite != null) tflite.close();
    }
}