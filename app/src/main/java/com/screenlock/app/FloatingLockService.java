package com.screenlock.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

public class FloatingLockService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private View lockOverlayView;
    private View statusBarOverlayView; // notification panel block করার জন্য
    private boolean isLocked = false;

    private static final String CHANNEL_ID = "ScreenLockChannel";
    private static final int NOTIF_ID = 1;
    public static final String ACTION_STOP = "ACTION_STOP_SERVICE";

    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    // Charging state receiver
    private BroadcastReceiver chargingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                // চার্জ লাগানো হলে — lock করো
                if (!isLocked) {
                    ImageView lockIcon = floatingView != null ? floatingView.findViewById(R.id.iv_lock_icon) : null;
                    if (lockIcon != null) {
                        enableLock(lockIcon);
                    }
                }
            } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                // চার্জ খোলা হলে — unlock করো
                if (isLocked) {
                    ImageView lockIcon = floatingView != null ? floatingView.findViewById(R.id.iv_lock_icon) : null;
                    if (lockIcon != null) {
                        disableLock(lockIcon);
                        Toast.makeText(context, "🔓 চার্জ খোলা হয়েছে — Screen Unlock!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Register charging receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(chargingReceiver, filter);

        setupFloatingButton();

        // চার্জে থাকলে app চালু হওয়ার সাথে সাথে lock করো
        if (isCurrentlyCharging()) {
            ImageView lockIcon = floatingView.findViewById(R.id.iv_lock_icon);
            enableLock(lockIcon);
            Toast.makeText(this, "🔒 চার্জ দেওয়া আছে — Screen Lock চালু!", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isCurrentlyCharging() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (batteryStatus == null) return false;
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL;
    }

    private void setupFloatingButton() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_lock_button, null);

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        final WindowManager.LayoutParams floatParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        floatParams.gravity = Gravity.TOP | Gravity.LEFT;
        floatParams.x = 50;
        floatParams.y = 300;

        windowManager.addView(floatingView, floatParams);

        // ===== FULL SCREEN OVERLAY (touch blocker) =====
        lockOverlayView = new View(this);
        lockOverlayView.setBackgroundColor(Color.TRANSPARENT);

        final WindowManager.LayoutParams overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT
        );
        overlayParams.gravity = Gravity.TOP;
        lockOverlayView.setOnTouchListener((v, event) -> true);

        // ===== STATUS BAR OVERLAY (notification panel block) =====
        // Status bar এর উপর একটা얇은 invisible overlay বসাই
        // এটা swipe down gesture টা consume করে, ফলে notification panel নামতে পারে না
        statusBarOverlayView = new View(this);
        statusBarOverlayView.setBackgroundColor(Color.TRANSPARENT);

        // Status bar height বের করি
        int statusBarHeight = 80; // fallback dp
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        // একটু বেশি নিই যাতে swipe area পুরোপুরি cover হয়
        final int sbHeight = statusBarHeight * 3;

        final WindowManager.LayoutParams statusBarParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                sbHeight,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT
        );
        statusBarParams.gravity = Gravity.TOP;
        // Swipe gesture সম্পূর্ণ block করো
        statusBarOverlayView.setOnTouchListener((v, event) -> true);

        ImageView lockIcon = floatingView.findViewById(R.id.iv_lock_icon);

        // Drag support
        floatingView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = floatParams.x;
                    initialY = floatParams.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    floatParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                    floatParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(floatingView, floatParams);
                    return true;
                case MotionEvent.ACTION_UP:
                    int dx = (int) (event.getRawX() - initialTouchX);
                    int dy = (int) (event.getRawY() - initialTouchY);
                    if (Math.abs(dx) < 10 && Math.abs(dy) < 10) {
                        if (!isLocked) {
                            enableLock(lockIcon);
                        } else {
                            disableLock(lockIcon);
                        }
                    }
                    return true;
            }
            return false;
        });

        // Store params for later use
        floatingView.setTag(new Object[]{overlayParams, floatParams, statusBarParams});
    }

    private void enableLock(ImageView lockIcon) {
        if (floatingView == null || floatingView.getTag() == null) return;
        Object[] params = (Object[]) floatingView.getTag();
        WindowManager.LayoutParams overlayParams = (WindowManager.LayoutParams) params[0];
        WindowManager.LayoutParams floatParams = (WindowManager.LayoutParams) params[1];
        WindowManager.LayoutParams statusBarParams = (WindowManager.LayoutParams) params[2];

        isLocked = true;
        lockIcon.setImageResource(R.drawable.ic_lock_closed);
        floatParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        // Full screen overlay
        try {
            windowManager.addView(lockOverlayView, overlayParams);
        } catch (Exception ignored) {}

        // Status bar overlay — notification panel নামানো block করে
        try {
            windowManager.addView(statusBarOverlayView, statusBarParams);
        } catch (Exception ignored) {}

        // Floating button সবার উপরে থাকবে
        try { windowManager.removeView(floatingView); } catch (Exception ignored) {}
        try { windowManager.addView(floatingView, floatParams); } catch (Exception ignored) {}
    }

    private void disableLock(ImageView lockIcon) {
        isLocked = false;
        lockIcon.setImageResource(R.drawable.ic_lock_open);
        try { windowManager.removeView(lockOverlayView); } catch (Exception ignored) {}
        try { windowManager.removeView(statusBarOverlayView); } catch (Exception ignored) {}
    }

    private Notification buildNotification() {
        // Stop action for notification
        Intent stopIntent = new Intent(this, FloatingLockService.class);
        stopIntent.setAction(ACTION_STOP);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, flags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🔒 Screen Lock Active")
                .setContentText("চার্জে দিলে auto lock, খুললে auto unlock")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(android.R.drawable.ic_delete, "Stop", stopPendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Screen Lock Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("চার্জে দিলে screen lock করে, খুললে unlock করে");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(chargingReceiver); } catch (Exception ignored) {}
        if (floatingView != null) {
            try { windowManager.removeView(floatingView); } catch (Exception ignored) {}
        }
        if (lockOverlayView != null) {
            try { windowManager.removeView(lockOverlayView); } catch (Exception ignored) {}
        }
        if (statusBarOverlayView != null) {
            try { windowManager.removeView(statusBarOverlayView); } catch (Exception ignored) {}
        }
    }
}
