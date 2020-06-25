package com.example.licenta;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "canal";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button detectedFacesButton = (Button) findViewById(R.id.detectedFacesButton);
        detectedFacesButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, DetectedFacesFeedActivity.class));
        });

        Button liveStreamButton = (Button) findViewById(R.id.liveStreamButton);
        liveStreamButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LiveStreamActivity.class));
        });

        Button settings = (Button) findViewById(R.id.settings);
        settings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });

        createNotificationChannel();

        startRabbitMQService();
    }

    private void startRabbitMQService(){
        Intent i = new Intent(getApplicationContext(), RabbitMQListenerService.class);
        getApplicationContext().startService(i);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "notifications", importance);
            channel.setDescription("descriere");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }



}