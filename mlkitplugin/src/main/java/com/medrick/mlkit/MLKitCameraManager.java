package com.medrick.mlkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.unity3d.player.UnityPlayer;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MLKitCameraManager {
    private static final String TAG = "MLKitCameraManager";

    private Context context;
    private FaceDetector faceDetector;
    private ExecutorService cameraExecutor;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;
    private int rotationDegrees = 0;
    private boolean isCameraInitialized = false;
    private int analyzerInterval = 5; // Analyze every 5 frames
    private int frameCounter = 0;

    // Add the LifecycleOwner wrapper
    private UnityLifecycleOwner lifecycleOwner;

    public MLKitCameraManager(Context context) {
        this.context = context;
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Create the lifecycle owner
        lifecycleOwner = new UnityLifecycleOwner();

        // Setup face detector
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build();

        faceDetector = FaceDetection.getClient(options);

        Log.d(TAG, "MLKitCameraManager initialized with UnityLifecycleOwner");
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                Log.d(TAG, "Camera provider future completed");
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
                isCameraInitialized = true;
                Log.d(TAG, "Camera initialized successfully, sending to Unity");
                UnityPlayer.UnitySendMessage("MLKitManager", "OnCameraInitialized", "SUCCESS");
            } catch (ExecutionException | CameraAccessException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: ", e);
                UnityPlayer.UnitySendMessage("MLKitManager", "OnCameraInitialized", "ERROR: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void bindCameraUseCases() throws CameraAccessException, InterruptedException {
        if (cameraProvider == null) {
            throw new IllegalStateException("Camera initialization failed.");
        }

        // Get screen metrics
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        // Setup image analysis
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new FaceAnalyzer());

        // Setup camera preview
        Preview preview = new Preview.Builder().build();

        // Must unbind before rebinding
        cameraProvider.unbindAll();

        // Determine display rotation
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        String[] cameraIds = cameraManager.getCameraIdList();
        for (String id : cameraIds) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == lensFacing) {
                rotationDegrees = UnityPlayer.currentActivity.getWindowManager().getDefaultDisplay().getRotation();
                break;
            }
        }

        // Bind to lifecycle using our custom LifecycleOwner instead of casting Unity's activity
        camera = cameraProvider.bindToLifecycle(
                lifecycleOwner, // Use our custom LifecycleOwner here
                cameraSelector,
                preview,
                imageAnalysis);
    }

    private class FaceAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            frameCounter++;

            // Only analyze every X frames for performance
            if (frameCounter % analyzerInterval != 0) {
                imageProxy.close();
                return;
            }

            // Get image
            InputImage image = InputImage.fromMediaImage(
                    imageProxy.getImage(),
                    imageProxy.getImageInfo().getRotationDegrees());

            // Process image
            faceDetector.process(image)
                    .addOnSuccessListener(faces -> {
                        // Format results
                        String result = formatFaceResults(faces, imageProxy.getWidth(), imageProxy.getHeight());
                        UnityPlayer.UnitySendMessage("MLKitManager", "OnFaceDetectionResult", result);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Face detection failed", e);
                        UnityPlayer.UnitySendMessage("MLKitManager", "OnFaceDetectionResult", "ERROR: " + e.getMessage());
                    })
                    .addOnCompleteListener(task -> {
                        imageProxy.close();
                    });
        }
    }

    private String formatFaceResults(List<Face> faces, int width, int height) {
        StringBuilder result = new StringBuilder();
        result.append("FACES_COUNT:").append(faces.size()).append("|");

        for (Face face : faces) {
            android.graphics.Rect bounds = face.getBoundingBox();

            // Convert face bounds to normalized coordinates (0-1)
            float normalizedLeft = bounds.left / (float) width;
            float normalizedTop = bounds.top / (float) height;
            float normalizedWidth = bounds.width() / (float) width;
            float normalizedHeight = bounds.height() / (float) height;

            Float smilingProbability = face.getSmilingProbability();
            Float leftEyeOpenProbability = face.getLeftEyeOpenProbability();
            Float rightEyeOpenProbability = face.getRightEyeOpenProbability();
            float upsetness = calculateUpsetness(smilingProbability, leftEyeOpenProbability, rightEyeOpenProbability);
            result.append("FACE:")
                    .append(normalizedLeft).append(",")
                    .append(normalizedTop).append(",")
                    .append(normalizedWidth).append(",")
                    .append(normalizedHeight).append("|")
                    .append("SMILE:").append(smilingProbability).append("|")
                    .append("LEFT_EYE:").append(leftEyeOpenProbability).append("|")
                    .append("RIGHT_EYE:").append(rightEyeOpenProbability).append("|")
                    .append("UPSETNESS:").append(upsetness).append("|");

        }

        return result.toString();
    }
    private float calculateUpsetness(Float smileProbability, Float leftEyeOpenProbability, Float rightEyeOpenProbability) {
        // Handle null values (can happen if face data is incomplete)
        float smile = smileProbability != null ? smileProbability : 0.5f;
        float leftEye = leftEyeOpenProbability != null ? leftEyeOpenProbability : 0.5f;
        float rightEye = rightEyeOpenProbability != null ? rightEyeOpenProbability : 0.5f;

        // Calculate upsetness score (range 0-1)
        // 1. Invert smile (not smiling can indicate upset)
        float notSmiling = 1.0f - smile;

        // 2. Eye openness - both fully open or fully closed aren't upset indicators
        //    Partially closed eyes can indicate upset
        float leftEyeUpset = Math.abs(leftEye - 0.3f) < 0.3f ? (0.3f - Math.abs(leftEye - 0.3f)) / 0.3f : 0f;
        float rightEyeUpset = Math.abs(rightEye - 0.3f) < 0.3f ? (0.3f - Math.abs(rightEye - 0.3f)) / 0.3f : 0f;
        float eyeUpset = Math.max(leftEyeUpset, rightEyeUpset);

        // 3. Combine factors (weighted sum)
        // Not smiling is a stronger indicator than eye state
        float upsetness = (0.7f * notSmiling) + (0.3f * eyeUpset);

        // Ensure range is 0-1
        return Math.min(1.0f, Math.max(0.0f, upsetness));
    }
    public void setDetectionInterval(int interval) {
        this.analyzerInterval = Math.max(1, interval);
    }

    public void switchCamera() {
        if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            lensFacing = CameraSelector.LENS_FACING_BACK;
        } else {
            lensFacing = CameraSelector.LENS_FACING_FRONT;
        }

        try {
            if (cameraProvider != null) {
                bindCameraUseCases();
            }
        } catch (CameraAccessException | InterruptedException e) {
            Log.e(TAG, "Error switching camera: ", e);
        }
    }

    public void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    public void release() {
        stopCamera();
        cameraExecutor.shutdown();

        // Properly destroy our lifecycle owner
        if (lifecycleOwner != null) {
            lifecycleOwner.destroy();
        }
    }
}