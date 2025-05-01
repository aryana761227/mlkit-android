package com.medrick.mlkit;

import android.app.Activity;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

public class UnityMLKitBridge {
    private static final String TAG = "UnityMLKitBridge";
    private static MLKitFaceDetector faceDetector;
    private static Activity currentActivity;

    // Called from Unity to initialize the ML Kit
    public static void initialize() {
        Log.d(TAG, "Initializing ML Kit");
        currentActivity = UnityPlayer.currentActivity;

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                faceDetector = new MLKitFaceDetector(currentActivity);
                faceDetector.initializeFaceDetector();

                // Notify Unity that initialization is complete
                UnityPlayer.UnitySendMessage("MLKitManager", "OnInitialized", "SUCCESS");
            }
        });
    }

    // Called from Unity to detect faces in an image
    public static void detectFaces(final byte[] imageData, final int width, final int height) {
        if (faceDetector == null) {
            Log.e(TAG, "Face detector not initialized");
            UnityPlayer.UnitySendMessage("MLKitManager", "OnFaceDetectionResult", "ERROR: Not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String result = faceDetector.detectFaces(imageData, width, height, 0);
                UnityPlayer.UnitySendMessage("MLKitManager", "OnFaceDetectionResult", result);
            }
        });
    }

    // Called from Unity to detect faces with rotation
    public static void detectFacesWithRotation(final byte[] imageData, final int width,
                                               final int height, final int rotation) {
        if (faceDetector == null) {
            Log.e(TAG, "Face detector not initialized");
            UnityPlayer.UnitySendMessage("MLKitManager", "OnFaceDetectionResult", "ERROR: Not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String result = faceDetector.detectFaces(imageData, width, height, rotation);
                UnityPlayer.UnitySendMessage("MLKitManager", "OnFaceDetectionResult", result);
            }
        });
    }
}