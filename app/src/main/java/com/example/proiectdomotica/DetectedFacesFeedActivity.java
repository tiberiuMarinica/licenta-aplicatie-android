package com.example.proiectdomotica;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class DetectedFacesFeedActivity extends AppCompatActivity {

    private ConnectionFactory factory = new ConnectionFactory();
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private Channel subscribeChannel;
    private String detectedFacesDynamicQueueName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detected_faces);
        setupConnectionFactory();

        DeliverCallback deliverCallback = new DeliverCallback() {
            @Override
            public void handle(String consumerTag, Delivery delivery) throws IOException {
                String message = new String(delivery.getBody(), "UTF-8");

                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //deprecated in API 26
                    v.vibrate(500);
                }

                changeImage(message);
            }
        };

        CancelCallback cancelCallback = new CancelCallback() {
            @Override
            public void handle(String consumerTag) throws IOException {
                Log.i("MESAJ"," [x] Received '" + consumerTag + "'");
            }
        };

        try {
            subscribe(deliverCallback, cancelCallback);
        }catch(Exception e){
            Log.e("MESAJ", e.toString());
            e.printStackTrace();
        }

    }
    private void changeImage(String encodedImage){

        ImageView img = (ImageView) findViewById(R.id.imageView);
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        Drawable d = new BitmapDrawable(getResources(), decodedByte);
        this.runOnUiThread(new Runnable() {
            public void run() {
                img.setImageDrawable(d);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

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
}