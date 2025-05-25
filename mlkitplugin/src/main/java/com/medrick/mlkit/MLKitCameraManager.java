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
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
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

    // Updated to include more facial features
    private boolean enableLandmarks = true;
    private boolean enableContours = true;

    public MLKitCameraManager(Context context) {
        this.context = context;
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Create the lifecycle owner
        lifecycleOwner = new UnityLifecycleOwner();

        // Setup face detector with enhanced options
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)  // Enable all landmarks
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)    // Enable all contours
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build();

        faceDetector = FaceDetection.getClient(options);

        Log.d(TAG, "MLKitCameraManager initialized with UnityLifecycleOwner and enhanced detection options");
    }

    // Allow options to be configured from Unity
    public void configureDetection(boolean enableLandmarks, boolean enableContours) {
        this.enableLandmarks = enableLandmarks;
        this.enableContours = enableContours;

        // Reinitialize face detector with updated options
        FaceDetectorOptions.Builder optionsBuilder = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f);

        if (enableLandmarks) {
            optionsBuilder.setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL);
        } else {
            optionsBuilder.setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE);
        }

        if (enableContours) {
            optionsBuilder.setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL);
        } else {
            optionsBuilder.setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE);
        }

        faceDetector = FaceDetection.getClient(optionsBuilder.build());
        Log.d(TAG, "Reconfigured face detector - landmarks: " + enableLandmarks + ", contours: " + enableContours);
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

    // FIXED: Properly implement ImageAnalysis.Analyzer interface
    private class FaceAnalyzer implements ImageAnalysis.Analyzer {

        // This method is required for newer versions of CameraX
        @Override
        public Size getDefaultTargetResolution() {
            // Return a reasonable default resolution
            return new Size(640, 480);
        }

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
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(
                        mediaImage,
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
            } else {
                imageProxy.close();
            }
        }
    }

    private String formatFaceResults(List<Face> faces, int width, int height) {
        StringBuilder result = new StringBuilder();
        result.append("FACES_COUNT:").append(faces.size()).append("|");

        for (int faceIndex = 0; faceIndex < faces.size(); faceIndex++) {
            Face face = faces.get(faceIndex);
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

            // Basic face data
            result.append("FACE:")
                    .append(normalizedLeft).append(",")
                    .append(normalizedTop).append(",")
                    .append(normalizedWidth).append(",")
                    .append(normalizedHeight).append("|")
                    .append("SMILE:").append(smilingProbability != null ? smilingProbability : 0).append("|")
                    .append("LEFT_EYE:").append(leftEyeOpenProbability != null ? leftEyeOpenProbability : 0).append("|")
                    .append("RIGHT_EYE:").append(rightEyeOpenProbability != null ? rightEyeOpenProbability : 0).append("|")
                    .append("UPSETNESS:").append(upsetness).append("|");

            // Add landmarks if enabled
            if (enableLandmarks) {
                addLandmarks(result, face, faceIndex, width, height);
            }

            // Add contours if enabled
            if (enableContours) {
                addContours(result, face, faceIndex, width, height);
            }
        }

        return result.toString();
    }

    // Helper method to add face landmarks to the result string
    private void addLandmarks(StringBuilder result, Face face, int faceIndex, int width, int height) {
        // Process all possible landmarks
        addLandmarkIfPresent(result, face, FaceLandmark.LEFT_EYE, 0, faceIndex, width, height);
        addLandmarkIfPresent(result, face, FaceLandmark.RIGHT_EYE, 1, faceIndex, width, height);
        addLandmarkIfPresent(result, face, FaceLandmark.LEFT_EAR, 3, faceIndex, width, height);
        addLandmarkIfPresent(result, face, FaceLandmark.RIGHT_EAR, 4, faceIndex, width, height);
        addLandmarkIfPresent(result, face, FaceLandmark.LEFT_CHEEK, 5, faceIndex, width, height);
        addLandmarkIfPresent(result, face, FaceLandmark.RIGHT_CHEEK, 6, faceIndex, width, height);
        addLandmarkIfPresent(result, face, FaceLandmark.NOSE_BASE, 7, faceIndex, width, height);
        addLandmarkIfPresent(result, face, FaceLandmark.MOUTH_LEFT, 8, faceIndex, width, height);
        addLandmarkIfPresent(result, face, FaceLandmark.MOUTH_RIGHT, 9, faceIndex, width, height);
        addLandmarkIfPresent(result, face, FaceLandmark.MOUTH_BOTTOM, 10, faceIndex, width, height);
    }

    // Helper to add a single landmark if it exists
    private void addLandmarkIfPresent(StringBuilder result, Face face, int landmarkType, int unityLandmarkType,
                                      int faceIndex, int width, int height) {
        FaceLandmark landmark = face.getLandmark(landmarkType);
        if (landmark != null) {
            // Normalize coordinates
            float normalizedX = landmark.getPosition().x / (float) width;
            float normalizedY = landmark.getPosition().y / (float) height;

            result.append("LANDMARK:")
                    .append(faceIndex).append(",")
                    .append(unityLandmarkType).append(",")
                    .append(normalizedX).append(",")
                    .append(normalizedY).append("|");
        }
    }

    // Helper method to add face contours to the result string
    private void addContours(StringBuilder result, Face face, int faceIndex, int width, int height) {
        // Process all possible contours
        addContourIfPresent(result, face, FaceContour.FACE, 1, faceIndex, width, height);
        addContourIfPresent(result, face, FaceContour.LEFT_EYEBROW_TOP, 2, faceIndex, width, height);
        addContourIfPresent(result, face, FaceContour.LEFT_EYEBROW_BOTTOM, 3, faceIndex, width, height);
        addContourIfPresent(result, face, FaceContour.RIGHT_EYEBROW_TOP, 4, faceIndex, width, height);
        addContourIfPresent(result, face, FaceContour.RIGHT_EYEBROW_BOTTOM, 5, faceIndex, width, height);
        addContourIfPresent(result, face, FaceContour.LEFT_EYE, 6, faceIndex, width, height);
        addContourIfPresent(result, face, FaceContour.RIGHT_EYE, 7, faceIndex, width, height);
        addContourIfPresent(result, face, FaceContour.UPPER_LIP_TOP, 8, faceIndex, width, height);
        addContourIfPresent(result, face, FaceContour.UPPER_LIP_BOTTOM, 9, faceIndex, width, height);
        addContourIfPresent(result, face, FaceContour.LOWER_LIP_TOP, 10, faceIndex, width, height);
        addContourIfPresent(result, face, FaceContour.LOWER_LIP_BOTTOM, 11, faceIndex, width, height);
        addContourIfPresent(result, face, FaceContour.NOSE_BRIDGE, 12, faceIndex, width, height);
        addContourIfPresent(result, face, FaceContour.NOSE_BOTTOM, 13, faceIndex, width, height);
        addContourIfPresent(result, face, FaceContour.LEFT_CHEEK, 14, faceIndex, width, height);
        addContourIfPresent(result, face, FaceContour.RIGHT_CHEEK, 15, faceIndex, width, height);
    }

    // Helper to add a contour if it exists
    private void addContourIfPresent(StringBuilder result, Face face, int contourType, int unityContourType,
                                     int faceIndex, int width, int height) {
        FaceContour contour = face.getContour(contourType);
        if (contour != null && !contour.getPoints().isEmpty()) {
            int pointCount = contour.getPoints().size();

            // Add contour start marker
            result.append("CONTOUR_START:")
                    .append(faceIndex).append(",")
                    .append(unityContourType).append(",")
                    .append(pointCount).append("|");

            // Add each point in the contour
            for (android.graphics.PointF point : contour.getPoints()) {
                // Normalize coordinates
                float normalizedX = point.x / (float) width;
                float normalizedY = point.y / (float) height;

                result.append("CONTOUR_POINT:")
                        .append(normalizedX).append(",")
                        .append(normalizedY).append("|");
            }
        }
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