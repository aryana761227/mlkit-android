package com.medrick.mlkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.facemesh.FaceMesh;
import com.google.mlkit.vision.facemesh.FaceMeshDetection;
import com.google.mlkit.vision.facemesh.FaceMeshDetector;
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions;
import com.google.mlkit.vision.facemesh.FaceMeshPoint;
import com.google.mlkit.vision.common.Triangle;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Handles face mesh detection using Google ML Kit
 */
public class MLKitFaceMeshDetector {
    private static final String TAG = "MLKitFaceMeshDetector";
    private FaceMeshDetector detector;
    private Context context;
    private boolean useHighAccuracyMode = false;

    /**
     * Constructor with context
     * @param context Android context
     */
    public MLKitFaceMeshDetector(Context context) {
        this.context = context;
    }

    /**
     * Initialize the face mesh detector with current options
     */
    public void initializeFaceMeshDetector() {
        FaceMeshDetectorOptions.Builder optionsBuilder = new FaceMeshDetectorOptions.Builder();

        // Based on your ML Kit version, we use the available options
        try {
            if (useHighAccuracyMode) {
                // High accuracy mode - Full mesh
                optionsBuilder.setUseCase(FaceMeshDetectorOptions.FACE_MESH);
                MLKitLogger.d(TAG, "Setting use case to FACE_MESH (high accuracy)");
            } else {
                // Basic mode - only bounding box
                optionsBuilder.setUseCase(FaceMeshDetectorOptions.BOUNDING_BOX_ONLY);
                MLKitLogger.d(TAG, "Setting use case to BOUNDING_BOX_ONLY (faster)");
            }
        } catch (Exception e) {
            MLKitLogger.e(TAG, "Error configuring face mesh detector", e);
        }

        FaceMeshDetectorOptions options = optionsBuilder.build();
        detector = FaceMeshDetection.getClient(options);
        MLKitLogger.i(TAG, "Face mesh detector initialized with high accuracy mode: " + useHighAccuracyMode);
    }

    /**
     * Configure the face mesh detector
     * @param useHighAccuracy Whether to use high accuracy mode
     */
    public void configureFaceMeshDetector(boolean useHighAccuracy) {
        this.useHighAccuracyMode = useHighAccuracy;
        MLKitLogger.i(TAG, "Face mesh detector configured with high accuracy mode: " + useHighAccuracy);
    }

