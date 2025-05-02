package com.medrick.mlkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Handles face detection using Google ML Kit
 */
public class MLKitFaceDetector {
    private static final String TAG = "MLKitFaceDetector";
    private FaceDetector detector;
    private Context context;

    /**
     * Constructor with context and options
     * @param context Android context
     * @param options Face detector options
     */
    public MLKitFaceDetector(Context context, FaceDetectorOptions options) {
        this.context = context;
        detector = FaceDetection.getClient(options);
        MLKitLogger.d(TAG, "Face detector created with options");
    }

    /**
     * Detect faces in an image
     * @param imageData Raw image data in YUV format
     * @param width Image width
     * @param height Image height
     * @param rotation Image rotation (0, 90, 180, 270)
     * @param callback Callback to receive detection results
     */
    public void detectFaces(byte[] imageData, int width, int height, int rotation, MLKitResultCallback callback) {
        try {
            MLKitLogger.v(TAG, "Converting image to bitmap");
            Bitmap bitmap = convertToBitmap(imageData, width, height);
            InputImage image = InputImage.fromBitmap(bitmap, rotation);

            MLKitLogger.v(TAG, "Starting face detection");
            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        MLKitLogger.d(TAG, "Face detection successful, found " + faces.size() + " faces");
                        byte[] result = formatFaceResultsBinary(faces);
                        String base64Result = android.util.Base64.encodeToString(result, android.util.Base64.DEFAULT);
                        callback.onResult("BINARY:" + base64Result);
                    })
                    .addOnFailureListener(e -> {
                        MLKitLogger.e(TAG, "Face detection failed", e);
                        callback.onResult("ERROR:" + e.getMessage());
                    });
        } catch (Exception e) {
            MLKitLogger.e(TAG, "Error processing image", e);
            callback.onResult("ERROR:" + e.getMessage());
        }
    }

    /**
     * Convert YUV image data to Bitmap
     */
    private Bitmap convertToBitmap(byte[] imageData, int width, int height) {
        YuvImage yuv = new YuvImage(imageData, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    /**
     * Format face detection results as binary data
     * Format:
     * [int: face count]
     * For each face:
     *   [int: tracking ID] (or -1 if not tracked)
     *   [float: bounds left, top, width, height]
     *   [float: head angles - X, Y, Z]
     *   [float: probabilities - smile, left eye open, right eye open]
     *   [int: landmark count]
     *   For each landmark:
     *     [int: landmark type]
     *     [float: x, y coordinates]
     *   [int: contour count]
     *   For each contour:
     *     [int: contour type]
     *     [int: point count]
     *     [float: all contour points x,y]
     */
    private byte[] formatFaceResultsBinary(List<Face> faces) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(4); // Temporary buffer for integer/float conversion
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        try {
            MLKitLogger.v(TAG, "Formatting binary results for " + faces.size() + " faces");

            // Write face count
            buffer.putInt(0, faces.size());
            baos.write(buffer.array(), 0, 4);

            for (Face face : faces) {
                // Write tracking ID (or -1 if not tracked)
                Integer trackingId = face.getTrackingId();
                buffer.putInt(0, trackingId != null ? trackingId : -1);
                baos.write(buffer.array(), 0, 4);

                // Write bounds (left, top, width, height)
                Rect bounds = face.getBoundingBox();
                buffer.putFloat(0, bounds.left);
                baos.write(buffer.array(), 0, 4);
                buffer.putFloat(0, bounds.top);
                baos.write(buffer.array(), 0, 4);
                buffer.putFloat(0, bounds.width());
                baos.write(buffer.array(), 0, 4);
                buffer.putFloat(0, bounds.height());
                baos.write(buffer.array(), 0, 4);

                // Write head Euler angles
                Float headEulerAngleX = face.getHeadEulerAngleX();
                buffer.putFloat(0, headEulerAngleX != null ? headEulerAngleX : 0.0f);
                baos.write(buffer.array(), 0, 4);

                Float headEulerAngleY = face.getHeadEulerAngleY();
                buffer.putFloat(0, headEulerAngleY != null ? headEulerAngleY : 0.0f);
                baos.write(buffer.array(), 0, 4);

                Float headEulerAngleZ = face.getHeadEulerAngleZ();
                buffer.putFloat(0, headEulerAngleZ != null ? headEulerAngleZ : 0.0f);
                baos.write(buffer.array(), 0, 4);

                // Write classification probabilities
                Float smileProbability = face.getSmilingProbability();
                buffer.putFloat(0, smileProbability != null ? smileProbability : 0.0f);
                baos.write(buffer.array(), 0, 4);

                Float leftEyeOpenProbability = face.getLeftEyeOpenProbability();
                buffer.putFloat(0, leftEyeOpenProbability != null ? leftEyeOpenProbability : 0.0f);
                baos.write(buffer.array(), 0, 4);

                Float rightEyeOpenProbability = face.getRightEyeOpenProbability();
                buffer.putFloat(0, rightEyeOpenProbability != null ? rightEyeOpenProbability : 0.0f);
                baos.write(buffer.array(), 0, 4);

                // Write landmarks
                int landmarkCount = 0;
                ByteArrayOutputStream landmarksBuffer = new ByteArrayOutputStream();
                ByteBuffer landmarkTypeBuffer = ByteBuffer.allocate(4);
                landmarkTypeBuffer.order(ByteOrder.LITTLE_ENDIAN);

                // Count and write all available landmarks
                if (face.getLandmark(FaceLandmark.LEFT_EYE) != null) {
                    landmarkCount++;
                    writeLandmark(landmarksBuffer, landmarkTypeBuffer, FaceLandmark.LEFT_EYE, face.getLandmark(FaceLandmark.LEFT_EYE));
                }
                if (face.getLandmark(FaceLandmark.RIGHT_EYE) != null) {
                    landmarkCount++;
                    writeLandmark(landmarksBuffer, landmarkTypeBuffer, FaceLandmark.RIGHT_EYE, face.getLandmark(FaceLandmark.RIGHT_EYE));
                }
                if (face.getLandmark(FaceLandmark.LEFT_EAR) != null) {
                    landmarkCount++;
                    writeLandmark(landmarksBuffer, landmarkTypeBuffer, FaceLandmark.LEFT_EAR, face.getLandmark(FaceLandmark.LEFT_EAR));
                }
                if (face.getLandmark(FaceLandmark.RIGHT_EAR) != null) {
                    landmarkCount++;
                    writeLandmark(landmarksBuffer, landmarkTypeBuffer, FaceLandmark.RIGHT_EAR, face.getLandmark(FaceLandmark.RIGHT_EAR));
                }
                if (face.getLandmark(FaceLandmark.LEFT_CHEEK) != null) {
                    landmarkCount++;
                    writeLandmark(landmarksBuffer, landmarkTypeBuffer, FaceLandmark.LEFT_CHEEK, face.getLandmark(FaceLandmark.LEFT_CHEEK));
                }
                if (face.getLandmark(FaceLandmark.RIGHT_CHEEK) != null) {
                    landmarkCount++;
                    writeLandmark(landmarksBuffer, landmarkTypeBuffer, FaceLandmark.RIGHT_CHEEK, face.getLandmark(FaceLandmark.RIGHT_CHEEK));
                }
                if (face.getLandmark(FaceLandmark.NOSE_BASE) != null) {
                    landmarkCount++;
                    writeLandmark(landmarksBuffer, landmarkTypeBuffer, FaceLandmark.NOSE_BASE, face.getLandmark(FaceLandmark.NOSE_BASE));
                }
                if (face.getLandmark(FaceLandmark.MOUTH_LEFT) != null) {
                    landmarkCount++;
                    writeLandmark(landmarksBuffer, landmarkTypeBuffer, FaceLandmark.MOUTH_LEFT, face.getLandmark(FaceLandmark.MOUTH_LEFT));
                }
                if (face.getLandmark(FaceLandmark.MOUTH_RIGHT) != null) {
                    landmarkCount++;
                    writeLandmark(landmarksBuffer, landmarkTypeBuffer, FaceLandmark.MOUTH_RIGHT, face.getLandmark(FaceLandmark.MOUTH_RIGHT));
                }
                if (face.getLandmark(FaceLandmark.MOUTH_BOTTOM) != null) {
                    landmarkCount++;
                    writeLandmark(landmarksBuffer, landmarkTypeBuffer, FaceLandmark.MOUTH_BOTTOM, face.getLandmark(FaceLandmark.MOUTH_BOTTOM));
                }

                // Write landmark count
                buffer.putInt(0, landmarkCount);
                baos.write(buffer.array(), 0, 4);

                // Write landmarks data
                baos.write(landmarksBuffer.toByteArray());

                // Write contours
                int contourCount = 0;
                ByteArrayOutputStream contoursBuffer = new ByteArrayOutputStream();
                ByteBuffer contourTypeBuffer = ByteBuffer.allocate(4);
                contourTypeBuffer.order(ByteOrder.LITTLE_ENDIAN);

                // Check and write all possible contours
                if (face.getContour(FaceContour.FACE) != null) {
                    contourCount++;
                    writeContour(contoursBuffer, contourTypeBuffer, FaceContour.FACE, face.getContour(FaceContour.FACE));
                }
                if (face.getContour(FaceContour.LEFT_EYEBROW_TOP) != null) {
                    contourCount++;
                    writeContour(contoursBuffer, contourTypeBuffer, FaceContour.LEFT_EYEBROW_TOP, face.getContour(FaceContour.LEFT_EYEBROW_TOP));
                }
                if (face.getContour(FaceContour.LEFT_EYEBROW_BOTTOM) != null) {
                    contourCount++;
                    writeContour(contoursBuffer, contourTypeBuffer, FaceContour.LEFT_EYEBROW_BOTTOM, face.getContour(FaceContour.LEFT_EYEBROW_BOTTOM));
                }
                if (face.getContour(FaceContour.RIGHT_EYEBROW_TOP) != null) {
                    contourCount++;
                    writeContour(contoursBuffer, contourTypeBuffer, FaceContour.RIGHT_EYEBROW_TOP, face.getContour(FaceContour.RIGHT_EYEBROW_TOP));
                }
                if (face.getContour(FaceContour.RIGHT_EYEBROW_BOTTOM) != null) {
                    contourCount++;
                    writeContour(contoursBuffer, contourTypeBuffer, FaceContour.RIGHT_EYEBROW_BOTTOM, face.getContour(FaceContour.RIGHT_EYEBROW_BOTTOM));
                }
                if (face.getContour(FaceContour.LEFT_EYE) != null) {
                    contourCount++;
                    writeContour(contoursBuffer, contourTypeBuffer, FaceContour.LEFT_EYE, face.getContour(FaceContour.LEFT_EYE));
                }
                if (face.getContour(FaceContour.RIGHT_EYE) != null) {
                    contourCount++;
                    writeContour(contoursBuffer, contourTypeBuffer, FaceContour.RIGHT_EYE, face.getContour(FaceContour.RIGHT_EYE));
                }
                if (face.getContour(FaceContour.UPPER_LIP_TOP) != null) {
                    contourCount++;
                    writeContour(contoursBuffer, contourTypeBuffer, FaceContour.UPPER_LIP_TOP, face.getContour(FaceContour.UPPER_LIP_TOP));
                }
                if (face.getContour(FaceContour.UPPER_LIP_BOTTOM) != null) {
                    contourCount++;
                    writeContour(contoursBuffer, contourTypeBuffer, FaceContour.UPPER_LIP_BOTTOM, face.getContour(FaceContour.UPPER_LIP_BOTTOM));
                }
                if (face.getContour(FaceContour.LOWER_LIP_TOP) != null) {
                    contourCount++;
                    writeContour(contoursBuffer, contourTypeBuffer, FaceContour.LOWER_LIP_TOP, face.getContour(FaceContour.LOWER_LIP_TOP));
                }
                if (face.getContour(FaceContour.LOWER_LIP_BOTTOM) != null) {
                    contourCount++;
                    writeContour(contoursBuffer, contourTypeBuffer, FaceContour.LOWER_LIP_BOTTOM, face.getContour(FaceContour.LOWER_LIP_BOTTOM));
                }
                if (face.getContour(FaceContour.NOSE_BRIDGE) != null) {
                    contourCount++;
                    writeContour(contoursBuffer, contourTypeBuffer, FaceContour.NOSE_BRIDGE, face.getContour(FaceContour.NOSE_BRIDGE));
                }
                if (face.getContour(FaceContour.NOSE_BOTTOM) != null) {
                    contourCount++;
                    writeContour(contoursBuffer, contourTypeBuffer, FaceContour.NOSE_BOTTOM, face.getContour(FaceContour.NOSE_BOTTOM));
                }

                // Write contour count
                buffer.putInt(0, contourCount);
                baos.write(buffer.array(), 0, 4);

                // Write contours data
                baos.write(contoursBuffer.toByteArray());
            }

            MLKitLogger.v(TAG, "Binary face results formatted, size: " + baos.size() + " bytes");
            return baos.toByteArray();
        } catch (Exception e) {
            MLKitLogger.e(TAG, "Error formatting binary face results", e);
            return new byte[0];
        }
    }

    /**
     * Write a landmark to the buffer
     */
    private void writeLandmark(ByteArrayOutputStream buffer, ByteBuffer typeBuffer, int landmarkType, FaceLandmark landmark) throws Exception {
        // Write landmark type
        typeBuffer.putInt(0, landmarkType);
        buffer.write(typeBuffer.array(), 0, 4);

        // Write position
        PointF position = landmark.getPosition();
        typeBuffer.putFloat(0, position.x);
        buffer.write(typeBuffer.array(), 0, 4);
        typeBuffer.putFloat(0, position.y);
        buffer.write(typeBuffer.array(), 0, 4);
    }

    /**
     * Write a contour to the buffer
     */
    private void writeContour(ByteArrayOutputStream buffer, ByteBuffer typeBuffer, int contourType, FaceContour contour) throws Exception {
        // Write contour type
        typeBuffer.putInt(0, contourType);
        buffer.write(typeBuffer.array(), 0, 4);

        // Write point count
        List<PointF> points = contour.getPoints();
        typeBuffer.putInt(0, points.size());
        buffer.write(typeBuffer.array(), 0, 4);

        // Write all points
        for (PointF point : points) {
            typeBuffer.putFloat(0, point.x);
            buffer.write(typeBuffer.array(), 0, 4);
            typeBuffer.putFloat(0, point.y);
            buffer.write(typeBuffer.array(), 0, 4);
        }
    }
}