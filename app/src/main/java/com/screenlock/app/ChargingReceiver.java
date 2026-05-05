package com.screenlock.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

/**
 * Manifest-registered receiver for charging events.
 * When app is NOT running, this starts the service.
 * When app IS running, FloatingLockService handles it internally.
 */
public class ChargingReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
            // Overlay permission check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(context)) {
                    return;
                }
            }
            // Service চালু করো
            Intent serviceIntent = new Intent(context, FloatingLockService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
        // POWER_DISCONNECTED এর কাজ FloatingLockService এর ভেতরে হয়
    }
}
