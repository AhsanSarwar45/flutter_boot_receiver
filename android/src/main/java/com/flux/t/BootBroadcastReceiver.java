package com.flux.flutter_boot_receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import android.content.SharedPreferences;
import android.util.Log;

public class BootBroadcastReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
      Log.i("BootBroadcastReceiver", "Boot Completed");
      BootHandlerService.enqueueOnReceiveProcessing(context, intent);
    }
  }
}
