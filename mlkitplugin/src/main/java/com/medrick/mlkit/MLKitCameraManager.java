package com.medrick.mlkit;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

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

    public MLKitCameraManager(Context context) {
        this.context = context;
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Setup face detector
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build();

        faceDetector = FaceDetection.getClient(options);
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
                isCameraInitialized = true;
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

        // Bind to lifecycle
        camera = cameraProvider.bindToLifecycle(
                (LifecycleOwner) UnityPlayer.currentActivity,
                cameraSelector,
                preview,
                imageAnalysis);
    }

    private class FaceAnalyzer implements ImageAnalysis.Analyzer {
        @ExperimentalGetImage
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
                        String result = formatFaceResults(faces);
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

    private String formatFaceResults(List<Face> faces) {
        StringBuilder result = new StringBuilder();
        result.append("FACES_COUNT:").append(faces.size()).append("|");

        for (Face face : faces) {
            android.graphics.Rect bounds = face.getBoundingBox();
            result.append("FACE:")
                    .append(bounds.left).append(",")
                    .append(bounds.top).append(",")
                    .append(bounds.width()).append(",")
                    .append(bounds.height()).append("|")
                    .append("SMILE:").append(face.getSmilingProbability()).append("|")
                    .append("LEFT_EYE:").append(face.getLeftEyeOpenProbability()).append("|")
                    .append("RIGHT_EYE:").append(face.getRightEyeOpenProbability()).append("|");
        }

        return result.toString();
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
    }
}