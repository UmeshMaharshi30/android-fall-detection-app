package com.example.falldetection;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class MonitorFall extends IntentService implements SensorEventListener {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_START = "START_MONITORING";
    public static final String ACTION_STOP = "STOP_MONITORING";

    private static boolean monitor = true;

    private static SensorManager sensorManager;
    private static Sensor gryoSensor;
    private static Sensor accSensor;

    private static PowerManager.WakeLock wakeLock;

    private NotificationManagerCompat notificationManager;

    public MonitorFall() {
        super("MonitorFall");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo(Context context) {
        Intent intent = new Intent(context, MonitorFall.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            notificationManager = NotificationManagerCompat.from(this);
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FallMonitoring::FallTag");
            if (ACTION_START.equals(action)) {
                monitor = true;
                handleActionStart();
                if(!wakeLock.isHeld()) wakeLock.acquire();
            } else if (ACTION_STOP.equals(action)) {
                monitor = false;
                handleActionStop();
                if(wakeLock.isHeld()) wakeLock.release();            }
        }
    }

    private void handleActionStart() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        gryoSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if(gryoSensor != null) sensorManager.registerListener(this, gryoSensor, Sensor.REPORTING_MODE_CONTINUOUS);
        if(accSensor != null) sensorManager.registerListener(this, accSensor, Sensor.REPORTING_MODE_CONTINUOUS);

        Notification notification = createNotification(getString(R.string.notification_description));
        notificationManager.notify(1, notification);
        startForeground(1, notification);
    }

    /**
     * Handle action Stop in the provided background thread with the provided
     * parameters.
     */
    private void handleActionStop() {
        // TODO: Handle action Baz
        if(sensorManager == null) return;
        sensorManager.unregisterListener(this);
        sensorManager = null;
        stopForeground(true);
        notificationManager.cancel(1);
    }


    public Notification createNotification(String notificationText) {
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
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true).build();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(!monitor) return;
        Log.d("Sensor", "Monitoring");
    }
}
