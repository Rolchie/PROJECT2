package com.maramagagriculturalaid.app.Logs;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventLogger {
    private static final String TAG = "EventLogger";
    private static final String LOG_FILE = "app_events.log";
    private static final List<String> inMemoryLogs = new ArrayList<>();
    private static Context appContext;

    // Initialize with application context
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    // Log user action
    public static void logUserAction(String username, String action) {
        String message = String.format("USER ACTION: User '%s' performed action: %s", username, action);
        Log.i(TAG, message);
        inMemoryLogs.add(formatLogEntry("INFO", message));
        writeToFile(formatLogEntry("INFO", message));
    }

    // Log error
    public static void logError(String errorMessage, Throwable exception) {
        String message = String.format("ERROR: %s", errorMessage);
        if (exception != null) {
            Log.e(TAG, message, exception);
            inMemoryLogs.add(formatLogEntry("ERROR", message + " - " + exception.toString()));
            writeToFile(formatLogEntry("ERROR", message + " - " + exception.toString()));
        } else {
            Log.e(TAG, message);
            inMemoryLogs.add(formatLogEntry("ERROR", message));
            writeToFile(formatLogEntry("ERROR", message));
        }
    }

    // Log system event
    public static void logSystemEvent(String event) {
        String message = String.format("SYSTEM: %s", event);
        Log.i(TAG, message);
        inMemoryLogs.add(formatLogEntry("INFO", message));
        writeToFile(formatLogEntry("INFO", message));
    }

    // Format log entry with timestamp
    private static String formatLogEntry(String level, String message) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        return dateFormat.format(new Date()) + " [" + level + "] " + message;
    }

    // Write log to file
    private static synchronized void writeToFile(String logEntry) {
        if (appContext == null) {
            Log.e(TAG, "Context not initialized. Call init() first.");
            return;
        }

        try {
            File logFile = new File(appContext.getFilesDir(), LOG_FILE);
            if (!logFile.exists()) {
                logFile.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(logFile, true);
            fos.write((logEntry + "\n").getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file", e);
        }
    }

    // View logs in logcat
    public static void viewLogsInConsole() {
        Log.d(TAG, "===== APPLICATION LOGS =====");
        for (String log : inMemoryLogs) {
            Log.d(TAG, log);
        }
        Log.d(TAG, "===== END OF LOGS =====");
    }

    // Export logs to a custom file
    public static void exportLogs(String fileName) {
        if (appContext == null) {
            Log.e(TAG, "Context not initialized. Call init() first.");
            return;
        }

        try {
            File exportFile = new File(appContext.getFilesDir(), fileName);
            FileOutputStream fos = new FileOutputStream(exportFile);

            for (String log : inMemoryLogs) {
                fos.write((log + "\n").getBytes());
            }

            fos.close();
            Log.i(TAG, "Logs exported successfully to: " + exportFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to export logs", e);
        }
    }

    // Get the path to the log file
    public static String getLogFilePath() {
        if (appContext == null) {
            return null;
        }
        return new File(appContext.getFilesDir(), LOG_FILE).getAbsolutePath();
    }
}