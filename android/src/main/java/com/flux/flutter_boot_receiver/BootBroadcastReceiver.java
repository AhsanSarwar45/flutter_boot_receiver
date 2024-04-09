package com.flux.flutter_boot_receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class BootBroadcastReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") ||
        intent.getAction().equals(
            "android.intent.action.LOCKED_BOOT_COMPLETED") ||
        intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON") ||
        intent.getAction().equals("com.htc.intent.action.QUICKBOOT_POWERON")) {
      Log.i("BootBroadcastReceiver", "Boot Completed");
      BootHandlerService.enqueueOnReceiveProcessing(context, intent);
    }
  }
}
