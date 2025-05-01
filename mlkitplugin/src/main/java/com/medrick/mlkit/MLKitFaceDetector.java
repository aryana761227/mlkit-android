package com.medrick.mlkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class MLKitFaceDetector {
    private static final String TAG = "MLKitFaceDetector";
    private FaceDetector detector;
    private Context context;

    public MLKitFaceDetector(Context context) {
        this.context = context;
    }

    public void initializeFaceDetector() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build();

        detector = FaceDetection.getClient(options);
        Log.d(TAG, "Face detector initialized");
    }

    public String detectFaces(byte[] imageData, int width, int height, int rotation) {
        try {
            Bitmap bitmap = convertToBitmap(imageData, width, height);
            InputImage image = InputImage.fromBitmap(bitmap, rotation);

            return detector.process(image)
                    .addOnSuccessListener(faces -> {
                        String result = formatFaceResults(faces);
                        sendToUnity(result);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Face detection failed", e);
                        sendToUnity("ERROR: " + e.getMessage());
                    })
                    .toString();
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            return "ERROR: " + e.getMessage();
        }
    }

    private Bitmap convertToBitmap(byte[] imageData, int width, int height) {
        YuvImage yuv = new YuvImage(imageData, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private String formatFaceResults(List<Face> faces) {
        StringBuilder result = new StringBuilder();
        result.append("FACES_COUNT:").append(faces.size()).append("|");
        for (Face face : faces) {
            Rect bounds = face.getBoundingBox();
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

    private void sendToUnity(String message) {
        Log.d(TAG, "Sending to Unity: " + message);
        // This will be called from Unity's thread, so we can directly log
        // Unity will handle the callback mechanism
    }
}