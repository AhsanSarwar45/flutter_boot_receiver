package com.flux.flutter_boot_receiver;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class BootHandlerService {
  private static final String TAG = "BootHandlerService";
  protected static final String SHARED_PREFERENCES_KEY = "com.flux.flutter_boot_receiver";

  private static final List<Intent> mQueue = Collections.synchronizedList(new LinkedList<>());
  private static FlutterBackgroundExecutor mFlutterBackgroundExecutor;
  private static volatile boolean mIsInitialized = false;

  /**
   * Called when the Dart isolate is ready to handle callbacks.
   */
  static void onInitialized() {
    Log.i(TAG, "BootHandlerService started!");
    mIsInitialized = true;

    synchronized (mQueue) {
      for (Intent intent : mQueue) {
        executeDartCallback(intent, null);
      }
      mQueue.clear();
    }
  }

  /**
   * Processes an intent with the background Dart isolate.
   */
  public static void processIntent(Context context, Intent intent) {
    if (mFlutterBackgroundExecutor == null) {
      mFlutterBackgroundExecutor = new FlutterBackgroundExecutor();
      mFlutterBackgroundExecutor.startBackgroundIsolate(context);
    }

    synchronized (mQueue) {
      if (!mIsInitialized || !mFlutterBackgroundExecutor.isRunning()) {
        Log.i(TAG, "BootHandlerService has not yet started. Queuing intent.");
        mQueue.add(intent);
        return;
      }
    }

    Log.i(TAG, "Handling Boot Complete...");
    executeDartCallback(intent, null);
  }

  private static void executeDartCallback(Intent intent, CountDownLatch latch) {
    if (mFlutterBackgroundExecutor == null) {
      Log.e(TAG, "FlutterBackgroundExecutor not initialized");
      if (latch != null) {
        latch.countDown();
      }
      return;
    }

    new Handler(Looper.getMainLooper()).post(() -> {
      mFlutterBackgroundExecutor.executeDartCallbackInBackgroundIsolate(intent, latch);
    });
  }

  /**
   * Starts the background isolate for the {@link BootHandlerService}.
   */
  public static void startBackgroundIsolate(Context context, long callbackDispatcherHandle) {
    if (mFlutterBackgroundExecutor != null) {
      Log.w(TAG, "Attempted to start a duplicate background isolate. Returning...");
      return;
    }
    mFlutterBackgroundExecutor = new FlutterBackgroundExecutor();
    mFlutterBackgroundExecutor.startBackgroundIsolate(context, callbackDispatcherHandle);
  }

  /**
   * Sets the Dart callback handle for the Dart method that is responsible for
   * initializing the background Dart isolate.
   */
  public static void setCallbackHandles(Context context, long callbackDispatcherHandle, long callbackHandle) {
    FlutterBackgroundExecutor.setCallbackHandles(context, callbackDispatcherHandle, callbackHandle);
  }

  /**
   * Check if the service is ready to handle callbacks.
   */
  public static boolean isInitialized() {
    return mIsInitialized && mFlutterBackgroundExecutor != null && mFlutterBackgroundExecutor.isRunning();
  }

  /**
   * Clean up resources when no longer needed.
   */
  public static void shutdown() {
    synchronized (mQueue) {
      mQueue.clear();
    }
    mIsInitialized = false;
    mFlutterBackgroundExecutor = null;
  }
}