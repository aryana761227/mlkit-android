package com.medrick.mlkit;

/**
 * Interface for callbacks from ML Kit operations
 */
public interface MLKitResultCallback {
    /**
     * Called when a result is available
     * @param result String result from the ML Kit operation
     *               For binary results, it will be in format "BINARY:[base64 encoded data]"
     *               For errors, it will be in format "ERROR:[error message]"
     */
    void onResult(String result);
}