package com.example.licenta;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RabbitMQListenerService extends Service {

    private ConnectionFactory factory = new ConnectionFactory();
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private Channel subscribeChannel;
    private String detectedFacesDynamicQueueName;
    private static final String CHANNEL_ID = "canal";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        System.out.println("here!!!");

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        System.out.println("CREATED");

        setupConnectionFactory();

        DeliverCallback deliverCallback = getDeliverCallback();

        CancelCallback cancelCallback = getCancelCallback();

        try {
            subscribe(deliverCallback, cancelCallback);
        }catch(Exception e){
            Log.e("MESAJ", e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        System.out.println("DESTROYEDDDDDDDDDDD");

        executorService.submit(() -> {
            if(subscribeChannel != null){
                try {
                    subscribeChannel.queueDelete(detectedFacesDynamicQueueName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        executorService.shutdown();
    }
    private void setupConnectionFactory() {
        try {

            factory.setAutomaticRecoveryEnabled(false);
            factory.setUsername("admin");
            factory.setPassword("admin");
            factory.setVirtualHost("/");
            factory.setHost("192.168.100.29");
            factory.setPort(5672);
            factory.setConnectionTimeout(5000);

        } catch (Exception e1) {
            e1.printStackTrace();
        }

    }

    public CancelCallback getCancelCallback() {
        return consumerTag -> Log.i("MESAJ"," [x] Received '" + consumerTag + "'");
    }

    private DeliverCallback getDeliverCallback() {
        return (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");

            Integer frequencyInSeconds = getFrequencyInSecondsFromPreferences();

            Long lastNotificationTimestamp = getLastNotificationTimestampFromPreferences();
            Long nowTimestamp = new Date().getTime();

            if(nowTimestamp - lastNotificationTimestamp > frequencyInSeconds * 1000){
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //deprecated in API 26
                    v.vibrate(500);
                }

                sendMessageToActivity(message);

                saveIntoSharedPreferences("lastNotificationTimestamp", nowTimestamp);
            }
        };
    }

    private void sendMessageToActivity(String encodedImage){
        System.out.println("Sending message to activity...");

        Intent intent = new Intent("PersonDetectionNotification");
        // You can also include some extra data.
        intent.putExtra("encodedImage", encodedImage);
        //Bundle b = new Bundle();
        //b.putParcelable("encodedImage", encodedImage);
        //intent.putExtra("Location", b);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        Intent detectedFacesActivityIntent = new Intent(getApplicationContext(), DetectedFacesFeedActivity.class);
        detectedFacesActivityIntent.putExtra("com.licenta", 1);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, detectedFacesActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap myBitmap = decodedByte;
        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Alertă!")
                .setContentText("Persoană detectată de camera video")
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(myBitmap))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(new Random().nextInt(), notification);
    }

    private void subscribe(DeliverCallback deliverCallback, CancelCallback cancelCallback) {

        executorService.submit(() -> {

            try {

                Connection subscribeConnection = null;

                subscribeChannel = null;

                detectedFacesDynamicQueueName = "";

                while(true){

                    if((subscribeConnection == null || !subscribeConnection.isOpen()) || (subscribeChannel == null || !subscribeChannel.isOpen())){
                        Log.i("MESAJ", "CONEXIUNEA E INCHISA!, refac conexiunea!");
                        subscribeConnection = factory.newConnection();
                        subscribeChannel = subscribeConnection.createChannel();
                        AMQP.Queue.DeclareOk dc = subscribeChannel.queueDeclare("", true, false, true, null);
                        detectedFacesDynamicQueueName = dc.getQueue();

                        subscribeChannel.queueBind(detectedFacesDynamicQueueName, "poze", "");
                    }


                    subscribeChannel.basicConsume(detectedFacesDynamicQueueName, true, deliverCallback, cancelCallback);
                    Log.i("MESAJ","Astept 1 secunda...");
                    Thread.sleep(1000);
                }

            } catch (Exception e1) {
                Log.d("MESAJ", "Exception: " + e1.toString());
                e1.printStackTrace();
            }
        });
    }

    private void saveIntoSharedPreferences(String key, Long value) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value.toString());
        editor.commit();
    }

    private Long getLastNotificationTimestampFromPreferences(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String lastNotificationTimestampString = sharedPref.getString("lastNotificationTimestamp", null);

        Long lastNotificationTimestamp;
        if(lastNotificationTimestampString == null){
            lastNotificationTimestamp = 0l;
        }else{
            lastNotificationTimestamp = Long.parseLong(lastNotificationTimestampString);
        }

        return lastNotificationTimestamp;
    }

    private Integer getFrequencyInSecondsFromPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int selectedFrequency = Integer.valueOf(prefs.getString("frequency", "1"));
        switch (selectedFrequency){
            case 0:
                System.out.println("low");
                return 60;
            case 1:
                System.out.println("medium");
                return 30;
            case 2:
                System.out.println("high");
                return 1;
            default:
                return 30;
        }
    }
}
