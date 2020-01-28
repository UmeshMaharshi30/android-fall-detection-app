package com.example.falldetection;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
            final String action = intent.getAction();
            if (ACTION_FOO.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionFoo(param1, param2);
            } else if (ACTION_BAZ.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBaz(param1, param2);
            }
            GYRO_READING = new ReadingData(Sensor.TYPE_GYROSCOPE);
            ACC_READING = new ReadingData(Sensor.TYPE_ACCELEROMETER);

            readingCount = 0;

            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

            mSensorGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            if(mSensorGyro != null) mSensorManager.registerListener(this, mSensorGyro, Sensor.REPORTING_MODE_CONTINUOUS);
            if(mSensorAcc != null) mSensorManager.registerListener(this, mSensorAcc, Sensor.REPORTING_MODE_CONTINUOUS);
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
                fos.write(sb.toString().getBytes());
                fos.write(newLine);
            }
            fos.flush();
            fos.close();
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
