package com.example.flutter_boot_listener;

// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.json.JSONException;
import org.json.JSONObject;

public class BootHandlerService extends JobIntentService {
  private static final String TAG = "BootHandlerService";
  protected static final String SHARED_PREFERENCES_KEY = "com.example.flutter_boot_listener";
  private static final int JOB_ID = 1984; // Random job ID.

  private static final List<Intent> mQueue = Collections.synchronizedList(new LinkedList<>());

  private static FlutterBackgroundExecutor mFlutterBackgroundExecutor;

  private static Context mContext;

  public static void enqueueOnReceiveProcessing(Context context, Intent intent) {
    enqueueWork(context, BootHandlerService.class, JOB_ID, intent);
  }

  static void onInitialized() {
    Log.i(TAG, "BootHandlerService started!");
    synchronized (mQueue) {
      // Handle all the events received before the Dart isolate was
      // initialized, then clear the mQueue.
      for (Intent intent : mQueue) {
        mFlutterBackgroundExecutor.executeDartCallbackInBackgroundIsolate(mContext, intent, null);
      }
      mQueue.clear();
    }
  }

  /**
   * Starts the background isolate for the {@link BootHandlerService}.
   *
   * <p>
   * Preconditions:
   *
   * <ul>
   * <li>The given {@code callbackHandle} must correspond to a registered Dart
   * callback. If the
   * handle does not resolve to a Dart callback then this method does nothing.
   * <li>A static {@link #pluginRegistrantCallback} must exist, otherwise a {@link
   * PluginRegistrantException} will be thrown.
   * </ul>
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
   * initializing the
   * background Dart isolate, preparing it to receive Dart callback tasks
   * requests.
   */
  public static void setCallbackHandles(Context context, long callbackDispatcherHandle, long callbackHandle) {
    FlutterBackgroundExecutor.setCallbackHandles(context, callbackDispatcherHandle, callbackHandle);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    if (mFlutterBackgroundExecutor == null) {
      mFlutterBackgroundExecutor = new FlutterBackgroundExecutor();
    }
    mContext = getApplicationContext();
    mFlutterBackgroundExecutor.startBackgroundIsolate(mContext);
  }

  /**
   * Executes a Dart callback, as specified within the incoming {@code intent}.
   *
   * <p>
   * Invoked by our {@link JobIntentService} superclass after a call to {@link
   * JobIntentService#enmQueueWork(Context, Class, int, Intent);}.
   */
  @Override
  protected void onHandleWork(@NonNull final Intent intent) {
    // If we're in the middle of processing queued events, add the incoming
    // intent to the queue and return.
    synchronized (mQueue) {
      if (!mFlutterBackgroundExecutor.isRunning()) {
        Log.i(TAG, "BootHandlerService has not yet started.");
        mQueue.add(intent);
        return;
      }
    }

    Log.i(TAG, "Handling Boot Complete...");

    // There were no pre-existing callback requests. Execute the callback
    // specified by the incoming intent.
    final CountDownLatch latch = new CountDownLatch(1);
    new Handler(getMainLooper())
        .post(
            () -> mFlutterBackgroundExecutor.executeDartCallbackInBackgroundIsolate(mContext, intent, latch));

    try {
      latch.await();
    } catch (InterruptedException ex) {
      Log.i(TAG, "Exception waiting to execute Dart callback", ex);
    }
  }
}
