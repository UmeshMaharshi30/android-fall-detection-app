package com.example.falldetection;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class FallMonitorService extends Service implements SensorEventListener {

    private static RequestQueue queue;
    private static String BASE_URL = "http://desktop-smbkroj.student.iastate.edu:8080";
    private static String FALL_API_URL = "http://desktop-smbkroj.student.iastate.edu:5000/predict/android";
    private static  String FALL_TRIGGER_URL = BASE_URL + "/fall";

    private static String current_activity;
    private static int total_activity_readings;
    private static  int START_DELAY;

    private SensorManager mSensorManager;
    private Sensor mSensorGyro;
    private Sensor mSensorAcc;

    private double ACC_ABOVE_THRESHOLD = 25.0, ACC_BELOW_THRESHOLD = 7.0, GYRO_ABOVE_THRESHOLD = 3.5;

    private int acc_normal = 0, acc_above = 0, acc_below = 0, gyro_normal = 0, gyro_above = 0;

    private static long start_time = -1;
    private static long end_time = -1;

    public static final String ACTION_START = "START_MONITORING";
    public static final String ACTION_STOP = "STOP_MONITORING";

    private static boolean monitor = true;

    private static SensorManager sensorManager;
    private static Sensor gryoSensor;
    private static Sensor accSensor;

    private static PowerManager.WakeLock wakeLock;

    private NotificationManagerCompat notificationManager;

    public FallMonitorService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
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

    private void
    handleActionStop() {
        // TODO: Handle action Baz
        if(sensorManager == null) return;
        sensorManager.unregisterListener(this);
        sensorManager = null;
        notificationManager.cancel(1);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            Log.d("action", action);
            Log.d("status", monitor + "");
            notificationManager = NotificationManagerCompat.from(this);
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FallMonitoring::FallTag");
            queue = Volley.newRequestQueue(this);
            if (ACTION_START.equals(action)) {
                monitor = true;
                handleActionStart();
                wakeLock.acquire();
            } else if (ACTION_STOP.equals(action) && monitor) {
                monitor = false;
                handleActionStop();
                //stopForeground(true);
                stopSelf();
                if(wakeLock.isHeld()) wakeLock.release();            }
        }
        return START_STICKY;
    }

    public void reset() {
        start_time = -1;
        end_time = -1;
        gyro_above = 0;
        gyro_normal = 0;
        acc_below = 0;
        acc_normal = 0;
        acc_above = 0;
    }

    public String[] getReadings() {
        String[] readingCounts = new String[4];
        readingCounts[0] = gyro_above + "";
        readingCounts[1] = acc_below + "";
        readingCounts[2] = acc_above + "";
        readingCounts[3] = (end_time - start_time) + "";
        return readingCounts;
    }

    public String getState() {
        return gyro_normal + " " + gyro_above + " " + acc_below + " " + acc_normal + " " + acc_above + " " + (end_time - start_time);
    }

    private void connectToFallAPI(final String[] sensorData, final boolean avmFall) {
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        JSONObject data = new JSONObject();
        try {
            data.put("data", new JSONArray(Arrays.asList(sensorData)));
            data.put("avm", avmFall ? 1 : 0);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.POST, FALL_API_URL, data, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                Notification notification = createNotification("Prediction " + response.getDouble("prediction") + " AVM " + avmFall);
                                double prediction = response.getDouble("prediction");
                                if(prediction >= 0.5 || (avmFall && prediction >= 0.25)) {
                                    notificationManager.notify(1, notification);
                                }
                            } catch (JSONException ex) {
                                Log.d("Error while parsing", " response");
                            }
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO: Handle error
                            if(avmFall) {
                                Notification notification = createNotification("Only AVM Fall Detected !");
                                notificationManager.notify(1, notification);
                            }
                            if(error.getMessage() != null) Log.e("Error", error.getMessage());

                        }
                    });
            queue.add(jsonObjectRequest);
        } catch (JSONException e) {
            Log.e("Exception", "Creating");
        }

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        long diff = 0;
        if(end_time > 0) diff = System.currentTimeMillis() - end_time;
        if(diff > 500) {
            long delta = end_time - start_time;
            boolean avm = (gyro_above >= 20 && gyro_above < 200 && acc_below >= 65 && acc_above >= 15 && delta >= 500 && delta <= 1800);
            connectToFallAPI(getReadings(), avm);
            reset();
        }
        if(sensorEvent.values.length < 3) return;
        float x = sensorEvent.values[0], y = sensorEvent.values[1], z = sensorEvent.values[2];
        double net_acc = Math.sqrt(x * x + y * y + z * z);
        switch (sensorType) {
            case Sensor.TYPE_GYROSCOPE:
                if(net_acc >= GYRO_ABOVE_THRESHOLD) {
                    gyro_above++;
                    if(start_time == -1) {
                        start_time = System.currentTimeMillis();
                    }
                    end_time = System.currentTimeMillis();
                } else {
                    if(start_time > 0) gyro_normal++;
                }
                break;
            case Sensor.TYPE_ACCELEROMETER:
                if(net_acc < ACC_BELOW_THRESHOLD) {
                    acc_below++;
                    if(start_time == -1) {
                        start_time = System.currentTimeMillis();
                    }
                    end_time = System.currentTimeMillis();
                } else if(net_acc >= ACC_ABOVE_THRESHOLD) {
                    acc_above++;
                    if(start_time == -1) {
                        start_time = System.currentTimeMillis();
                    }
                    end_time = System.currentTimeMillis();
                } else {
                    if(start_time > 0) acc_normal++;
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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
}
