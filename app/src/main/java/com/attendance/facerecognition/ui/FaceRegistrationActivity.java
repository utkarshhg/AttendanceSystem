package com.attendance.facerecognition.ui;

import android.annotation.SuppressLint;
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
import androidx.core.content.ContextCompat;

import com.attendance.facerecognition.R;
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
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceRegistrationActivity extends AppCompatActivity {
    private static final String TAG = "FaceRegistration";
    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;

    private Interpreter tflite;
    private ImageView facePreview;

    // ==========================================
    // Memory to hold Face 1 for our live test
    // ==========================================
    private float[] savedTestFingerprint = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_registration);

        viewFinder = findViewById(R.id.viewFinder);
        Button captureButton = findViewById(R.id.capture_button);
        facePreview = findViewById(R.id.face_preview);

        cameraExecutor = Executors.newSingleThreadExecutor();

        try {
            tflite = new Interpreter(loadModelFile("mobile_face_net.tflite"));
            Log.d(TAG, "TFLite model loaded successfully!");
        } catch (IOException e) {
            Log.e(TAG, "Error loading TFLite model", e);
            Toast.makeText(this, "Failed to load AI model.", Toast.LENGTH_LONG).show();
        }

        startCamera();
        captureButton.setOnClickListener(v -> takePhoto());
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
                            Log.d(TAG, "No face detected in this frame.");
                        } else {
                            Log.d(TAG, "Found " + faces.size() + " faces in the frame!");

                            // ========================================================
                            // THE NEW MULTIPLE-FACE LOOP!
                            // ========================================================
                            for (int i = 0; i < faces.size(); i++) {
                                Face currentFace = faces.get(i);
                                Rect bounds = currentFace.getBoundingBox();

                                try {
                                    Bitmap fullBitmap = imageProxy.toBitmap();
                                    Matrix matrix = new Matrix();
                                    matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());
                                    Bitmap rotatedBitmap = Bitmap.createBitmap(fullBitmap, 0, 0, fullBitmap.getWidth(), fullBitmap.getHeight(), matrix, true);

                                    // Crop THIS specific student's face
                                    int x = Math.max(bounds.left, 0);
                                    int y = Math.max(bounds.top, 0);
                                    int width = Math.min(bounds.width(), rotatedBitmap.getWidth() - x);
                                    int height = Math.min(bounds.height(), rotatedBitmap.getHeight() - y);

                                    Bitmap croppedFace = Bitmap.createBitmap(rotatedBitmap, x, y, width, height);
                                    Bitmap scaledFace = Bitmap.createScaledBitmap(croppedFace, 112, 112, false);

                                    // Show the most recently processed face in the preview box
                                    runOnUiThread(() -> facePreview.setImageBitmap(scaledFace));

                                    // 1. Convert to raw math
                                    ByteBuffer inputBuffer = convertBitmapToByteBuffer(scaledFace);
                                    float[][] faceEmbedding = new float[1][192];

                                    // 2. Run the AI for THIS student
                                    tflite.run(inputBuffer, faceEmbedding);

                                    Log.d(TAG, "Processed Face #" + (i + 1) + " | Fingerprint Generated!");

                                    // 3. The Live Match Test
                                    if (savedTestFingerprint == null) {
                                        savedTestFingerprint = faceEmbedding[0];
                                        runOnUiThread(() -> Toast.makeText(FaceRegistrationActivity.this, "Face 1 Saved! Bring a friend into the frame.", Toast.LENGTH_SHORT).show());
                                    } else {
                                        float distance = calculateDistance(savedTestFingerprint, faceEmbedding[0]);

                                        if (distance < 1.0f) {
                                            Log.d(TAG, "Face #" + (i + 1) + " -> ✅ MATCH! It's You! (Score: " + distance + ")");
                                        } else {
                                            Log.d(TAG, "Face #" + (i + 1) + " -> ❌ NO MATCH! Imposter! (Score: " + distance + ")");
                                        }
                                    }

                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing Face #" + (i + 1), e);
                                }
                            }

                            // Let the professor know we processed a group!
                            if (faces.size() > 1) {
                                // Tell us EXACTLY how many faces it found every single time we click!
                                runOnUiThread(() -> Toast.makeText(FaceRegistrationActivity.this, "AI Found " + faces.size() + " Faces!", Toast.LENGTH_LONG).show());
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

    // ==========================================
    // The Euclidean Distance math function
    // ==========================================
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
        if (tflite != null) {
            tflite.close();
        }
    }
}