    /**
     * Detect face mesh in an image
     * @param imageData Raw image data in YUV format
     * @param width Image width
     * @param height Image height
     * @param rotation Image rotation (0, 90, 180, 270)
     * @param callback Callback to receive detection results
     */
    public void detectFaceMesh(byte[] imageData, int width, int height, int rotation, MLKitResultCallback callback) {
        try {
            MLKitLogger.v(TAG, "Converting image to bitmap");
            Bitmap bitmap = convertToBitmap(imageData, width, height);
            InputImage image = InputImage.fromBitmap(bitmap, rotation);

            MLKitLogger.v(TAG, "Starting face mesh detection");
            detector.process(image)
                    .addOnSuccessListener(faceMeshes -> {
                        MLKitLogger.d(TAG, "Face mesh detection successful, found " + faceMeshes.size() + " meshes");
                        byte[] result = formatFaceMeshResultsBinary(faceMeshes);
                        String base64Result = android.util.Base64.encodeToString(result, android.util.Base64.DEFAULT);
                        callback.onResult("BINARY:" + base64Result);
                    })
                    .addOnFailureListener(e -> {
                        MLKitLogger.e(TAG, "Face mesh detection failed", e);
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
     * Format face mesh detection results as binary data
     * Format:
     * [int: mesh count]
     * For each mesh:
     *   [int: mesh ID]
     *   [float: bounds left, top, width, height]
     *   [int: point count]
     *   [float: all points x,y,z]
     *   [int: triangle count]
     *   [int: all triangle indices]
     *   [int: contour count]
     *   For each contour:
     *     [int: contour type]
     *     [int: contour point count]
     *     [int: contour point indices]
     */
    private byte[] formatFaceMeshResultsBinary(List<FaceMesh> faceMeshes) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(4); // Temporary buffer for integer/float conversion
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        try {
            MLKitLogger.v(TAG, "Formatting binary results for " + faceMeshes.size() + " face meshes");

            // Write mesh count
            buffer.putInt(0, faceMeshes.size());
            baos.write(buffer.array(), 0, 4);

            for (int i = 0; i < faceMeshes.size(); i++) {
                FaceMesh faceMesh = faceMeshes.get(i);

                // Write mesh ID
                buffer.putInt(0, i);
                baos.write(buffer.array(), 0, 4);

                // Write bounds (left, top, width, height)
                Rect bounds = faceMesh.getBoundingBox();
                buffer.putFloat(0, bounds.left);
                baos.write(buffer.array(), 0, 4);
                buffer.putFloat(0, bounds.top);
                baos.write(buffer.array(), 0, 4);
                buffer.putFloat(0, bounds.width());
                baos.write(buffer.array(), 0, 4);
                buffer.putFloat(0, bounds.height());
                baos.write(buffer.array(), 0, 4);

                // Write points
                List<FaceMeshPoint> points = faceMesh.getAllPoints();

                // Write point count
                buffer.putInt(0, points.size());
                baos.write(buffer.array(), 0, 4);

                MLKitLogger.v(TAG, "Writing " + points.size() + " points for mesh " + i);

                // Write all points
                for (FaceMeshPoint point : points) {
                    PointF3D position = point.getPosition();
                    buffer.putFloat(0, position.getX());
                    baos.write(buffer.array(), 0, 4);
                    buffer.putFloat(0, position.getY());
                    baos.write(buffer.array(), 0, 4);
                    buffer.putFloat(0, position.getZ());
                    baos.write(buffer.array(), 0, 4);
                }

                // Write triangles
                List<Triangle<FaceMeshPoint>> triangles = faceMesh.getAllTriangles();

                // Write triangle count
                buffer.putInt(0, triangles.size());
                baos.write(buffer.array(), 0, 4);

                MLKitLogger.v(TAG, "Writing " + triangles.size() + " triangles for mesh " + i);

                // Write all triangles (indices into points array)
                for (Triangle<FaceMeshPoint> triangle : triangles) {
                    // Get triangle points using the getAllPoints() method from the Triangle class
                    List<FaceMeshPoint> trianglePoints = triangle.getAllPoints();

                    if (trianglePoints.size() >= 3) {
                        int idx1 = points.indexOf(trianglePoints.get(0));
                        int idx2 = points.indexOf(trianglePoints.get(1));
                        int idx3 = points.indexOf(trianglePoints.get(2));

                        buffer.putInt(0, idx1);
                        baos.write(buffer.array(), 0, 4);
                        buffer.putInt(0, idx2);
                        baos.write(buffer.array(), 0, 4);
                        buffer.putInt(0, idx3);
                        baos.write(buffer.array(), 0, 4);
                    }
                }

                // Collect contours
                int contourCount = 0;
                ByteArrayOutputStream contoursBuffer = new ByteArrayOutputStream();
                ByteBuffer contourTypeBuffer = ByteBuffer.allocate(4);
                contourTypeBuffer.order(ByteOrder.LITTLE_ENDIAN);

                for (int contourType = 1; contourType <= 12; contourType++) {
                    List<FaceMeshPoint> contourPoints = faceMesh.getPoints(contourType);
                    if (!contourPoints.isEmpty()) {
                        contourCount++;

                        // Write contour type
                        contourTypeBuffer.putInt(0, contourType);
                        contoursBuffer.write(contourTypeBuffer.array(), 0, 4);

                        // Write contour point count
                        contourTypeBuffer.putInt(0, contourPoints.size());
                        contoursBuffer.write(contourTypeBuffer.array(), 0, 4);

                        // Write indices of contour points
                        for (FaceMeshPoint contourPoint : contourPoints) {
                            int pointIndex = points.indexOf(contourPoint);
                            contourTypeBuffer.putInt(0, pointIndex);
                            contoursBuffer.write(contourTypeBuffer.array(), 0, 4);
                        }
                    }
                }

                // Write contour count
                buffer.putInt(0, contourCount);
                baos.write(buffer.array(), 0, 4);

                MLKitLogger.v(TAG, "Writing " + contourCount + " contours for mesh " + i);

                // Write contours data
                baos.write(contoursBuffer.toByteArray());
            }

            MLKitLogger.v(TAG, "Binary face mesh results formatted, size: " + baos.size() + " bytes");
            return baos.toByteArray();
        } catch (Exception e) {
            MLKitLogger.e(TAG, "Error formatting binary face mesh results", e);
            return new byte[0];
        }
    }
}