package com.medrick.mlkit;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

    // Configure face detection features
    public static void configureFaceDetection(final boolean enableLandmarks, final boolean enableContours) {
        if (cameraManager == null) {
            Log.e(TAG, "Camera manager not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cameraManager.configureDetection(enableLandmarks, enableContours);
                Log.d(TAG, "Face detection configured - landmarks: " + enableLandmarks + ", contours: " + enableContours);
            }
        });
    }

    // NEW METHOD: Set detection mode (FACE_DETECTION or FACE_MESH)
    public static void setDetectionMode(final String mode) {
        if (cameraManager == null) {
            Log.e(TAG, "Camera manager not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cameraManager.setDetectionMode(mode);
                Log.d(TAG, "Detection mode set to: " + mode);
            }
        });
    }

    // NEW METHOD: Configure face mesh detection
    public static void configureFaceMesh(final boolean useHighAccuracy) {
        if (cameraManager == null) {
            Log.e(TAG, "Camera manager not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cameraManager.configureFaceMesh(useHighAccuracy);
                Log.d(TAG, "Face mesh configured - high accuracy: " + useHighAccuracy);
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