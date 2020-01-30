package com.example.falldetection;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class FallMonitoringSerivce extends Service {

    PowerManager.WakeLock wakeLock = null;
    boolean isRunning = false;

    static int state = 0;

    public FallMonitoringSerivce() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            String action = intent.getAction();
            Log.d("Action", action);
            if(action.equals("start")) {
                startService(intent);
            } else if(action.equals("stop")) {
                stopService(intent);
            } else {
                Log.e("Error", "Something wrong !");
            }
        }
        return START_STICKY_COMPATIBILITY;
    }

    public Notification createNotification() {
        String channelID = getString(R.string.channel_id);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.notification_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, FallMonitorService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        return new NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.drawable.example_picture)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getText(R.string.notification_description))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true).build();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Notification notification = createNotification();
        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show();
    }

    public void startService() {
        if(isRunning) return;
        Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show();
        isRunning = true;
        setServiceState(getApplicationContext(),1);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FallMonitoring::FallTag");
        wakeLock.acquire();
    }


    public void stopService() {
        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show();
        if(wakeLock.isHeld()) {
            wakeLock.release();
        }
        stopForeground(true);
        stopSelf();
        isRunning = false;
        setServiceState(getApplicationContext(),0);
    }

    public void setServiceState(Context context, int serviceState) {
        Log.d("state in service", serviceState + "");
        state = serviceState;
    }

}
