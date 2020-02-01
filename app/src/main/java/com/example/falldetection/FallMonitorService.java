package com.example.falldetection;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.xml.transform.sax.TransformerHandler;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class FallMonitorService extends IntentService implements SensorEventListener {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS

    private static RequestQueue queue;
    private static String BASE_URL = "http://desktop-smbkroj.student.iastate.edu:8080";
    private static String FALL_API_URL = "http://192.168.2.90:5000/predict/android";
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

    private FallMonitorService current;

    public FallMonitorService() {
        super("FallMonitorService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, SensorReaderService.class);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionBaz(Context context, String param1, String param2) {
        Intent intent = new Intent(context, SensorReaderService.class);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String baseUrl = intent.getStringExtra("baseUrl");
            BASE_URL = baseUrl;
            FALL_TRIGGER_URL = BASE_URL + "/fall";

            queue = Volley.newRequestQueue(this);
            createToastMessage(getString(R.string.start_reading_sensor_data));
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

            mSensorGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            current = this;
            if(mSensorGyro != null) mSensorManager.registerListener(this, mSensorGyro, Sensor.REPORTING_MODE_CONTINUOUS);
            if(mSensorAcc != null) mSensorManager.registerListener(this, mSensorAcc, Sensor.REPORTING_MODE_CONTINUOUS);
        }
    }

    private void createToastMessage(final String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable(){
            public void run(){
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void triggerFall(final String sensorData) {
        StringRequest sr = new StringRequest(Request.Method.GET , FALL_TRIGGER_URL+ "?sensor=android", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                createToastMessage(getString(R.string.fall_detect));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(sr);
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
                                if(response.getDouble("prediction") > 0.5 || avmFall) {
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
                            if(error.getMessage() != null) Log.e("Error", error.getMessage());

                        }
                    });
            queue.add(jsonObjectRequest);
        } catch (JSONException e) {
            Log.e("Exception", "Creating");
        }

    }

    public Notification createNotification(String extraInfo) {
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
                .setContentText(getText(R.string.fall_detect) + (extraInfo.length() == 0 ? "" : " " + extraInfo))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true).build();
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

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
            if(avm) {
                final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                Notification notification = createNotification("Only AVM Fall Detected !");
                notificationManager.notify(1, notification);
            }
            reset();
            /*if(gyro_above >= 20 && gyro_above < 200 && acc_below >= 65 && acc_above >= 15 && delta >= 500 && delta <= 1800) {
                triggerFall("");
                reset();
            }
            else reset();*/
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
}
