package com.example.proiectdomotica;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button detectedFacesButton = (Button) findViewById(R.id.detectedFacesButton);
        detectedFacesButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent k = new Intent(MainActivity.this, DetectedFacesFeedActivity.class);
                startActivity(k);

            }
        });

        Button bluetoothSettingsButton = (Button) findViewById(R.id.bluetoothButton);
        bluetoothSettingsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent k = new Intent(MainActivity.this, BluetoothActivity.class);
                startActivity(k);

            }
        });

        Button liveStreamButton = (Button) findViewById(R.id.liveStreamButton);
        liveStreamButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent k = new Intent(MainActivity.this, LiveStreamActivity.class);
                startActivity(k);

            }
        });
    }

}