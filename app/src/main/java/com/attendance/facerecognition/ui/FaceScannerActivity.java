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
// Make sure this import matches your project!
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
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceScannerActivity extends AppCompatActivity {
    private static final String TAG = "FaceScanner";

    // Permission Variables
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;

    private Interpreter tflite;
    private ImageView facePreview;

    // Database and routing variables
    private FirebaseManager db;
    private String currentStudentId;
    private String mode;

    // Memory to hold all registered students for quick scanning
    private Map<String, float[]> faceDatabase = new HashMap<>();
    private ArrayList<String> alreadyMarkedStudents = new ArrayList<>(); // Prevents spamming Firebase
    private boolean isDatabaseLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_scanner);

        viewFinder = findViewById(R.id.viewFinder);
        Button captureButton = findViewById(R.id.capture_button);
        facePreview = findViewById(R.id.face_preview);

        db = new FirebaseManager();
        currentStudentId = getIntent().getStringExtra("STUDENT_ID");
        mode = getIntent().getStringExtra("MODE");

        cameraExecutor = Executors.newSingleThreadExecutor();

        try {
            tflite = new Interpreter(loadModelFile("mobile_face_net.tflite"));
            Log.d(TAG, "TFLite model loaded successfully!");
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load AI model.", Toast.LENGTH_LONG).show();
        }

        // If Professor is taking attendance, download the Face Bank!
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

    // Download the Face Bank from Firebase
    private void loadFaceDatabase() {
        Toast.makeText(this, "Downloading Face Bank...", Toast.LENGTH_SHORT).show();

        db.getAllFaceEmbeddings(new FirebaseManager.OnAllEmbeddingsRetrieved() {
            @Override
            public void onRetrieved(Map<String, float[]> data) {
                faceDatabase = data;
                isDatabaseLoaded = true;

                runOnUiThread(() -> {
                    Toast.makeText(FaceScannerActivity.this,
                            "Face Bank Loaded! (" + faceDatabase.size() + " students)",
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(FaceScannerActivity.this, "Failed to load database: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show();
                finish();
            }
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
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        if ("ATTENDANCE".equals(mode) && !isDatabaseLoaded) {
            Toast.makeText(this, "Still downloading database, please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                processImage(image);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
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
                    .build();

            FaceDetector detector = FaceDetection.getClient(options);

            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (faces.isEmpty()) {
                            // No faces, do nothing (removes annoying spam)
                        } else {
                            // Variables to track the crowd in this specific frame
                            ArrayList<String> recognizedInThisFrame = new ArrayList<>();
                            ArrayList<String> newlyMarkedThisFrame = new ArrayList<>();
                            int unrecognizedCount = 0;

                            for (int i = 0; i < faces.size(); i++) {
                                Face currentFace = faces.get(i);
                                Rect bounds = currentFace.getBoundingBox();

                                try {
                                    Bitmap fullBitmap = imageProxy.toBitmap();
                                    Matrix matrix = new Matrix();
                                    matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());
                                    Bitmap rotatedBitmap = Bitmap.createBitmap(fullBitmap, 0, 0, fullBitmap.getWidth(), fullBitmap.getHeight(), matrix, true);

                                    int x = Math.max(bounds.left, 0);
                                    int y = Math.max(bounds.top, 0);
                                    int width = Math.min(bounds.width(), rotatedBitmap.getWidth() - x);
                                    int height = Math.min(bounds.height(), rotatedBitmap.getHeight() - y);

                                    Bitmap croppedFace = Bitmap.createBitmap(rotatedBitmap, x, y, width, height);
                                    Bitmap scaledFace = Bitmap.createScaledBitmap(croppedFace, 112, 112, false);

                                    runOnUiThread(() -> facePreview.setImageBitmap(scaledFace));

                                    ByteBuffer inputBuffer = convertBitmapToByteBuffer(scaledFace);
                                    float[][] faceEmbedding = new float[1][192];
                                    tflite.run(inputBuffer, faceEmbedding);

                                    float[] currentEmbedding = faceEmbedding[0];

                                    if ("ATTENDANCE".equals(mode)) {
                                        String matchedStudentId = "Unknown";

                                        // ---> THE STRICTNESS DIAL <---
                                        // Properly set to 0.90f
                                        float minDistance = 0.90f;

                                        for (Map.Entry<String, float[]> entry : faceDatabase.entrySet()) {
                                            float distance = calculateDistance(currentEmbedding, entry.getValue());
                                            if (distance < minDistance) {
                                                minDistance = distance;
                                                matchedStudentId = entry.getKey();
                                            }
                                        }

                                        if (!matchedStudentId.equals("Unknown")) {
                                            // 1. ALWAYS add to the visual list so the camera knows you belong here
                                            recognizedInThisFrame.add(matchedStudentId);

                                            // 2. ONLY push to Firebase if you haven't been marked yet today
                                            if (!alreadyMarkedStudents.contains(matchedStudentId)) {
                                                alreadyMarkedStudents.add(matchedStudentId);
                                                newlyMarkedThisFrame.add(matchedStudentId);
                                            }
                                        } else {
                                            unrecognizedCount++;
                                        }

                                    } else {
                                        // REGISTRATION LOGIC
                                        if (currentStudentId != null && !currentStudentId.isEmpty()) {
                                            db.saveFaceEmbedding(currentStudentId, currentEmbedding, 1, new FirebaseManager.OnCompleteListener() {
                                                @Override
                                                public void onSuccess(String message) {
                                                    runOnUiThread(() -> {
                                                        Toast.makeText(FaceScannerActivity.this, "Face securely locked in database!", Toast.LENGTH_LONG).show();
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
                                    Log.e(TAG, "Error processing Face", e);
                                }
                            }

                            // ---> MULTIPLE FACE FEEDBACK LOGIC <---
                            if ("ATTENDANCE".equals(mode)) {
                                if (!newlyMarkedThisFrame.isEmpty()) {
                                    // Yell out the NEW people it just found
                                    String names = String.join(", ", newlyMarkedThisFrame);
                                    runOnUiThread(() -> Toast.makeText(FaceScannerActivity.this, "✅ Marked Present: " + names, Toast.LENGTH_LONG).show());

                                    // Push the updated class list to Firebase
                                    db.markAttendance("Class_Demo", alreadyMarkedStudents, "AI Attendance", new FirebaseManager.OnCompleteListener() {
                                        @Override
                                        public void onSuccess(String message) { Log.d(TAG, "Attendance saved to cloud!"); }
                                        @Override
                                        public void onError(String error) { Log.e(TAG, "Failed to save: " + error); }
                                    });

                                } else if (unrecognizedCount > 0 && recognizedInThisFrame.isEmpty()) {
                                    // ONLY complain about imposters if NO registered students are in the frame
                                    final int finalUnrecognized = unrecognizedCount;
                                    runOnUiThread(() -> Toast.makeText(FaceScannerActivity.this, "❌ " + finalUnrecognized + " Unrecognized Face(s)", Toast.LENGTH_SHORT).show());
                                }
                            }
                        }
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

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