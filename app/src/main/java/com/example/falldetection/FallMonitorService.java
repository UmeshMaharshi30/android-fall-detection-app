package com.example.falldetection;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.Map;

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
    private static final String ACTION_FOO = "com.example.falldetection.action.FOO";
    private static final String ACTION_BAZ = "com.example.falldetection.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.example.falldetection.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.example.falldetection.extra.PARAM2";


    private static RequestQueue queue;
    private static String BASE_URL = "http://desktop-smbkroj.student.iastate.edu:8080";
    private static  String UPLOAD_SENSOR_URL = BASE_URL + "/upload/sensordata";
    private static  String FALL_TRIGGER_URL = BASE_URL + "/fall";

    private SensorManager mSensorManager;
    private Sensor mSensorAcc;
    private Sensor mSensorGyro;

    private SensorEventListener current;
    private double THRESHOLD = 0.0;
    private double curr_acc = 0.0;
    private double curr_gyro = 0.0;

    private int acc_above = 0;
    private int gyro_above = 0;
    private int acc_normal = 0;
    private int gyro_normal = 0;

    private double ACC_TRHESHOLD = 20.0;
    private double GYRO_TRHESHOLD = 4.5;

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
        Intent intent = new Intent(context, FallMonitorService.class);
        intent.setAction(ACTION_FOO);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
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
        Intent intent = new Intent(context, FallMonitorService.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String baseUrl = intent.getStringExtra("baseUrl");
            BASE_URL = baseUrl;
            THRESHOLD = intent.getDoubleExtra("threshold", 30);
            UPLOAD_SENSOR_URL = BASE_URL + "/upload/sensordata";
            queue = Volley.newRequestQueue(this);
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            current = this;
            if(mSensorAcc != null) mSensorManager.registerListener(current, mSensorAcc, Sensor.REPORTING_MODE_CONTINUOUS);
            if(mSensorGyro != null) mSensorManager.registerListener(current, mSensorGyro, Sensor.REPORTING_MODE_CONTINUOUS);
        }
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


    private  void detectFall() {
        StringRequest sr = new StringRequest(Request.Method.GET, FALL_TRIGGER_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {

                }
                //mSensorManager.registerListener(current, mSensorAcc, Sensor.REPORTING_MODE_CONTINUOUS);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        queue.add(sr);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        if(sensorEvent.values.length < 3) return;
        float x = sensorEvent.values[0], y = sensorEvent.values[1], z = sensorEvent.values[2];
        double net_acc = Math.sqrt((x * x) + (y * y) + (z * z));
        switch (sensorType) {
            case Sensor.TYPE_GYROSCOPE:
                if(net_acc < GYRO_TRHESHOLD) gyro_normal++;
                else gyro_above++;
                break;
            case Sensor.TYPE_ACCELEROMETER:
                if(net_acc < ACC_TRHESHOLD) acc_normal++;
                else acc_above++;
                break;
            default:
        }
        if(gyro_normal > 50 && acc_normal > 50) {
            if(gyro_above + acc_above > 10 && gyro_above + acc_above < 20) {
                mSensorManager.unregisterListener(this);
                detectFall();
                onDestroy();
            } else {
                gyro_above = 0;
                gyro_normal = 0;
                acc_above = 0;
                acc_normal = 0;
            }
        }
    }
}
