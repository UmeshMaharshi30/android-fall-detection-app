package com.example.falldetection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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
                /*int sleep = Integer.parseInt(delayTime.getText().toString());
                int readingsCount = Integer.parseInt(activityReadings.getText().toString());
                Intent intent = new Intent(MainActivity.this, SensorReaderService.class);
                intent.putExtra("baseUrl", serviceUrl.getText().toString());
                intent.putExtra("readingCount", readingCount.getText().toString());
                intent.putExtra("delay", delayTime.getText().toString());
                intent.putExtra("activity", activityName.getText().toString());
                intent.putExtra("readings", activityReadings.getText().toString());
                startService(intent);*/

                Intent intent = new Intent(MainActivity.this, FallMonitorService.class);
                intent.setAction(FallMonitorService.ACTION_START);
                startForegroundService(intent);
            }
        });

        stopMonitorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FallMonitorService.class);
                intent.setAction(FallMonitorService.ACTION_STOP);
                startForegroundService(intent);
            }
        });
    }

}
