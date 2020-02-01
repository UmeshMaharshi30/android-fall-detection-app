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

    private Button startMonitorButton;
    private Button stopMonitorButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceUrl = (EditText) findViewById(R.id.ip_address);
        readingCount = (EditText) findViewById(R.id.reading_count);
        delayTime = (EditText) findViewById(R.id.delay_time);
        activityName = (EditText) findViewById(R.id.activity_name);
        activityReadings = (EditText) findViewById(R.id.activity_count);
        thresholdReader = (EditText) findViewById(R.id.threshold);

        startMonitorButton = (Button) findViewById(R.id.start_service);
        stopMonitorButton = (Button) findViewById(R.id.stop_service);

        startMonitorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ForeEverService.class);
                intent.setAction(MonitorFall.ACTION_START);
                startForegroundService(intent);
            }
        });

        stopMonitorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ForeEverService.class);
                intent.setAction(MonitorFall.ACTION_STOP);
                startForegroundService(intent);
            }
        });
    }

}
