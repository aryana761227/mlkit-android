package com.medrick.mlkit;

import android.app.Activity;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

public class UnityMLKitBridge {
    private static final String TAG = "UnityMLKitBridge";
    private static MLKitCameraManager cameraManager;
    private static Activity currentActivity;

    // Called from Unity to initialize the ML Kit
    public static void initialize() {
        Log.d(TAG, "Initializing ML Kit");
        currentActivity = UnityPlayer.currentActivity;

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    cameraManager = new MLKitCameraManager(currentActivity);
                    Log.d(TAG, "Camera manager created successfully");

                    // Notify Unity that initialization is complete
                    UnityPlayer.UnitySendMessage("MLKitManager", "OnInitialized", "SUCCESS");

                    // Automatically start the camera after initialization
                    Log.d(TAG, "Auto-starting camera after initialization");
                } catch (Exception e) {
                    Log.e(TAG, "Error during initialization: " + e.getMessage(), e);
                    UnityPlayer.UnitySendMessage("MLKitManager", "OnInitialized", "ERROR: " + e.getMessage());
                }
            }
        });
    }

    // Called from Unity to start the camera
    public static void startCamera() {
        if (cameraManager == null) {
            Log.e(TAG, "Camera manager not initialized");
            UnityPlayer.UnitySendMessage("MLKitManager", "OnCameraInitialized", "ERROR: Not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cameraManager.startCamera();
            }
        });
    }

    // Called from Unity to switch between front and back camera
    public static void switchCamera() {
        if (cameraManager == null) {
            Log.e(TAG, "Camera manager not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cameraManager.switchCamera();
            }
        });
    }

    // Called from Unity to set detection interval (performance vs. accuracy)
    public static void setDetectionInterval(final int interval) {
        if (cameraManager == null) {
            Log.e(TAG, "Camera manager not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cameraManager.setDetectionInterval(interval);
            }
        });
    }
    // Called from Unity to configure face detection (landmark and contours)
    public static void configureFaceDetection(final boolean enableLandmarks, final boolean enableContours) {
        if (cameraManager == null) {
            Log.e(TAG, "Camera manager not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                cameraManager.configureDetection(enableLandmarks, enableContours);
                Log.d(TAG, "Face detection configured - landmarks: " + enableLandmarks + ", contours: " + enableContours);
            }
        });
    }
    // Called from Unity to set detection interval (landmark and contours)
    public static void setFaceActivations(final boolean enableLightWeightFaceDetector, final boolean enableFaceMeshPoints, final boolean enableFaceMeshTriangles) {
        if (cameraManager == null) {
            Log.e(TAG, "Camera manager not initialized");
            return;
        }
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cameraManager.ActivateLightWeightFaceDetector(enableLightWeightFaceDetector);
                cameraManager.ActivateFaceMeshPoints(enableFaceMeshPoints);
                cameraManager.ActivateFaceMeshTriangles(enableFaceMeshTriangles);
                Log.d(TAG, "Face detection activation - lightweight: " + enableLightWeightFaceDetector + ", face mesh points: " + enableFaceMeshPoints + ", face mesh triangles: " + enableFaceMeshTriangles);
            }
        });
    }
    // Called from Unity to stop the camera
    public static void stopCamera() {
        if (cameraManager == null) {
            Log.e(TAG, "Camera manager not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cameraManager.stopCamera();
            }
        });
    }
}