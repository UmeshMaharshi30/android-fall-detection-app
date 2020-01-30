package com.example.falldetection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity  {

    private EditText serviceUrl;
    private EditText readingCount;
    private EditText delayTime;
    private EditText activityName;
    private EditText activityReadings;
    private EditText thresholdReader;

    private Button saveButton;
    private Button stopButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        saveButton = (Button) findViewById(R.id.start_reading);
        stopButton = (Button) findViewById(R.id.stop_reading);

        serviceUrl = (EditText) findViewById(R.id.ip_address);
        readingCount = (EditText) findViewById(R.id.reading_count);
        delayTime = (EditText) findViewById(R.id.delay_time);
        activityName = (EditText) findViewById(R.id.activity_name);
        activityReadings = (EditText) findViewById(R.id.activity_count);
        thresholdReader = (EditText) findViewById(R.id.threshold);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*int sleep = Integer.parseInt(delayTime.getText().toString());;
                int readingsCount = Integer.parseInt(activityReadings.getText().toString());
                Intent intent = new Intent(MainActivity.this, SensorReaderService.class);
                intent.putExtra("baseUrl", serviceUrl.getText().toString());
                intent.putExtra("readingCount", readingCount.getText().toString());
                intent.putExtra("delay", delayTime.getText().toString());
                intent.putExtra("activity", activityName.getText().toString());
                intent.putExtra("readings", activityReadings.getText().toString());
                startService(intent);*/
                /*Intent intent = new Intent(MainActivity.this, FallMonitorService.class);
                intent.putExtra("threshold", Double.parseDouble(thresholdReader.getText().toString()));
                intent.putExtra("baseUrl", serviceUrl.getText().toString());
                startService(intent);*/
                actionOnService("start");
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                actionOnService("stop");
            }
        });
    }

    private void actionOnService(String action) {
        int serviceStatus = FallMonitoringSerivce.state;
        Intent intent = new Intent(this, FallMonitoringSerivce.class);
        Log.d("Action from parent", action);
        intent.setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
            return;
        }
        startService(intent);
    }

}
