package com.example.falldetection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity  {

    private TextView mTextGyro;
    private TextView mTextAcc;

    private Button saveButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        saveButton = (Button) findViewById(R.id.start_reading);

        TextView sensorTextView = (TextView) findViewById(R.id.sensor_list);
        //sensorTextView.setText(sensorText);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SensorReaderService.class);
                startService(intent);
            }
        });

    }

}
