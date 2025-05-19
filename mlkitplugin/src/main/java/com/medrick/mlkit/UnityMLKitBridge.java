package com.medrick.mlkit;

import android.app.Activity;

import com.unity3d.player.UnityPlayer;
import com.google.mlkit.vision.face.FaceDetectorOptions;

/**
 * Bridge class that handles communication between Unity and Google ML Kit.
 * This class provides static methods that can be called from Unity to access
 * ML Kit face detection and face mesh functionality.
 */
public class UnityMLKitBridge {
    private static final String TAG = "UnityMLKitBridge";

    // Feature detector instances
    private static MLKitFaceDetector faceDetector;
    private static MLKitFaceMeshDetector faceMeshDetector;

    private static Activity currentActivity;
    private static boolean isInitialized = false;

    /**
     * Set the log level for the ML Kit bridge.
     * @param logLevel The log level (0=None, 1=Error, 2=Warn, 3=Info, 4=Debug, 5=Verbose)
     */
    public static void setLogLevel(final int logLevel) {
        MLKitLogger.setLogLevel(logLevel);
        MLKitLogger.i(TAG, "Log level set to: " + logLevel);
    }

    /**
     * Initialize the ML Kit bridge.
     * Called from Unity to set up the ML Kit bridge.
     */
    public static void initialize() {
        MLKitLogger.d(TAG, "Initializing ML Kit Bridge");
        currentActivity = UnityPlayer.currentActivity;

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Initialize common resources
                isInitialized = true;

                // Notify Unity that initialization is complete
                UnityPlayer.UnitySendMessage("MLKitManager", "OnInitialized", "SUCCESS");
                MLKitLogger.i(TAG, "ML Kit Bridge initialized successfully");
            }
        });
    }

    /**
     * Initialize the face detector with default options.
     */
    public static void initializeFaceDetector() {
        if (!isInitialized) {
            MLKitLogger.e(TAG, "Bridge not initialized. Call initialize() first.");
            UnityPlayer.UnitySendMessage("MLKitFaceDetection", "OnInitialized", "ERROR: Bridge not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Create with default options
                FaceDetectorOptions.Builder faceOptionsBuilder = new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                        .setMinFaceSize(0.15f);
                FaceDetectorOptions options = faceOptionsBuilder.build();

                faceDetector = new MLKitFaceDetector(currentActivity, options);
                MLKitLogger.d(TAG, "Face detector initialized with default options");

                // Notify Unity that initialization is complete
                UnityPlayer.UnitySendMessage("MLKitFaceDetection", "OnInitialized", "SUCCESS");
            }
        });
    }

    /**
     * Initialize the face detector with custom options.
     * @param optionsString Options string with key=value pairs separated by commas
     */
    public static void initializeFaceDetectorWithOptions(final String optionsString) {
        if (!isInitialized) {
            MLKitLogger.e(TAG, "Bridge not initialized. Call initialize() first.");
            UnityPlayer.UnitySendMessage("MLKitFaceDetection", "OnInitialized", "ERROR: Bridge not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Default values
                    int performanceMode = FaceDetectorOptions.PERFORMANCE_MODE_FAST;
                    int landmarkMode = FaceDetectorOptions.LANDMARK_MODE_NONE;
                    int classificationMode = FaceDetectorOptions.CLASSIFICATION_MODE_NONE;
                    int contourMode = FaceDetectorOptions.CONTOUR_MODE_NONE;
                    float minFaceSize = 0.15f;
                    boolean enableTracking = false;

                    // Parse options string - format: "performanceMode=fast,landmarkMode=all,..."
                    MLKitLogger.v(TAG, "Parsing face detector options: " + optionsString);
                    String[] options = optionsString.split(",");
                    for (String option : options) {
                        String[] keyValue = option.split("=");
                        if (keyValue.length == 2) {
                            String key = keyValue[0].trim();
                            String value = keyValue[1].trim();

                            if (key.equals("performanceMode")) {
                                if (value.equalsIgnoreCase("fast")) {
                                    performanceMode = FaceDetectorOptions.PERFORMANCE_MODE_FAST;
                                } else if (value.equalsIgnoreCase("accurate")) {
                                    performanceMode = FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE;
                                }
                                MLKitLogger.v(TAG, "Set performanceMode to: " + value);
                            } else if (key.equals("landmarkMode")) {
                                if (value.equalsIgnoreCase("none")) {
                                    landmarkMode = FaceDetectorOptions.LANDMARK_MODE_NONE;
                                } else if (value.equalsIgnoreCase("all")) {
                                    landmarkMode = FaceDetectorOptions.LANDMARK_MODE_ALL;
                                }
                                MLKitLogger.v(TAG, "Set landmarkMode to: " + value);
                            } else if (key.equals("contourMode")) {
                                if (value.equalsIgnoreCase("none")) {
                                    contourMode = FaceDetectorOptions.CONTOUR_MODE_NONE;
                                } else if (value.equalsIgnoreCase("all")) {
                                    contourMode = FaceDetectorOptions.CONTOUR_MODE_ALL;
                                }
                                MLKitLogger.v(TAG, "Set contourMode to: " + value);
                            } else if (key.equals("classificationMode")) {
                                if (value.equalsIgnoreCase("none")) {
                                    classificationMode = FaceDetectorOptions.CLASSIFICATION_MODE_NONE;
                                } else if (value.equalsIgnoreCase("all")) {
                                    classificationMode = FaceDetectorOptions.CLASSIFICATION_MODE_ALL;
                                }
                                MLKitLogger.v(TAG, "Set classificationMode to: " + value);
                            } else if (key.equals("minFaceSize")) {
                                try {
                                    float size = Float.parseFloat(value);
                                    if (size >= 0.0f && size <= 1.0f) {
                                        minFaceSize = size;
                                    }
                                    MLKitLogger.v(TAG, "Set minFaceSize to: " + minFaceSize);
                                } catch (NumberFormatException e) {
                                    MLKitLogger.e(TAG, "Invalid minFaceSize value: " + value);
                                }
                            } else if (key.equals("enableTracking")) {
                                enableTracking = Boolean.parseBoolean(value);
                                MLKitLogger.v(TAG, "Set enableTracking to: " + enableTracking);
                            }
                        }
                    }

                    // Build options and create detector
                    FaceDetectorOptions.Builder faceOptionsBuilder = new FaceDetectorOptions.Builder()
                            .setPerformanceMode(performanceMode)
                            .setLandmarkMode(landmarkMode)
                            .setClassificationMode(classificationMode)
                            .setContourMode(contourMode)
                            .setMinFaceSize(minFaceSize);
                    if (enableTracking) {
                        faceOptionsBuilder.enableTracking();
                    }
                    FaceDetectorOptions faceOptions = faceOptionsBuilder.build();
                    faceDetector = new MLKitFaceDetector(currentActivity, faceOptions);
                    MLKitLogger.d(TAG, "Face detector initialized with custom options");
                } catch (Exception e) {
                    MLKitLogger.e(TAG, "Error initializing face detector with options", e);
                    FaceDetectorOptions options = new FaceDetectorOptions.Builder().build();
                    faceDetector = new MLKitFaceDetector(currentActivity, options);
                }

                // Notify Unity that initialization is complete
                UnityPlayer.UnitySendMessage("MLKitFaceDetection", "OnInitialized", "SUCCESS");
            }
        });
    }

    /**
     * Initialize the face mesh detector with default options.
     */
    public static void initializeFaceMeshDetector() {
        if (!isInitialized) {
            MLKitLogger.e(TAG, "Bridge not initialized. Call initialize() first.");
            UnityPlayer.UnitySendMessage("MLKitFaceMesh", "OnInitialized", "ERROR: Bridge not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (faceMeshDetector == null) {
                    faceMeshDetector = new MLKitFaceMeshDetector(currentActivity);
                    faceMeshDetector.initializeFaceMeshDetector();
                    MLKitLogger.d(TAG, "Face mesh detector initialized with default options");
                }

                // Notify Unity that initialization is complete
                UnityPlayer.UnitySendMessage("MLKitFaceMesh", "OnInitialized", "SUCCESS");
            }
        });
    }

    /**
     * Initialize the face mesh detector with high accuracy mode.
     * @param optionsString Options string with key=value pairs separated by commas
     */
    public static void initializeFaceMeshDetectorWithOptions(final String optionsString) {
        if (!isInitialized) {
            MLKitLogger.e(TAG, "Bridge not initialized. Call initialize() first.");
            UnityPlayer.UnitySendMessage("MLKitFaceMesh", "OnInitialized", "ERROR: Bridge not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean useHighAccuracyMode = false;

                // Parse options string
                try {
                    MLKitLogger.v(TAG, "Parsing face mesh detector options: " + optionsString);
                    String[] options = optionsString.split(",");
                    for (String option : options) {
                        String[] keyValue = option.split("=");
                        if (keyValue.length == 2) {
                            String key = keyValue[0].trim();
                            String value = keyValue[1].trim();

                            if (key.equals("useHighAccuracyMode")) {
                                useHighAccuracyMode = Boolean.parseBoolean(value);
                                MLKitLogger.v(TAG, "Set useHighAccuracyMode to: " + useHighAccuracyMode);
                            }
                        }
                    }
                } catch (Exception e) {
                    MLKitLogger.e(TAG, "Error parsing face mesh detector options", e);
                }

                if (faceMeshDetector == null) {
                    faceMeshDetector = new MLKitFaceMeshDetector(currentActivity);
                }

                faceMeshDetector.configureFaceMeshDetector(useHighAccuracyMode);
                faceMeshDetector.initializeFaceMeshDetector();
                MLKitLogger.d(TAG, "Face mesh detector initialized with useHighAccuracyMode: " + useHighAccuracyMode);

                // Notify Unity that initialization is complete
                UnityPlayer.UnitySendMessage("MLKitFaceMesh", "OnInitialized", "SUCCESS");
            }
        });
    }

    /**
     * Detect faces in an image.
     * @param imageData Raw image data in YUV format
     * @param width Image width
     * @param height Image height
     * @param rotation Image rotation (0, 90, 180, 270)
     */
    public static void detectFaces(final byte[] imageData, final int width, final int height, final int rotation) {
        if (faceDetector == null) {
            MLKitLogger.e(TAG, "Face detector not initialized");
            UnityPlayer.UnitySendMessage("MLKitFaceDetection", "OnFaceDetectionResult", "ERROR: Face detector not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MLKitLogger.v(TAG, "Starting face detection: " + width + "x" + height + " @ " + rotation + "°");
                faceDetector.detectFaces(imageData, width, height, rotation, new MLKitResultCallback() {
                    @Override
                    public void onResult(String result) {
                        if (result.startsWith("ERROR:")) {
                            MLKitLogger.e(TAG, "Face detection error: " + result);
                        } else if (result.startsWith("BINARY:")) {
                            MLKitLogger.v(TAG, "Face detection success, binary data size: " +
                                    (result.length() - 7)); // Subtract "BINARY:" prefix
                        }
                        UnityPlayer.UnitySendMessage("MLKitFaceDetection", "OnFaceDetectionResult", result);
                    }
                });
            }
        });
    }

    /**
     * Detect face mesh in an image.
     * @param imageData Raw image data in YUV format
     * @param width Image width
     * @param height Image height
     * @param rotation Image rotation (0, 90, 180, 270)
     */
    public static void detectFaceMesh(final byte[] imageData, final int width, final int height, final int rotation) {
        if (faceMeshDetector == null) {
            MLKitLogger.e(TAG, "Face mesh detector not initialized");
            UnityPlayer.UnitySendMessage("MLKitFaceMesh", "OnFaceMeshDetectionResult", "ERROR: Face mesh detector not initialized");
            return;
        }

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MLKitLogger.v(TAG, "Starting face mesh detection: " + width + "x" + height + " @ " + rotation + "°");
                faceMeshDetector.detectFaceMesh(imageData, width, height, rotation, new MLKitResultCallback() {
                    @Override
                    public void onResult(String result) {
                        if (result.startsWith("ERROR:")) {
                            MLKitLogger.e(TAG, "Face mesh detection error: " + result);
                        } else if (result.startsWith("BINARY:")) {
                            MLKitLogger.v(TAG, "Face mesh detection success, binary data size: " +
                                    (result.length() - 7)); // Subtract "BINARY:" prefix
                        }
                        UnityPlayer.UnitySendMessage("MLKitFaceMesh", "OnFaceMeshDetectionResult", result);
                    }
                });
            }
        });
    }
}