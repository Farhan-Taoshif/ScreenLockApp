package com.screenlock.app;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;
    private static final int NOTIFICATION_PERMISSION_REQ_CODE = 5678;

    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startBtn = findViewById(R.id.btn_start);
        Button stopBtn = findViewById(R.id.btn_stop);
        statusText = findViewById(R.id.tv_status);

        updateStatus();

        startBtn.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    statusText.setText("⚠️ Overlay permission দিন — 'Screen Lock' খুঁজে Allow করুন");
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
                    return;
                }
            }
            // Android 13+ notification permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                            new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_REQ_CODE);
                    return;
                }
            }
            startLockService();
        });

        stopBtn.setOnClickListener(v -> {
            stopService(new Intent(this, FloatingLockService.class));
            statusText.setText("❌ Lock Button বন্ধ করা হয়েছে");
            Toast.makeText(this, "Lock service বন্ধ হয়েছে", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateStatus() {
        if (isCharging()) {
            statusText.setText("🔌 ফোন চার্জে আছে — Start দিলে Auto Lock চালু হবে!");
        } else {
            statusText.setText("🔋 ফোন চার্জে নেই\nচার্জ দিলে Auto Lock চালু হবে, খুললে Auto Unlock।");
        }
    }

    private boolean isCharging() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (batteryStatus == null) return false;
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL;
    }

    private void startLockService() {
        Intent serviceIntent = new Intent(this, FloatingLockService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        statusText.setText("✅ Lock Button চালু!\n\n" +
                "• চার্জে দিলে → Auto Lock 🔒\n" +
                "• চার্জ খুললে → Auto Unlock 🔓\n" +
                "• Lock icon চেপে ধরে drag করা যাবে");
        Toast.makeText(this, "🔒 Floating lock button চালু হয়েছে!", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                // Check notification permission next
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(
                                new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                                NOTIFICATION_PERMISSION_REQ_CODE);
                        return;
                    }
                }
                startLockService();
            } else {
                Toast.makeText(this, "⚠️ Overlay Permission ছাড়া কাজ করবে না!", Toast.LENGTH_LONG).show();
                statusText.setText("❌ Permission দেননি। আবার Start দিন।");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == NOTIFICATION_PERMISSION_REQ_CODE) {
            // Permission দিক বা না দিক, service চালু করবো
            startLockService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
