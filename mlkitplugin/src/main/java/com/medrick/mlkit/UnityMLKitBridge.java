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
                cameraManager = new MLKitCameraManager(currentActivity);

                // Notify Unity that initialization is complete
                UnityPlayer.UnitySendMessage("MLKitManager", "OnInitialized", "SUCCESS");
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