package com.example.falldetection;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
    private static final String ACTION_FOO = "com.example.falldetection.action.FOO";
    private static final String ACTION_BAZ = "com.example.falldetection.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.example.falldetection.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.example.falldetection.extra.PARAM2";

    private static RequestQueue queue;
    private static String BASE_URL = "http://desktop-smbkroj.student.iastate.edu:8080";
    private static  String UPLOAD_SENSOR_URL = BASE_URL + "/upload/sensordata";

    private class SensorData {
        float x = 0, y = 0, z = 0;

        public SensorData(float _x, float _y, float _z) {
            x = _x;
            y = _y;
            z = _z;
        }

        @Override
        public String toString() {
            return getString(R.string.sensor_data_format, x, y, z);
        }
    }

    private class ReadingData {
        String type = "";
        List<SensorData> list = new ArrayList<SensorData>();

        public ReadingData(int sensorId) {
            if(sensorId == Sensor.TYPE_GYROSCOPE) type = "Gyroscope";
            else type = "Accelerometer";
        }

    }

    private ReadingData GYRO_READING;
    private ReadingData ACC_READING;

    private SensorManager mSensorManager;
    private Sensor mSensorGyro;
    private Sensor mSensorAcc;

    private int readingCount;
    private int READING_LIMIT = 2000;

    public SensorReaderService() {
        super("SensorReaderService");
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
        Intent intent = new Intent(context, SensorReaderService.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

            String baseUrl = intent.getStringExtra("baseUrl");
            int readsNeeded = 2000;
            int delay = 5;
            try {
                delay = Integer.parseInt(intent.getStringExtra("delay"));
                createToastMessage(getString(R.string.sleep_start) + " " +  delay + " seconds !");
                Thread.sleep(delay * 1000);
                readsNeeded = Integer.parseInt(intent.getStringExtra("readingCount"));
                readsNeeded = 2 * readsNeeded;
            } catch (NumberFormatException ex) {
            } catch (InterruptedException ex) {
            }
            BASE_URL = baseUrl;
            UPLOAD_SENSOR_URL = BASE_URL + "/upload/sensordata";
            READING_LIMIT = readsNeeded;
            GYRO_READING = new ReadingData(Sensor.TYPE_GYROSCOPE);
            ACC_READING = new ReadingData(Sensor.TYPE_ACCELEROMETER);
            queue = Volley.newRequestQueue(this);
            readingCount = 0;
            createToastMessage(getString(R.string.start_reading_sensor_data));
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

            mSensorGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

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

    private void uploadSensorData(final File sensorData) {
        final Map<String, String> sensorReadings = new HashMap<String, String>();
        sensorReadings.put("sensorData", fetchFileAsString(sensorData));
        sensorReadings.put("count", (READING_LIMIT/2) + "");
        StringRequest sr = new StringRequest(Request.Method.POST, UPLOAD_SENSOR_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                sensorData.delete();
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

    private void saveReadingData() {
        String title = "gyro_x,gyro_y,gyro_z,acc_x,acc_y,acc_z";
        int len = Math.max(GYRO_READING.list.size(), ACC_READING.list.size());
        String filename = "filename_" + System.currentTimeMillis() + ".csv";
        File file = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filename);
        //Log.d("location", file.getAbsolutePath());
        FileOutputStream fos;
        byte[] newLine = System.getProperty("line.separator").getBytes();
        try {
            fos = new FileOutputStream(file);
            fos.write(title.getBytes());
            fos.write(newLine);
            for(int i = 0; i < len; i++) {
                StringBuilder sb = new StringBuilder();
                if(i < GYRO_READING.list.size()) sb.append(GYRO_READING.list.get(i) + ",");
                else sb.append(",,,");
                if(i < ACC_READING.list.size()) sb.append(ACC_READING.list.get(i) + ",");
                else sb.append(",,,");
                sb.deleteCharAt(sb.length() - 1);
                fos.write(sb.toString().getBytes());
                fos.write(newLine);
            }
            fos.flush();
            fos.close();
            uploadSensorData(file);
            GYRO_READING = new ReadingData(Sensor.TYPE_GYROSCOPE);
            ACC_READING = new ReadingData(Sensor.TYPE_ACCELEROMETER);
            Log.d("Writing ", "Success");
        } catch (FileNotFoundException e) {
            Log.d("Folder not ", "found");
        } catch (IOException e) {
            Log.d("IO ", "Exception");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        if(readingCount == READING_LIMIT) {
            saveReadingData();
            Log.d("Success", "destroying");
            mSensorManager.unregisterListener(this);
            return;
        }
        if(sensorEvent.values.length < 3) return;
        readingCount++;
        float x = sensorEvent.values[0], y = sensorEvent.values[1], z = sensorEvent.values[2];
        switch (sensorType) {
            case Sensor.TYPE_GYROSCOPE:
                GYRO_READING.list.add(new SensorData(x, y, z));
                break;
            case Sensor.TYPE_ACCELEROMETER:
                ACC_READING.list.add(new SensorData(x, y, z));
                break;
            default:
                Log.d("Sensor " + sensorType, "");
        }
    }
}
