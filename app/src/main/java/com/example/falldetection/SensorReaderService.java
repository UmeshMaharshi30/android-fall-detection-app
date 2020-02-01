package com.example.falldetection;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

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
public class SensorReaderService extends IntentService implements SensorEventListener {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS

    private static RequestQueue queue;
    private static String BASE_URL = "http://desktop-smbkroj.student.iastate.edu:8080";
    private static  String UPLOAD_SENSOR_URL = BASE_URL + "/upload/sensordata";

    private static String current_activity;
    private static int total_activity_readings;
    private static  int START_DELAY;

    private SensorManager mSensorManager;
    private Sensor mSensorGyro;
    private Sensor mSensorAcc;

    private int readingCount;
    private int READING_LIMIT = 2000;

    private double ACC_ABOVE_THRESHOLD = 25.0, ACC_BELOW_THRESHOLD = 7.0, GYRO_ABOVE_THRESHOLD = 3.5;

    private int acc_normal = 0, acc_above = 0, acc_below = 0, gyro_normal = 0, gyro_above = 0;

    private static long start_time = -1;
    private static long end_time = -1;

    private SensorReaderService current;

    public SensorReaderService() {
        super("SensorReaderService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mSensorGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        current = this;
        if(mSensorGyro != null) mSensorManager.registerListener(this, mSensorGyro, Sensor.REPORTING_MODE_CONTINUOUS);
        if(mSensorAcc != null) mSensorManager.registerListener(this, mSensorAcc, Sensor.REPORTING_MODE_CONTINUOUS);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String baseUrl = intent.getStringExtra("baseUrl");
            int readsNeeded = 2000;
            int delay = 5;
            total_activity_readings =  5;
            START_DELAY = 5;
            current_activity = intent.getStringExtra("activity");
            try {
                delay = Integer.parseInt(intent.getStringExtra("delay"));
                total_activity_readings = Integer.parseInt(intent.getStringExtra("readings"));
                START_DELAY = delay;
                createToastMessage(getString(R.string.sleep_start) + " " +  delay + " seconds !");
                //Thread.sleep(START_DELAY * 1000);
                readsNeeded = Integer.parseInt(intent.getStringExtra("readingCount"));
                readsNeeded = 2 * readsNeeded;
            } catch (NumberFormatException ex) {
            }
            BASE_URL = baseUrl;
            UPLOAD_SENSOR_URL = BASE_URL + "/upload/sensordata";
            READING_LIMIT = readsNeeded;

            queue = Volley.newRequestQueue(this);
            readingCount = 0;
            createToastMessage(getString(R.string.start_reading_sensor_data));
        }
    }

    @Override
    public void onDestroy() {

    }

    private void createToastMessage(final String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable(){
            public void run(){
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadSensorData(final String sensorData) {
        final Map<String, String> sensorReadings = new HashMap<String, String>();
        sensorReadings.put("sensorData", sensorData);
        sensorReadings.put("count", (READING_LIMIT/2) + "");
        sensorReadings.put("activity", current_activity);
        final Bundle bundle = new Bundle();
        StringRequest sr = new StringRequest(Request.Method.POST, UPLOAD_SENSOR_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                createToastMessage(getString(R.string.uploaed_success_sensor_data));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                createToastMessage(getString(R.string.uploaed_failed_sensor_data));
            }
        }){
            @Override
            protected Map<String,String> getParams(){
                return sensorReadings;
            }
        };
        queue.add(sr);
    }


    public byte[] fetchFileAsByteArr(File file) {
        byte[] bytesArray = new byte[(int) file.length()];

        try {
            FileInputStream fis = new FileInputStream(file);
            fis.read(bytesArray); //read file into bytes[]
            fis.close();
        } catch (FileNotFoundException ex) {
            Log.e("File" + file.getName(), "Not founc");
        } catch (IOException ex) {
            Log.e("File" + file.getName(), "IO Exception");
        }
        return bytesArray;
    }

    private String fetchFileAsString(File file) {
        String contentBuilder = "";
        try  {
            contentBuilder = new String (fetchFileAsByteArr(file));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return contentBuilder;
    }

    private void transferData() {
        String title = "gyro_normal,gyro_above,acc_below,acc_normal,acc_above,duration";
        StringBuilder res = new StringBuilder();
        res.append(gyro_normal + "," + gyro_above + "," + acc_below + "," + gyro_normal + "," + gyro_above + "," + (end_time - start_time) + ",0" + System.getProperty("line.separator"));
        uploadSensorData(res.toString());
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
            if(gyro_above > 2 && acc_below > 2 && acc_above > 2) {
                transferData();
                reset();
            } else {
                reset();
            }
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

    public String getState() {
        return gyro_normal + " " + gyro_above + " " + acc_below + " " + acc_normal + " " + acc_above;
    }
}
