package com.flux.flutter_boot_receiver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;
import io.flutter.FlutterInjector;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.embedding.engine.dart.DartExecutor.DartCallback;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.JSONMethodCodec;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.view.FlutterCallbackInformation;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An background execution abstraction which handles initializing a background
 * isolate running a callback dispatcher, used to invoke Dart callbacks while backgrounded.
 */
public class FlutterBackgroundExecutor implements MethodCallHandler {
    private static final String TAG = "FlutterBackgroundExecutor";
    public static final String CALLBACK_DISPATCHER_HANDLE_KEY = "callback_dispatcher_handle";
    public static final String CALLBACK_HANDLE_KEY = "callback_handle";

    private MethodChannel backgroundChannel;
    private FlutterEngine backgroundFlutterEngine;
    private final AtomicBoolean isCallbackDispatcherReady = new AtomicBoolean(false);
    private Context storedContext;

    /**
     * Sets the Dart callback handle for the Dart method that is responsible for
     * initializing the background Dart isolate, preparing it to receive Dart callback tasks
     * requests.
     */
    public static void setCallbackHandles(Context context, long callbackDispatcherHandle, long callbackHandle) {
        SharedPreferences preferences = context.getSharedPreferences(BootHandlerService.SHARED_PREFERENCES_KEY, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(CALLBACK_DISPATCHER_HANDLE_KEY, callbackDispatcherHandle);
        editor.putLong(CALLBACK_HANDLE_KEY, callbackHandle);
        editor.apply();
    }

    /**
     * Returns true when the background isolate has started and is ready to handle
     * dart callbacks.
     */
    public boolean isRunning() {
        return isCallbackDispatcherReady.get();
    }

    private void onInitialized() {
        isCallbackDispatcherReady.set(true);
        BootHandlerService.onInitialized();
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        String method = call.method;
        try {
            if (method.equals("BootHandlerService.initialized")) {
                onInitialized();
                result.success(true);
            } else {
                result.notImplemented();
            }
        } catch (Exception error) {
            result.error("error", "FlutterBootReceiver error: " + error.getMessage(), null);
        }
    }

    /**
     * Starts running a background Dart isolate within a new {@link FlutterEngine}
     * using a previously used entrypoint.
     */
    public void startBackgroundIsolate(Context context) {
        if (backgroundFlutterEngine != null) {
            Log.e(TAG, "Background isolate already started");
            return;
        }

        Log.i(TAG, "Starting BootHandlerService...");
        if (!isRunning()) {
            this.storedContext = context;
            backgroundFlutterEngine = new FlutterEngine(context);

            SharedPreferences preferences = context.getSharedPreferences(BootHandlerService.SHARED_PREFERENCES_KEY, 0);
            long callbackDispatcherHandle = preferences.getLong(CALLBACK_DISPATCHER_HANDLE_KEY, 0);
            startBackgroundIsolate(context, callbackDispatcherHandle);
        }
    }

    /**
     * Starts running a background Dart isolate within a new {@link FlutterEngine}.
     */
    public void startBackgroundIsolate(Context context, long callbackHandle) {
        if (backgroundFlutterEngine != null) {
            Log.e(TAG, "Background isolate already started");
            return;
        }

        Log.i(TAG, "Starting BootHandlerService...");
        if (!isRunning()) {
            this.storedContext = context; // Store context here too
            backgroundFlutterEngine = new FlutterEngine(context);

            String appBundlePath = FlutterInjector.instance().flutterLoader().findAppBundlePath();
            AssetManager assets = context.getAssets();

            FlutterCallbackInformation flutterCallback = FlutterCallbackInformation
                    .lookupCallbackInformation(callbackHandle);
            if (flutterCallback == null) {
                Log.e(TAG, "Fatal: failed to find callback");
                return;
            }

            DartExecutor executor = backgroundFlutterEngine.getDartExecutor();
            initializeMethodChannel(executor);
            DartCallback dartCallback = new DartCallback(assets, appBundlePath, flutterCallback);

            executor.executeDartCallback(dartCallback);

            Log.i(TAG, "Background isolate started successfully");
        } else {
            Log.e(TAG, "Background isolate already started");
        }
    }

    /**
     * Executes the desired Dart callback in a background Dart isolate.
     */
    public void executeDartCallbackInBackgroundIsolate(Intent intent, final CountDownLatch latch) {
        if (storedContext == null) {
            Log.e(TAG, "Context is null, cannot execute Dart callback");
            if (latch != null) {
                latch.countDown();
            }
            return;
        }

        SharedPreferences preferences = storedContext.getSharedPreferences(BootHandlerService.SHARED_PREFERENCES_KEY, 0);
        long callbackHandle = preferences.getLong(FlutterBackgroundExecutor.CALLBACK_HANDLE_KEY, 0);

        MethodChannel.Result result = null;
        if (latch != null) {
            result = new MethodChannel.Result() {
                @Override
                public void success(Object result) {
                    latch.countDown();
                }

                @Override
                public void error(String errorCode, String errorMessage, Object errorDetails) {
                    latch.countDown();
                }

                @Override
                public void notImplemented() {
                    latch.countDown();
                }
            };
        }

        Log.i(TAG, "Executing Dart callback: " + callbackHandle + "...");

        if (backgroundChannel != null) {
            backgroundChannel.invokeMethod("", new Object[] { callbackHandle }, result);
        } else {
            Log.e(TAG, "Background channel is null");
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    private void initializeMethodChannel(BinaryMessenger isolate) {
        backgroundChannel = new MethodChannel(
                isolate,
                "com.flux.flutter_boot_receiver/background",
                JSONMethodCodec.INSTANCE);
        backgroundChannel.setMethodCallHandler(this);
    }
}