package com.medrick.mlkit;

import android.util.Log;

/**
 * Logger utility for ML Kit bridge.
 * Allows for control of logging verbosity across the SDK.
 */
public class MLKitLogger {
    // Log levels
    public static final int LOG_LEVEL_VERBOSE = 5;
    public static final int LOG_LEVEL_DEBUG = 4;
    public static final int LOG_LEVEL_INFO = 3;
    public static final int LOG_LEVEL_WARN = 2;
    public static final int LOG_LEVEL_ERROR = 1;
    public static final int LOG_LEVEL_NONE = 0;

    // Current log level - default to INFO
    private static int currentLogLevel = LOG_LEVEL_INFO;

    /**
     * Set the current log level for the SDK.
     * @param logLevel The log level to set (use LOG_LEVEL_* constants)
     */
    public static void setLogLevel(int logLevel) {
        if (logLevel >= LOG_LEVEL_NONE && logLevel <= LOG_LEVEL_VERBOSE) {
            currentLogLevel = logLevel;
        }
    }

    /**
     * Get the current log level.
     * @return The current log level
     */
    public static int getLogLevel() {
        return currentLogLevel;
    }

    /**
     * Log a verbose message.
     * @param tag The log tag
     * @param message The message to log
     */
    public static void v(String tag, String message) {
        if (currentLogLevel >= LOG_LEVEL_VERBOSE) {
            Log.v(tag, message);
        }
    }

    /**
     * Log a debug message.
     * @param tag The log tag
     * @param message The message to log
     */
    public static void d(String tag, String message) {
        if (currentLogLevel >= LOG_LEVEL_DEBUG) {
            Log.d(tag, message);
        }
    }

    /**
     * Log an info message.
     * @param tag The log tag
     * @param message The message to log
     */
    public static void i(String tag, String message) {
        if (currentLogLevel >= LOG_LEVEL_INFO) {
            Log.i(tag, message);
        }
    }

    /**
     * Log a warning message.
     * @param tag The log tag
     * @param message The message to log
     */
    public static void w(String tag, String message) {
        if (currentLogLevel >= LOG_LEVEL_WARN) {
            Log.w(tag, message);
        }
    }

    /**
     * Log an error message.
     * @param tag The log tag
     * @param message The message to log
     */
    public static void e(String tag, String message) {
        if (currentLogLevel >= LOG_LEVEL_ERROR) {
            Log.e(tag, message);
        }
    }

    /**
     * Log an error message with an exception.
     * @param tag The log tag
     * @param message The message to log
     * @param throwable The exception to log
     */
    public static void e(String tag, String message, Throwable throwable) {
        if (currentLogLevel >= LOG_LEVEL_ERROR) {
            Log.e(tag, message, throwable);
        }
    }
}