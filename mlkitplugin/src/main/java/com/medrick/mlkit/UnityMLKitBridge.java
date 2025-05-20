package com.medrick.mlkit;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.unity3d.player.UnityPlayer;

public class UnityMLKitBridge {
    private static final String TAG = "UnityMLKitBridge";
    private static MLKitCameraManager cameraManager;  // This is null when startCamera is called
    private static Activity currentActivity;

    // Called from Unity to initialize the ML Kit
    // In UnityMLKitBridge.java
    // In UnityMLKitBridge.java
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
                    startCamera();
                } catch (Exception e) {
                    Log.e(TAG, "Error during initialization: " + e.getMessage(), e);
                    UnityPlayer.UnitySendMessage("MLKitManager", "OnInitialized", "ERROR: " + e.getMessage());
                }
            }
        });
    }
    private static void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(currentActivity, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "Requesting camera permission");
            String[] permissions = {android.Manifest.permission.CAMERA};
            ActivityCompat.requestPermissions(currentActivity, permissions, 100);
        } else {
            Log.d(TAG, "Camera permission already granted");
        }
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

    // The face detection will be handled automatically by the camera manager's analyzer
    // Unity can get the results through the OnFaceDetectionResult callback
}